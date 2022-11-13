package io.github.warren1001.auriel

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.mongodb.client.MongoDatabase
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.SendDefaults
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.command.Commands
import io.github.warren1001.auriel.eventhandler.ButtonInteractionHandler
import io.github.warren1001.auriel.eventhandler.CommandAutoCompleteInteractionHandler
import io.github.warren1001.auriel.eventhandler.ModalInteractionHandler
import io.github.warren1001.auriel.eventhandler.SpecialMessageHandler
import io.github.warren1001.auriel.guild.Config
import io.github.warren1001.auriel.guild.Guilds
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.apache.commons.lang3.StringUtils
import org.litote.kmongo.KMongo
import org.litote.kmongo.util.UpdateConfiguration
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class Auriel(val jda: JDA, youtubeToken: String) {
	
	companion object {
		val LINK_PATTERN: Regex = Regex("https?://")
	}
	
	private val mongo = KMongo.createClient()
	private val buttonInteractionHandler = ButtonInteractionHandler(this)
	private val modalInteractionHandler = ModalInteractionHandler()
	
	val youtube: YouTube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()) {}.setApplicationName("new-video-checker")
		.setYouTubeRequestInitializer(YouTubeRequestInitializer(youtubeToken)).build()
	val database: MongoDatabase = mongo.getDatabase("auriel")
	val guilds = Guilds(this)
	val specialMessageHandler = SpecialMessageHandler(this)
	val autoCompletionHandler = CommandAutoCompleteInteractionHandler()
	val commands = Commands(this)
	val config = Config(this)
	
	init {
		SendDefaults.ephemeral = true
		UpdateConfiguration.updateOnlyNotNullProperties = true
		jda.listener<MessageReceivedEvent> {
			try {
				specialMessageHandler.handleMessageReceived(it)
			} catch (e: Exception) {
				warren(e.stackTraceToString())
			}
		}
		jda.listener<ButtonInteractionEvent> {
			try {
				buttonInteractionHandler.handle(it)
			} catch (e: Exception) {
				warren(e.stackTraceToString())
			}
		}
		jda.listener<SelectMenuInteractionEvent> {
			try {
				specialMessageHandler.handleSelectMenu(it)
			} catch (e: Exception) {
				warren(e.stackTraceToString())
			}
		}
		jda.listener<ModalInteractionEvent> {
			try {
				modalInteractionHandler.onModalInteraction(it)
			} catch(e: Exception) {
				warren(e.stackTraceToString())
			}
		}
		jda.listener<CommandAutoCompleteInteractionEvent> {
			try {
				autoCompletionHandler.onCommandAutoCompleteInteraction(it)
			} catch(e: Exception) {
				warren(e.stackTraceToString())
			}
		}
	}
	
	fun shutdown() {
		jda.shutdown()
		mongo.close()
	}
	
	fun warren(action: (User) -> Unit) = jda.retrieveUserById("164118147073310721").queue(action)
	
	fun warren(msg: String) = warren { it.dm(msg) }
	
}

private lateinit var auriel: Auriel

fun main(args: Array<String>) {
	val discordToken = args[0]
	val youtubeToken = args[1]
	val jda: JDA = light(discordToken, enableCoroutines = true) {
		enableIntents(GatewayIntent.values().toList())
		disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.STICKER, CacheFlag.VOICE_STATE)
		setChunkingFilter(ChunkingFilter.ALL)
		setMemberCachePolicy(MemberCachePolicy.ALL)
	}
	jda.listener<ReadyEvent> {
		try {
			auriel = Auriel(jda, youtubeToken)
			println("Ready!")
		} catch (e: Exception) {
			e.printStackTrace()
			exitProcess(1)
		}
	}
}

fun User.dm(message: String) = openPrivateChannel().queue_ { it.fullMessage(message).queue_() }

fun Member.dmWithFallback(message: String) = user.openPrivateChannel().queue { it.message(message).queue_(failure = ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER) {
	val aGuild = auriel.guilds.getGuild(guild.id)
	if (aGuild.data.has("guild:fallback-channel")) {
		guild.getTextChannelById(aGuild.data.getAsString("guild:fallback-channel"))!!
			.message("*${user.asMention} You have private messages disabled, so I'm forced to message you here:*\n$message").queue_()
	} else {
		aGuild.logDMFailure(this, message)
	}
}) }

fun Guild.a() = auriel.guilds.getGuild(id)

fun GuildMessageChannel.a() = guild.a().getGuildMessageChannel(this)

fun Member.a() = guild.a().users.getUser(id)

fun User.a(guildId: String) = auriel.guilds.getGuild(guildId).users.getUser(id)

