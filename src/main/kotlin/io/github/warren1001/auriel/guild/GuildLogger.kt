package io.github.warren1001.auriel.guild

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.NOTHING
import io.github.warren1001.auriel.truncate
import reactor.core.publisher.Mono

class GuildLogger(private val auriel: Auriel, private val guildManager: GuildManager) {
	
	fun logMessageDelete(author: User, channel: GuildMessageChannel, header: String, reason: String, originalMessage: String, repostId: Snowflake? = null): Mono<out Any> {
		val msg = if (originalMessage.length > 1024) "```${originalMessage.replace("```", "`\\`\\`").truncate(1024 - 9)}..." else "```$originalMessage```"
		var builder = EmbedCreateSpec.builder()
			.color(Color.RED)
			.title(header)
			.addField("User", author.mention, true)
			.addField("Channel", channel.mention, true)
			.addField("Reason", reason, false)
			.addField("Original Message", msg, false)
		if (repostId != null) builder = builder.description(
			"[Jump to repost](https://discord.com/channels/${guildManager.guildData._id.asString()}/${channel.id.asString()}/${repostId.asString()})"
		)
		return log(builder.build())
	}
	
	fun logBan(banned: User, bannedBy: User, reason: String): Mono<out Any> {
		return log(
			EmbedCreateSpec.builder()
				.color(Color.RED)
				.title("User Banned")
				.addField("User", banned.mention, false)
				.addField("Banned By", bannedBy.mention, false)
				.addField("Reason", reason, false).build()
		)
	}
	
	fun logUnban(banned: User, /* TODO bannedBy: User, reason: String, */unbannedBy: User): Mono<out Any> {
		return log(
			EmbedCreateSpec.builder()
				.color(Color.RED)
				.title("User Unbanned")
				.addField("User", banned.mention, true)
				//.addField("Originally Banned By", bannedBy.mention, true)
				//.addField("Ban Reason", reason, false)
				.addField("Unbanned By", unbannedBy.mention, false).build()
		)
	}
	
	fun logKick(kicked: User, kickedBy: User, reason: String): Mono<out Any> {
		return log(
			EmbedCreateSpec.builder()
				.color(Color.RED)
				.title("User Kicked")
				.addField("User", kicked.mention, false)
				.addField("Kicked By", kickedBy.mention, false)
				.addField("Reason", reason, false).build()
		)
	}
	
	private fun log(embed: EmbedCreateSpec): Mono<out Any> {
		return if (guildManager.guildData.logChannelId != null) auriel.gateway.getChannelById(guildManager.guildData.logChannelId!!).ofType(GuildMessageChannel::class.java)
			.flatMap { it.createMessage(embed) }
		else NOTHING
	}
	
}