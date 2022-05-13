package io.github.warren1001.auriel.guild

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.spec.MessageCreateSpec
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.channel.ChannelData
import io.github.warren1001.auriel.channel.ChannelManager
import io.github.warren1001.auriel.user.Permission
import io.github.warren1001.auriel.user.UserDataManager
import io.github.warren1001.auriel.util.YoutubeAnnouncer
import io.github.warren1001.auriel.util.YoutubeData
import org.litote.kmongo.eq
import org.litote.kmongo.reactor.findOneById
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class GuildManager(private val auriel: Auriel, private val id: Snowflake) {
	
	private val channelManagers = mutableMapOf<Snowflake, ChannelManager>()
	private val httpsLinkPattern: Regex = Regex("https?://")
	
	val userDataManager = UserDataManager(auriel, id)
	val guildData: GuildData = auriel.guildDataCollection.findOneById(id).blockOptional().orElseGet { GuildData(id) }
	var youtubeAnnouncer: YoutubeAnnouncer? = auriel.youtubeManager.getYoutubeAnnouncer(id)
	var cloneQueue: CloneQueue? = null
	
	init {
		auriel.channelDataCollection.find(ChannelData::guildId eq id).toFlux().subscribe { channelManagers[it._id] = ChannelManager(auriel, it._id, it.guildId, it) }
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
	
	fun stopCloneQueue() {
		cloneQueue?.destroy()
		cloneQueue = null
	}
	
	fun getChannelManager(channelId: Snowflake) = channelManagers.computeIfAbsent(channelId) { ChannelManager(auriel, channelId, id) }
	
	fun handleMessageCreate(event: MessageCreateEvent): Mono<out Any> {
		return event.message.authorAsMember.flatMap { member ->
			val content = event.message.content
			val filters = guildData.filters.filter { it.containsMatchIn(content) }
			member.getData(auriel).filter { !it.hasPermission(Permission.MODERATOR) }.flatMap {
				if (filters.isNotEmpty()) {
					val blacklistedStrings = filters.filter { it.containsMatchIn(content) }.joinToString(separator = ", ") { it.getAllMatchedStrings(content) }
					if (filters.any { !it.shouldReplace() } || !getChannelManager(event.message.channelId).allowsReposting()) {
						Mono.`when`(
							event.message.delAndLog(auriel, "swearing: $blacklistedStrings").async(),
							member.dm(
								"Your message was deleted because it contained the following: $blacklistedStrings\n" +
										"```${content.replace("```", "`\\`\\`")}```"
							).async()
						)
					} else {
						var newContent = content
						filters.forEach { newContent = it.replace(newContent) }
						Mono.`when`(
							event.message.delAndLog(auriel, "swearing: $blacklistedStrings").async(),
							event.message.channel.flatMap { it.message("${member.mention} said (censored): $newContent") }.async()
						)
					}
				} else if (guildData.muteRoleId != null && content.contains(httpsLinkPattern) && content.contains("@everyone", true)) {
					Mono.`when`(
						member.addRole(guildData.muteRoleId!!, "Suspected scammer").async(),
						event.message.delAndLog(auriel, "suspected scammer, muted").async(),
						member.dm(
							"You have been muted on MrLlamaSC's Discord for posting what is seemingly a scam website link.\n" +
									"If this is an error, please contact ${auriel.warren.mention} or an online moderator to be unmuted."
						).async()
					)
				} else getChannelManager(event.message.channelId).handleMessageCreate(event)
			}
		}
		
	}
	
	fun startYoutubeAnnouncer(guildId: Snowflake, playlistId: String, roleId: Snowflake, channelId: Snowflake, roleMessageChannelId: Snowflake) {
		youtubeAnnouncer = auriel.youtubeManager.createYoutubeAnnouncer(YoutubeData(guildId, playlistId, roleId, channelId, roleMessageChannelId))
	}
	
	fun log(author: User, channel: GuildMessageChannel, header: String, reason: String, originalMessage: String): Mono<out Any> {
		return if (guildData.logChannelId != null) auriel.gateway.getChannelById(guildData.logChannelId!!).ofType(GuildMessageChannel::class.java)
			.flatMap {
				it.message(
					"__**$header**__\n" +
							"${author.mention} in ${channel.mention} for **$reason**\n" +
							"```${originalMessage.replace("```", "`\\`\\`")}```"
				)
			}
		else NOTHING
	}
	
	fun sendRoleGiveMsg(channelId: Snowflake, roleId: Snowflake, message: String): Mono<Message> {
		return auriel.gateway.getChannelById(channelId).ofType(GuildMessageChannel::class.java).flatMap {
			it.message(
				MessageCreateSpec.builder().content(message)
					.addComponent(ActionRow.of(Button.primary("r-${roleId.asString()}-g", "Give me the role"), Button.danger("r-${roleId.asString()}-r", "I don't want the role anymore"))).build()
			)
		}
	}
	
}