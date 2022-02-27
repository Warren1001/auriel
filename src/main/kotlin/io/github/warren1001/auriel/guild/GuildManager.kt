package io.github.warren1001.auriel.guild

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.channel.ChannelData
import io.github.warren1001.auriel.channel.ChannelManager
import io.github.warren1001.auriel.user.Permission
import io.github.warren1001.auriel.user.UserDataManager
import org.litote.kmongo.eq
import org.litote.kmongo.reactor.findOneById
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

class GuildManager(private val auriel: Auriel, private val id: Snowflake) {
	
	private val channelManagers = mutableMapOf<Snowflake, ChannelManager>()
	private val httpsLinkPattern: Regex = Regex("http[s]?://")
	
	val userDataManager = UserDataManager(auriel, id)
	val guildData = auriel.guildDataCollection.findOneById(id).blockOptional().orElseGet { GuildData(id) }
	
	init {
		auriel.channelDataCollection.find(ChannelData::guildId eq id).toFlux().subscribe { channelManagers[it._id] = ChannelManager(auriel, it._id, it.guildId, it) }
	}
	
	fun getChannelManager(channelId: Snowflake) = channelManagers.computeIfAbsent(channelId) { ChannelManager(auriel, channelId, id) }
	
	fun handleMessageCreate(event: MessageCreateEvent): Mono<out Any> {
		return event.message.authorAsMember.flatMap { member ->
			val content = event.message.content
			val filters = guildData.filters.filter { it.containsMatchIn(content) }
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
			} else member.getData(auriel).flatMap { data ->
				if (guildData.muteRoleId != null && !data.hasPermission(Permission.MODERATOR) && content.contains(httpsLinkPattern) && content.contains("@everyone", true)) {
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
	
	fun log(author: User, channel: GuildMessageChannel, header: String, reason: String, originalMessage: String): Mono<out Any> {
		return if (guildData.logChannelId != null) auriel.gateway.getChannelById(guildData.logChannelId!!).ofType(GuildMessageChannel::class.java)
			.flatMap {
				it.message(
					"__**$header**__\n" +
							"${author.mention} in ${channel.mention} for *$reason*\n" +
							"```${originalMessage.replace("```", "`\\`\\`")}```"
				)
			}
		else NOTHING
	}
	
}