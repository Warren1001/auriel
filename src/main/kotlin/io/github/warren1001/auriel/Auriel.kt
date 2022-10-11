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
import io.github.warren1001.auriel.util.SpecialMessageHandler
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.litote.kmongo.KMongo
import org.litote.kmongo.util.UpdateConfiguration

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
		jda.listener<MessageReceivedEvent> { specialMessageHandler.handleMessageReceived(it) }
		jda.listener<ButtonInteractionEvent> { buttonInteractionHandler.handle(it) }
		jda.listener<SelectMenuInteractionEvent> { specialMessageHandler.handleSelectMenu(it) }
		jda.listener<ModalInteractionEvent> { modalInteractionHandler.onModalInteraction(it) }
	}
	
	fun shutdown() {
		jda.shutdown()
		mongo.close()
	}
	
	fun warren(): User = jda.retrieveUserById("164118147073310721").complete()
	
}

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
		Auriel(jda, youtubeToken)
		println("Ready!")
	}
}

fun User.dm(message: String) = openPrivateChannel().complete().sendMessage(message.truncate(2000))

fun String.truncate(length: Int): String = substring(0, this.length.coerceAtMost(length))

fun MessageChannel.message(message: String) = sendMessage(message.truncate(2000))

fun AuditableRestAction<Void>.queueDelete() = queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))

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