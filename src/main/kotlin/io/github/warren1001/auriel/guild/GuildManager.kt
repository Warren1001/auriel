package io.github.warren1001.auriel.guild

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.spec.MessageCreateSpec
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.channel.ChannelData
import io.github.warren1001.auriel.channel.ChannelManager
import io.github.warren1001.auriel.d2.clone.CloneQueue
import io.github.warren1001.auriel.user.UserDataManager
import io.github.warren1001.auriel.youtube.YoutubeAnnouncer
import io.github.warren1001.auriel.youtube.YoutubeData
import org.litote.kmongo.eq
import org.litote.kmongo.reactor.findOneById
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class GuildManager(private val auriel: Auriel, private val id: Snowflake) {
	
	private val channelManagers = mutableMapOf<Snowflake, ChannelManager>()
	private val httpsLinkPattern: Regex = Regex("https?://")
	
	private val discordBotSpam: ConcurrentMap<Snowflake, Message> = ConcurrentHashMap()
	
	val userDataManager = UserDataManager(auriel, id)
	val guildData: GuildData = auriel.guildDataCollection.findOneById(id).blockOptional(Duration.ofMillis(5000L)).orElseGet { GuildData(id) }
	val guildLogger: GuildLogger = GuildLogger(auriel, this)
	var youtubeAnnouncer: YoutubeAnnouncer? = auriel.youtubeManager.getYoutubeAnnouncer(id)
	var cloneQueue: CloneQueue? = null
	
	init {
		auriel.channelDataCollection.find(ChannelData::guildId eq id).toFlux().async().subscribe { channelManagers[it._id] = ChannelManager(auriel, it._id, it.guildId, it) }
	}
	
	fun startCloneQueue(message: Message, helperChannelId: Snowflake, requestChannelId: Snowflake): Mono<out Any> {
		return if (cloneQueue != null) {
			message.reply("Queue already running!")
		} else {
			cloneQueue = CloneQueue(auriel, id)
			message.reply("Queue started!")
			message.guild.flatMap { guild ->
				val a = guild.getChannelById(helperChannelId).ofType(GuildMessageChannel::class.java).flatMap { cloneQueue!!.sendHelperMessage(it) }
				val b = guild.getChannelById(requestChannelId).ofType(GuildMessageChannel::class.java).flatMap { cloneQueue!!.sendJoinMessage(it) }
				Mono.`when`(a, b)
			}
		}
	}
	
	fun stopCloneQueue(): Mono<out Any>? {
		val a = cloneQueue?.destroy()
		cloneQueue = null
		return a
	}
	
	fun getChannelManager(channelId: Snowflake) = channelManagers.computeIfAbsent(channelId) { ChannelManager(auriel, channelId, id) }
	
	fun handleMessageCreate(event: MessageCreateEvent): Mono<out Any> {
		return event.message.authorAsMember.flatMap { member ->
			println("GuildManager: handleMessageCreate - start")
			val content = event.message.content
			val filters = guildData.filters.filter { it.containsMatchIn(content) }
			if (!userDataManager.isModerator(member.id)) {
				if (filters.isNotEmpty()) {
					val blacklistedStrings = filters.filter { it.containsMatchIn(content) }.joinToString(separator = ", ") { it.getAllMatchedStrings(content) }
					if (filters.any { !it.shouldReplace() } || !getChannelManager(event.message.channelId).allowsReposting()) {
						return@flatMap Mono.`when`(
							event.message.delAndLog(auriel, "swearing: $blacklistedStrings").async(),
							member.dm(
								"Your message was deleted for using a not-family-friendly word: $blacklistedStrings\n" +
										"```${content.replace("```", "`\\`\\`")}```"
							).async()
						)
					} else {
						var newContent = content
						filters.forEach { newContent = it.replace(newContent) }
						return@flatMap event.message.channel.flatMap { it.message("${member.mention} said (censored): $newContent") }.flatMap {
							event.message.delAndLog(auriel, "swearing: $blacklistedStrings", repostId = it.id)
						}
					}
				} else if (
					guildData.muteRoleId != null && content.contains(httpsLinkPattern) &&
					((content.contains("@everyone", true) || content.contains("nitro", true)) || content.contains("discord.gg"))
				) {
					if (content.contains("@everyone", true) || content.contains("nitro", true)) {
						discordBotSpam.remove(member.id)
						return@flatMap Mono.`when`(
							member.addRole(guildData.muteRoleId!!, "Suspected bot").async(),
							event.message.delAndLog(auriel, "suspected bot, muted").async(),
							member.dm(
								"You have been muted in MrLlamaSC's Discord for what is seemingly malicious bot behavior.\n" +
										"If this is an error, please contact ${auriel.warren.mention} or an online moderator to be unmuted."
							).async()
						)
					} else if (discordBotSpam.containsKey(member.id)) {
						val msg = discordBotSpam[member.id]!!
						val timeSince = event.message.timestamp.toEpochMilli() - msg.timestamp.toEpochMilli()
						if (timeSince < 5 * 1000L) {
							discordBotSpam.remove(member.id)
							return@flatMap Mono.`when`(
								msg.del().async(),
								member.addRole(guildData.muteRoleId!!, "Suspected bot").async(),
								event.message.delAndLog(auriel, "suspected bot, muted").async(),
								member.dm(
									"You have been muted in MrLlamaSC's Discord for what is seemingly malicious bot behavior.\n" +
											"If this is an error, please contact ${auriel.warren.mention} or an online moderator to be unmuted."
								).async()
							)
						} else {
							discordBotSpam[member.id] = event.message
						}
					} else {
						discordBotSpam[member.id] = event.message
					}
				}
				if (discordBotSpam.containsKey(member.id)) {
					val timeSince = event.message.timestamp.toEpochMilli() - discordBotSpam[member.id]!!.timestamp.toEpochMilli()
					if (timeSince > 5 * 1000L) discordBotSpam.remove(member.id)
				}
				return@flatMap getChannelManager(event.message.channelId).handleMessageCreate(event)
			}
			println("GuildManager: handleMessageCreate - end")
			return@flatMap event.message.channel.filter { it.type == Channel.Type.GUILD_NEWS }.flatMap { event.message.publish() }
		}
		
	}
	
	fun startYoutubeAnnouncer(guildId: Snowflake, playlistId: String, roleId: Snowflake, channelId: Snowflake, roleMessageChannelId: Snowflake) {
		youtubeAnnouncer = auriel.youtubeManager.createYoutubeAnnouncer(YoutubeData(guildId, playlistId, roleId, channelId, roleMessageChannelId))
	}
	
	fun sendRoleGiveMsg(channelId: Snowflake, roleId: Snowflake, message: String, given: String, removed: String): Mono<Message> {
		return auriel.gateway.getChannelById(channelId).ofType(GuildMessageChannel::class.java).flatMap {
			it.message(
				MessageCreateSpec.builder().content(message)
					.addComponent(ActionRow.of(Button.primary("r-${roleId.asString()}-g", given.truncate(80)), Button.danger("r-${roleId.asString()}-r", removed.truncate(80))))
					.build()
			)
		}
	}
	
}