fun Member.fullMention() = "$asMention (${user.name}#${user.discriminator})"

fun User.fullMention() = "$asMention ($name#$discriminator)"

fun User.isWarren() = id == "164118147073310721"

fun Member.isWarren() = id == "164118147073310721"

fun String.truncate(length: Int): String = substring(0, this.length.coerceAtMost(length))

fun MessageChannel.message(message: String) = sendMessage(message.truncate(2000))

fun MessageChannel.message(message: String, components: Collection<LayoutComponent>) = sendMessage(MessageCreate(message.truncate(2000), components = components))

fun SlashCommandInteractionEvent.replyFull(message: String): RestAction<out Any> {
	return if (message.length > 2000) {
		val parts = message.lineChunked(2000)
		var previousRestAction: RestAction<out Any> = reply(parts.removeAt(0))
		val chan = channel.asGuildMessageChannel()
		for ((i, part) in parts.withIndex()) {
			previousRestAction = previousRestAction.and(chan.sendMessage(part).delay(50L * i, TimeUnit.MILLISECONDS))
		}
		previousRestAction
	} else {
		reply_(message)
	}
}

fun MessageChannel.fullMessage(message: String): RestAction<out Any> {
	return if (message.length > 2000) {
		val parts = message.lineChunked(2000)
		var previousRestAction: RestAction<out Any>? = null
		for ((i, part) in parts.withIndex()) {
			previousRestAction = previousRestAction?.and(sendMessage(part).delay(50L * i, TimeUnit.MILLISECONDS)) ?: sendMessage(part).delay(50L * i, TimeUnit.MILLISECONDS)
		}
		previousRestAction!!
	} else {
		sendMessage(message)
	}
}

fun String.lineChunked(length: Int = 2000): MutableList<String> {
	val chunks = mutableListOf("")
	for (line in split('\n')) {
		if (line.length > length) lineChunked(line, length, chunks)
		else {
			val currIsEmpty = chunks.last().isEmpty()
			if (chunks.last().length + line.length > length) chunks += line
			else chunks[chunks.size - 1] += if (currIsEmpty) line else "\n$line"
		}
	}
	return chunks
}

private fun lineChunked(string: String, length: Int, chunks: MutableList<String>) {
	for (line in string.split(' ')) {
		if (chunks.last().length + line.length > length) chunks += line
		else chunks[chunks.size - 1] += " $line"
	}
}

fun MessageChannel.fullMessage(message: String, components: Collection<LayoutComponent>): RestAction<out Any> {
	return if (message.length > 2000) {
		val parts = message.chunked(2000)
		var previousRestAction: RestAction<out Any>? = null
		for ((i, part) in parts.withIndex()) {
			previousRestAction = previousRestAction?.and(sendMessage(MessageCreate(part, components = components)).delay(50L * i, TimeUnit.MILLISECONDS))
				?: sendMessage(MessageCreate(part, components = components)).delay(50L * i, TimeUnit.MILLISECONDS)
		}
		previousRestAction!!
	} else {
		sendMessage(MessageCreate(message, components = components))
	}
}

fun AuditableRestAction<Void>.queueDelete() = queue_(failure = ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))

fun <T> RestAction<T>.queue_(failure: ErrorHandler = ErrorHandler(), success: ((T) -> Unit)? = null): Unit = queue(success,
		failure.handle({ true }) { auriel.warren(it.stackTraceToString()) })

fun String.quote(truncateLength: Int = 2000): String {
	return if (length > truncateLength - 10) "```\n${replace("```", "`\\``").truncate(truncateLength - 10 - countMatches("```"))}...```" else "```\n$this```"
}

fun String.countMatches(substring: String): Int {
	var index = 0
	var count = 0
	
	while (true) {
		index = indexOf(substring, index)
		index += if (index != -1) {
			count++
			substring.length
		}
		else {
			return count
		}
	}
}

val cyrDict = mapOf(
	Pair("A", "А"),
	Pair("a", "а"),
	Pair("B", "В"),
	Pair("E", "Е"),
	Pair("e", "е"),
	Pair("K", "К"),
	Pair("M", "М"),
	Pair("H", "Н"),
	Pair("O", "О"),
	Pair("o", "о"),
	Pair("P", "Р"),
	Pair("p", "р"),
	Pair("C", "С"),
	Pair("c", "с"),
	Pair("T", "Т"),
	Pair("y", "y"),
	Pair("X", "Х"),
	Pair("x", "х"),
	Pair("U", "U"),
	Pair("u", "u")
)

fun String.replaceOtherAlphabets(): String {
	var result = StringUtils.stripAccents(this)
	for ((key, value) in cyrDict) {
		result = result.replace(value, key)
	}
	return result
}