package io.github.warren1001.auriel.eventhandler

import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.a
import io.github.warren1001.auriel.d2.tz.TerrorZone
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ButtonInteractionHandler(private val auriel: Auriel) {
	
	fun handle(event: ButtonInteractionEvent) {
		if (!event.isFromGuild) return
		if (event.componentId.contains("-")) {
			val guild = event.guild!!
			val args = event.componentId.split("-")
			val member = event.member!!
			if (args[0] == "r") {
				val role = guild.getRoleById(args[1])!!
				if (args[2] == "g") {
					if (member.roles.contains(role)) {
						event.reply_("You already have the role.", ephemeral = true).queue_()
					} else {
						guild.addRoleToMember(member, role).queue_()
						event.reply_("You now have the role.", ephemeral = true).queue_()
					}
				} else if (args[2] == "r") {
					if (member.roles.contains(role)) {
						guild.removeRoleFromMember(member, role).queue_()
						event.reply_("You no longer have the role.", ephemeral = true).queue_()
					} else {
						event.reply_("You do not have the role.", ephemeral = true).queue_()
					}
				}
			} else if (args[0] == "clone") {
				val aGuild = auriel.guilds.getGuild(guild.id)
				if (args[2] == "next") {
					//aGuild.cloneHandler.nextHelp(event)
					aGuild.cloneHandler.openGiveHelpModal(event)
				} else if (args[2] == "finished") {
					aGuild.cloneHandler.finishHelping(event)
				} else if (args[2] == "help") {
					aGuild.cloneHandler.openRequestHelpModal(event)
				} else if (args[2] == "cancel") {
					aGuild.cloneHandler.cancelHelp(event)
				} else if (args[2] == "start") {
					//aGuild.cloneHandler.beginHelping(event)
					aGuild.cloneHandler.openGiveHelpModal(event)
				}
			}
		}
		else if (event.componentId == "tzrolebutton") {
			val guild = event.guild!!.a()
			if (guild.tzGuildData.roleIds == null) {
				event.reply_("This feature has not been setup yet.").queue_()
			} else {
				val roleIds = guild.tzGuildData.roleIds!!
				val roleIdsList: List<Map.Entry<TerrorZone, String>> = roleIds.entries.toList()
				val roleIdsByAct: List<MutableList<Map.Entry<TerrorZone, String>>> = listOf(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
				roleIdsList.forEach { roleIdsByAct[it.key.act - 1].add(it) }
				var randId = 0
				
				val messageCreateData = auriel.specialMessageHandler.replyMultiSelectMenuMessage<MutableList<Map.Entry<TerrorZone, String>>> {
					userId = event.user.id
					values = roleIdsByAct
					format = "What notifications do you want for the Terror Zones in **%s**?"
					finishMsg = "You will now receive notifications for the selected TZs."
					onlyOne = false
					mustChoose = false
					filter = { list, i -> (list[0].key.act - 1) == i }
					optionConverter = { tz -> tz.map { SelectOption(it.key.zoneName, "${it.value}-${randId++}") } }
					display = { list -> "Act ${list[0].key.act}" }
					finished = { data ->
						val addRoles = data.map { it.value }.flatten().map { if (it.contains("-")) it.substringBefore("-") else it }.map { auriel.jda.getRoleById(it)!! }.toSet()
						val removeRoles = roleIds.values.map { auriel.jda.getRoleById(it)!! }.toSet() - addRoles
						event.guild!!.modifyMemberRoles(event.member!!, addRoles, removeRoles).queue_()
					}
				}
				event.reply_(messageCreateData.content, components = messageCreateData.components).queue_()
			}
		}
	}
	
}