package io.github.warren1001.auriel.eventhandler

import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ButtonInteractionHandler {
	
	fun handle(event: ButtonInteractionEvent) {
		if (!event.isFromGuild) return
		val guild = event.guild!!
		val args = event.componentId.split("-")
		val member = event.member!!
		if (args[0] == "r") {
			val role = guild.getRoleById(args[1])!!
			if (args[2] == "g") {
				if (member.roles.contains(role)) {
					event.reply_("You already have the role.", ephemeral = true).queue()
				} else {
					guild.addRoleToMember(member, role).queue()
					event.reply_("You now have the role.", ephemeral = true).queue()
				}
			} else if (args[2] == "r") {
				if (member.roles.contains(role)) {
					guild.removeRoleFromMember(member, role).queue()
					event.reply_("You no longer have the role.", ephemeral = true).queue()
				} else {
					event.reply_("You do not have the role.", ephemeral = true).queue()
				}
			}
		}
	}
	
}