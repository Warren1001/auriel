package io.github.warren1001.auriel.channel

import com.mongodb.client.result.UpdateResult
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.user.Permission
import io.github.warren1001.auriel.util.Filter
import org.litote.kmongo.reactor.findOneById
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*
import kotlin.concurrent.timer

class ChannelManager {
	
	private val auriel: Auriel
	private val id: Snowflake
	private val guildId: Snowflake
	private val channelData: ChannelData
	
	constructor(auriel: Auriel, id: Snowflake, guildId: Snowflake) {
		this.auriel = auriel
		this.id = id
		this.guildId = guildId
		channelData = auriel.channelDataCollection.findOneById(id).blockOptional().orElseGet { ChannelData(id, guildId) }
		startMessageAgeTimer()
		deleteAllButOneMessage()
	}
	
	constructor(auriel: Auriel, id: Snowflake, guildId: Snowflake, channelData: ChannelData) {
		this.auriel = auriel
		this.id = id
		this.guildId = guildId
		this.channelData = channelData
		startMessageAgeTimer()
		deleteAllButOneMessage()
	}
	
	private val lastUserMessage = mutableMapOf<Snowflake, Snowflake>()
	private var messageAgeTimer: Timer? = null
	
	fun updateChannelData() = auriel.updateChannelData(channelData)
	
	fun setMaxMessageAge(maxMessageAge: Long, messageAgeInterval: Long): Mono<UpdateResult> {
		channelData.maxMessageAge = maxMessageAge
		channelData.messageAgeInterval = messageAgeInterval
		messageAgeTimer?.cancel()
		startMessageAgeTimer()
		return auriel.updateChannelData(channelData)
	}
	
	private fun startMessageAgeTimer() {
		if (channelData.maxMessageAge != 0L && channelData.messageAgeInterval != 0L) {
			messageAgeTimer = timer("messageAge-${id.asString()}", true, 0, channelData.messageAgeInterval) {
				auriel.gateway
					.getChannelById(id)
					.handleErrors(auriel, "startMessageAgeTimer")
					.cast(GuildMessageChannel::class.java)
					.flatMapMany {
						it.getMessagesBefore(Snowflake.of(Instant.now().minusMillis(channelData.maxMessageAge)))
					}
					.filter {
						!it.isPinned && !it.author.orElseThrow().isBot && !auriel.getGuildManager(guildId).userDataManager.getData(it.author.orElseThrow().id).block()!!
							.hasPermission(Permission.MODERATOR)
					}
					.flatMap { it.del() }
					.subscribe()
			}
		}
	}
	
	fun handleMessageCreate(event: MessageCreateEvent): Mono<out Any> {
		val author = event.message.author.orElseThrow()
		return author.getData(auriel, guildId).flatMap { userData ->
			if (userData.permission < Permission.MODERATOR && !author.isBot) {
				val content = event.message.content
				val filters = channelData.filters
				return@flatMap if (filters.any { it.containsMatchIn(content) }) {
					val blacklistedStrings = filters.filter { it.containsMatchIn(content) }.joinToString(separator = ", ") { it.getAllMatchedStrings(content) }
					Mono.`when`(
						event.message.del().async(),
						author.dm(
							"Your message was deleted because it contained the following: $blacklistedStrings\n" +
									"```${content.replace("```", "`\\`\\`")}```"
						).async()
					)
				} else if (content.split('\n').size > channelData.lineLimit) {
					Mono.`when`(
						event.message.del().async(),
						author.dm(
							"Your message was deleted because it went past the channel's line limit of ${channelData.lineLimit}.\n" +
									"```\n${content.replace("```", "`\\`\\`")}```"
						).async()
					)
				} else if (channelData.onlyOneMessage) {
					val messageId = lastUserMessage[author.id]
					lastUserMessage[author.id] = event.message.id
					if (messageId != null && !messageId.isDeletedMessage()) {
						auriel.gateway.getMessageById(id, messageId).filter {
							!it.isPinned && !author.isBot && !userData.hasPermission(Permission.MODERATOR)
						}.flatMap { it.del() }
					} else NOTHING
				} else NOTHING
			} else NOTHING
		}
	}
	
	fun toggleRepost(): Mono<UpdateResult> {
		channelData.allowBotReposts = !channelData.allowBotReposts
		return updateChannelData()
	}
	
	fun allowsReposting() = channelData.allowBotReposts
	
	fun addFilter(filter: Filter): Mono<UpdateResult> {
		channelData.filters.add(filter)
		return updateChannelData()
	}
	
	fun setOnlyOneMessage(value: Boolean): Mono<UpdateResult> {
		channelData.onlyOneMessage = value
		deleteAllButOneMessage()
		return updateChannelData()
	}
	
	fun setLineLimit(value: Int): Mono<UpdateResult> {
		channelData.lineLimit = value
		return updateChannelData()
	}
	
	private fun deleteAllButOneMessage() {
		if (channelData.onlyOneMessage) {
			auriel.gateway.getChannelById(id).cast(MessageChannel::class.java)
				.flatMapMany { it.getMessagesBefore(Snowflake.of(Instant.now())) }
				.handleErrors(auriel)
				.filter { !it.isPinned }
				.filter { !it.author.orElseThrow().isBot }
				.filter { !auriel.getGuildManager(guildId).userDataManager.getData(it.author.orElseThrow().id).block()!!.hasPermission(Permission.MODERATOR) }
				.filter {
					val authorId = it.author.orElseThrow().id
					val delete = lastUserMessage.containsKey(authorId)
					if (!delete) lastUserMessage[authorId] = it.id
					delete
				}
				.flatMap { it.del() }
				.handleErrors(auriel, "deleteAllButOneMessage")
				.subscribe()
		}
	}
	
	fun clearUserLastMessage(id: Snowflake, messageId: Snowflake) {
		if (lastUserMessage[id]?.equals(messageId) == true) lastUserMessage.remove(id)
	}
	
}