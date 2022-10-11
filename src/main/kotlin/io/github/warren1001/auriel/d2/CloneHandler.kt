package io.github.warren1001.auriel.d2

import dev.minn.jda.ktx.interactions.components.replyModal
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.guild.AGuild
import io.github.warren1001.auriel.truncate
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.util.concurrent.ConcurrentLinkedQueue

class CloneHandler(private val guild: AGuild) {
	
	private val queue = ConcurrentLinkedQueue<CloneInfo>()
	private val helpees = mutableMapOf<String, CloneInfo>()
	private val helpers = mutableMapOf<String, CloneInfo>()
	private val helperMsgs = mutableMapOf<String, InteractionHook>()
	private val helperButtons = listOf(ActionRow.of(Button.primary("clone-${guild.id}-next", "Next"), Button.danger("clone-${guild.id}-finished", "I'm done helping")))
	private val helpeeButtons = listOf(ActionRow.of(Button.primary("clone-${guild.id}-help", "I need help"), Button.danger("clone-${guild.id}-cancel", "I no longer need help")))
	private val helpeeStartButton = listOf(ActionRow.of(Button.primary("clone-${guild.id}-start", "Start helping")))
	
	fun reset() {
		queue.clear()
		helpees.clear()
		helpers.clear()
		helperMsgs.clear()
	}
	
	fun sendRequestHelpMessage(event: SlashCommandInteractionEvent) {
		event.reply_("Sent the message.").queue()
		event.channel.sendMessage(MessageCreate("", components = helpeeButtons)).queue()
	}
	
	fun sendHelperMessage(event: SlashCommandInteractionEvent) {
		event.reply_("Sent the message.").queue()
		event.channel.sendMessage(MessageCreate("", components = helpeeStartButton)).queue()
	}
	
	fun openRequestHelpModal(event: ButtonInteractionEvent) {
		event.replyModal("${guild.id}-request", "Request Diablo Clone Help") {
			short("game-name", "Game Name", true, placeholder = "MyGame1")
			short("game-password", "Game Password", true, placeholder = "1")
			paragraph("game-other", "Other Info", false, placeholder = "(optional) Any other information you want to provide")
		}.queue()
	}
	
	fun beginHelping(event: ButtonInteractionEvent) {
		val info = queue.poll()
		if (info == null) {
			event.reply_("Queue is empty.", components = helperButtons).queue()
		} else {
			helpers[event.user.id] = info
			val requester = event.guild!!.getMemberById(info.requesterId)!!
			requester.user.openPrivateChannel()
				.queue { it.sendMessage("${event.user.name}#${event.user.discriminator} is on the way! " +
						"If they don't show up in a couple minutes or it says they concluded helping you and they didn't " +
						"help you, just join the queue again and tell Warren. If they stole your Annihilus, please tell Warren or a mod ASAP.")
					.queue() }
			event.reply_(
				"You are now helping ${requester.asMention} with their clone.\n\n" +
						"Game Name: `${info.gameName}`\n" +
						"Game Password: `${info.password}`\n" +
						"Other Info: ```${info.otherInfo}```",
				components = helperButtons
			).queue { helperMsgs[event.user.id] = it }
		}
	}
	
	fun nextHelp(event: ButtonInteractionEvent) {
		val previous = helpers[event.user.id]
		if (previous != null) {
			helpees.remove(previous.requesterId)
			event.jda.getUserById(previous.requesterId)!!.openPrivateChannel()
				.queue { it.sendMessage("${event.user.name}#${event.user.discriminator} has concluded helping you, enjoy!").queue() }
		}
		val info = queue.poll()
		if (info == null) {
			helpers.remove(event.user.id)
			event.reply_(content = "Queue is empty.", components = helperButtons).queue()
		} else {
			helpers[event.user.id] = info
			val requester = event.guild!!.getMemberById(info.requesterId)!!
			requester.user.openPrivateChannel()
				.queue { it.sendMessage("${event.user.name}#${event.user.discriminator} is on the way! " +
						"If they don't show up in a couple minutes or it says they concluded helping you and they didn't " +
						"help you, just join the queue again and tell Warren. If they stole your Annihilus, please tell Warren or a mod ASAP.")
					.queue() }
			event.reply_("You are now helping ${requester.asMention} with their clone.\n\n" +
					"Game Name: `${info.gameName}`\n" +
					"Game Password: `${info.password}`\n" +
					"Other Info: ```${info.otherInfo.truncate(500)}```", components = helperButtons).queue()
		}
	}
	
	fun finishHelping(event: ButtonInteractionEvent) {
		helpers.remove(event.user.id)
		helperMsgs.remove(event.user.id)
		event.reply_("You are no longer helping. Click the Start button again if you want to help again.").queue()
	}
	
	fun cancelHelp(event: ButtonInteractionEvent) {
		val info = helpees.remove(event.user.id)
		if (info == null) {
			event.reply_("You are not queued for help.").queue()
		} else {
			queue.remove(info)
			event.reply_("You are no longer in the queue to receive help.").queue()
		}
	}
	
	fun submitModal(userId: String, gameName: String, password: String, otherInfo: String) {
		if (helpees.containsKey(userId)) {
			queue.removeIf { it.requesterId == userId }
		}
		val info = CloneInfo(userId, gameName, password, otherInfo)
		helpees[userId] = info
		queue.add(info)
	}
	
}