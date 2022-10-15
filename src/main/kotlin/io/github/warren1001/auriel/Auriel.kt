package io.github.warren1001.auriel

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.mongodb.client.MongoDatabase
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.messages.SendDefaults
import io.github.warren1001.auriel.command.Commands
import io.github.warren1001.auriel.eventhandler.ButtonInteractionHandler
import io.github.warren1001.auriel.eventhandler.ModalInteractionHandler
import io.github.warren1001.auriel.guild.Guilds
import io.github.warren1001.auriel.eventhandler.SpecialMessageHandler
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.litote.kmongo.KMongo
import org.litote.kmongo.util.UpdateConfiguration
import kotlin.system.exitProcess

class Auriel(val jda: JDA, youtubeToken: String) {
	
	companion object {
		val LINK_PATTERN: Regex = Regex("https?://")
	}
	
	private val mongo = KMongo.createClient()
	private val buttonInteractionHandler = ButtonInteractionHandler(this)
	private val modalInteractionHandler = ModalInteractionHandler(this)
	
	val youtube: YouTube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()) {}.setApplicationName("new-video-checker")
		.setYouTubeRequestInitializer(YouTubeRequestInitializer(youtubeToken)).build()
	val database: MongoDatabase = mongo.getDatabase("test2")
	val commands = Commands(this)
	val guilds = Guilds(this)
	val specialMessageHandler = SpecialMessageHandler(this)
	
	init {
		SendDefaults.ephemeral = true
		UpdateConfiguration.updateOnlyNotNullProperties = true
		jda.listener<MessageReceivedEvent> {
			try {
				specialMessageHandler.handleMessageReceived(it)
			} catch (e: Exception) {
				warren("${e.message}\n${e.stackTraceToString()}")
			}
		}
		jda.listener<ButtonInteractionEvent> {
			try {
				buttonInteractionHandler.handle(it)
			} catch (e: Exception) {
				warren("${e.message}\n${e.stackTraceToString()}")
			}
		}
		jda.listener<SelectMenuInteractionEvent> {
			try {
				specialMessageHandler.handleSelectMenu(it)
			} catch (e: Exception) {
				warren("${e.message}\n${e.stackTraceToString()}")
			}
		}
		jda.listener<ModalInteractionEvent> {
			try {
				modalInteractionHandler.onModalInteraction(it)
			} catch(e: Exception) {
				warren("${e.message}\n${e.stackTraceToString()}")
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

fun User.dm(message: String) = openPrivateChannel().queue_ { it.message(message).queue_() }

fun Member.dmWithFallback(message: String) = user.openPrivateChannel().queue { it.message(message).queue_(failure = ErrorHandler().handle(ErrorResponse.CANNOT_SEND_TO_USER) {
	val aGuild = auriel.guilds.getGuild(guild.id)
	val fallbackChannelId = aGuild.data.fallbackChannelId
	if (fallbackChannelId != null) {
		guild.getTextChannelById(fallbackChannelId)!!.message("*${user.asMention} I could not DM you, so I'm forced to message you here:*\n$message").queue_()
	} else {
		aGuild.logDMFailure(this, message)
	}
}) }

fun Guild.a() = auriel.guilds.getGuild(id)

fun GuildMessageChannel.a() = guild.a().getGuildMessageChannel(this)

fun Member.fullMention() = "$asMention (${user.name}#${user.discriminator})"

fun User.fullMention() = "$asMention ($name#$discriminator)"

fun String.truncate(length: Int): String = substring(0, this.length.coerceAtMost(length))

fun MessageChannel.message(message: String) = sendMessage(message.truncate(2000))

fun AuditableRestAction<Void>.queueDelete() = queue_(failure = ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))

fun <T> RestAction<T>.queue_(failure: ErrorHandler = ErrorHandler(), success: ((T) -> Unit)? = null): Unit = queue(success,
	failure.handle({ true }) { auriel.warren("${it.message}\n${it.stackTraceToString()}") })

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