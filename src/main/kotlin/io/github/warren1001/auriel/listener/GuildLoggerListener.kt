package io.github.warren1001.auriel.listener

import discord4j.core.event.domain.guild.BanEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.guild.UnbanEvent
import discord4j.core.`object`.audit.ActionType
import discord4j.core.spec.AuditLogQuerySpec
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.async
import io.github.warren1001.auriel.dm
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class GuildLoggerListener(private val auriel: Auriel) {
	
	fun handle(event: BanEvent): Publisher<out Any> {
		return event.guild.flatMap { it.getAuditLog(AuditLogQuerySpec.builder().limit(5).actionType(ActionType.MEMBER_BAN_ADD).build()).toMono() }.map {
			auriel.warren.dm("ban: ${it.entries}").async().subscribe()
			it.entries.first { it.targetId.isPresent && it.targetId.get() == event.user.id }
		}.flatMap {
			auriel.getGuildManager(event.guildId).guildLogger.logBan(event.user, it.responsibleUser.orElseThrow(), it.reason.orElse("No reason provided"))
		}
	}
	
	fun handle(event: UnbanEvent): Publisher<out Any> {
		return event.guild.flatMap {
			//val banMono = it.getAuditLog(AuditLogQuerySpec.builder().actionType(ActionType.MEMBER_BAN_ADD).build()).toMono()
			/*val unbanMono = */it.getAuditLog(AuditLogQuerySpec.builder().limit(5).actionType(ActionType.MEMBER_BAN_REMOVE).build()).toMono()
			//Mono.zip(banMono, unbanMono)
		}.map {
			auriel.warren.dm("unban: ${it.entries}").async().subscribe()
			it.entries.first { it.targetId.isPresent && it.targetId.get() == event.user.id }
		}.flatMap {
			Mono.`when`(
				auriel.getGuildManager(event.guildId).guildLogger.logUnban(
					event.user, /* TODO it.t1.entries.first().responsibleUser.orElseThrow(), it.t1.entries.first().reason.orElse("No reason provided"),*/
					it.responsibleUser.orElseThrow()
				).async()
			)
		}
	}
	
	fun handle(event: MemberLeaveEvent): Publisher<out Any> {
		return event.guild.flatMap {
			it.getAuditLog(AuditLogQuerySpec.builder().limit(5).actionType(ActionType.MEMBER_KICK).build()).toMono()
		}.map {
			auriel.warren.dm("kick: ${it.entries}").async().subscribe()
			it.entries.first { it.targetId.isPresent && it.targetId.get() == event.user.id }
		}.filter { it.targetId.isPresent && it.targetId.get() == event.user.id }.flatMap {
			auriel.getGuildManager(event.guildId).guildLogger.logKick(event.user, it.responsibleUser.orElseThrow(), it.reason.orElse("No reason provided"))
		}
	}
	
}