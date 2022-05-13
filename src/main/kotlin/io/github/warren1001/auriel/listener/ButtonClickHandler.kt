package io.github.warren1001.auriel.listener

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.NOTHING
import io.github.warren1001.auriel.async
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

class ButtonClickHandler(private val auriel: Auriel) {
	
	fun handle(event: ButtonInteractionEvent): Publisher<out Any> {
		val args = event.customId.split("-")
		val member = event.interaction.member.orElseThrow()
		return if (args[0] == "r") {
			val roleId = Snowflake.of(args[1])
			if (args[2] == "g") {
				if (member.roleIds.contains(roleId)) {
					event.reply("You already have the role.").withEphemeral(true)
				} else {
					val a = event.reply("You now have the role.").withEphemeral(true).async()
					val b = member.addRole(roleId).async()
					Flux.merge(a, b)
				}
			} else if (args[2] == "r") {
				if (member.roleIds.contains(roleId)) {
					val a = event.reply("You no longer have the role.").withEphemeral(true).async()
					val b = member.removeRole(roleId).async()
					Flux.merge(a, b)
				} else {
					event.reply("You do not have the role.").withEphemeral(true)
				}
			} else NOTHING
		} else NOTHING
	}
	
}