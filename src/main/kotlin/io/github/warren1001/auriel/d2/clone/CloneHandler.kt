package io.github.warren1001.auriel.d2.clone

import dev.minn.jda.ktx.interactions.components.replyModal
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.guild.AGuild
import io.github.warren1001.auriel.util.PinMessage
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.util.concurrent.ConcurrentLinkedQueue

class CloneHandler(private val auriel: Auriel, private val guild: AGuild) {
	
	private val queue = ConcurrentLinkedQueue<CloneInfo>()
	private val helpees = mutableMapOf<String, CloneInfo>()
	private val helpers = mutableMapOf<String, CloneInfo>()
	
	private var helpeeButtons: Collection<LayoutComponent>? = null
	private var helperButton: Collection<LayoutComponent>? = null
	private var helpeeMessage: PinMessage? = null
	private var helperMessage: PinMessage? = null
	private var helped: Int = 0
	
	private var running = false
	private var lastQueueUpdate = 0L
	
	fun stop() {
		running = false
		queue.clear()
		helpeeButtons = null
		helperButton = null
		helpeeMessage?.let { auriel.specialMessageHandler.deletePinMessage(it) }
		helpeeMessage = null
		helperMessage?.let { auriel.specialMessageHandler.deletePinMessage(it) }
		helperMessage = null
		helped = 0
		lastQueueUpdate = 0L
	}
	
	fun start(helpeeChannel: GuildMessageChannel, helperChannel: GuildMessageChannel) {
		helpeeButtons = listOf(ActionRow.of(
			Button.primary("clone:helpee-request", guild.data.getAsString("clone:helpee-request-button")),
			Button.danger("clone:helpee-cancel", guild.data.getAsString("clone:helpee-cancel-button"))
		))
		helperButton = listOf(ActionRow.of(
			Button.primary("clone:helper-begin", guild.data.getAsString("clone:helper-begin-button")),
			Button.primary("clone:helper-mention", guild.data.getAsString("clone:helper-mention-button")),
		))
		helpeeMessage = auriel.specialMessageHandler.sendPinMessage(2, helpeeChannel,
			guild.data.getAsString("clone:helpee-message").replace("%POSITION%", helped.toString()).replace("%REMAINING%", queue.size.toString()),
			helpeeButtons!!)
		helperMessage = auriel.specialMessageHandler.sendPinMessage(1, helperChannel,
			guild.data.getAsString("clone:helper-message").replace("%POSITION%", helped.toString()).replace("%REMAINING%", queue.size.toString()),
			helperButton!!)
		running = true
	}
	
	fun openRequestHelpModal(event: ButtonInteractionEvent) {
		event.replyModal("clone:request", "Request Diablo Clone Help") {
			short("game-name", "Game Name", true, placeholder = "MyGame1")
			short("game-password", "Game Password", true, placeholder = "1")
			paragraph("game-other", "Other Info", false, placeholder = "(optional) Any other information you want to provide")
		}.queue_()
	}
	
	fun openGiveHelpModal(event: ButtonInteractionEvent) {
		if (helpers.contains(event.user.id)) {
			val info = helpers[event.user.id]!!
			val requester = event.guild!!.getMemberById(info.requesterId)!!
			event.replyModal("clone:help", "Diablo Clone Helper Form") {
				short("user", "Discord User", true, "${requester.user.name}#${requester.user.discriminator}")
				short("game-name", "Game Name", true, info.gameName)
				short("game-password", "Game Password", true, info.password)
				paragraph("game-other", "Other Info", false, info.otherInfo)
			}.queue_()
		} else {
			val info = queue.poll()
			if (info == null) {
				updateMessages()
				event.reply_("Queue is empty.").queue_()
			} else {
				helped++
				if (helped <= 5 || helped % 5 == 0) updateMessages()
				helpers[event.user.id] = info
				val requester = event.guild!!.getMemberById(info.requesterId)!!
				requester.dmWithFallback("**${event.user.fullMention()}** is on the way! " +
						"If they stole your Annihilus, please tell a moderator ASAP.")
				event.replyModal("clone:help", "Diablo Clone Helper Form") {
					short("user", "Discord User", true, "${requester.user.name}#${requester.user.discriminator}")
					short("game-name", "Game Name", true, info.gameName)
					short("game-password", "Game Password", true, info.password)
					paragraph("game-other", "Other Info", false, info.otherInfo)
				}.queue_()
			}
		}
	}
	
	private fun updateMessages() {
		helpeeMessage?.editContent(guild.data.getAsString("clone:helpee-message").replace("%POSITION%", helped.toString()).replace("%REMAINING%", queue.size.toString()))
		helperMessage?.editContent(guild.data.getAsString("clone:helper-message").replace("%POSITION%", helped.toString()).replace("%REMAINING%", queue.size.toString()))
	}
	
	fun completedHelp(event: ModalInteractionEvent) {
		val previous = helpers.remove(event.user.id)!!
		helpees.remove(previous.requesterId)
		val requester = event.guild!!.getMemberById(previous.requesterId)!!
		requester.dmWithFallback("**${event.user.fullMention()}** has concluded helping you. If you would like to show appreciation to your helper," +
				" give them a vouch using the `/vouch` command in the server! (Not here)")
		val helperUser = event.member!!.a()
		val helperData = helperUser.data
		val cloneKills = helperData.getOrDefault("cloneKills", 0) as Int + 1
		helperData.set("cloneKills", cloneKills)
		event.reply_("You have finished helping **${requester.fullMention()}**. You have completed a total of $cloneKills Clones.").queue_()
		helperUser.saveData()
	}
	
	fun cancelHelp(event: ButtonInteractionEvent) {
		val info = helpees.remove(event.user.id)
		if (info == null) {
			event.reply_("You are not queued for help.").queue_()
		} else {
			queue.remove(info)
			event.reply_("You are no longer in the queue to receive help.").queue_()
		}
	}
	
	fun submitModal(event: ModalInteractionEvent) {
		val userId = event.user.id
		if (helpees.containsKey(userId)) {
			queue.removeIf { it.requesterId == userId }
		}
		val gameName = event.getValue("game-name")!!.asString
		val password = event.getValue("game-password")!!.asString
		val otherInfo = event.getValue("game-other")?.asString
		val info = if (otherInfo.isNullOrEmpty()) CloneInfo(userId, gameName, password) else CloneInfo(userId, gameName, password, otherInfo)
		helpees[userId] = info
		queue.add(info)
		if (System.currentTimeMillis() - lastQueueUpdate > 1000L) {
			updateMessages()
			lastQueueUpdate = System.currentTimeMillis()
		}
		event.reply_("Your request has been submitted successfully.").queue_()
		event.member!!.dmWithFallback("You are position #**${queue.size + helped}** in the queue to receive help.\n" +
				"Please be patient as there may be a lot of people waiting.")
	}
	
	fun replyMention(event: ButtonInteractionEvent) {
		val curr = helpers[event.user.id]
		if (curr == null) {
			event.reply_("You are not currently helping anyone.").queue_()
		} else {
			val requester = event.guild!!.getMemberById(curr.requesterId)!!
			event.reply_("The person you are currently helping: ${requester.fullMention()}").queue_()
		}
	}
	
	fun isRunning() = running
	
}