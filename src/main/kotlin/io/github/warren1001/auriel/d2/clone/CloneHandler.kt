package io.github.warren1001.auriel.d2.clone

import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.interactions.components.replyModal
import dev.minn.jda.ktx.messages.edit
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.guild.AGuild
import io.github.warren1001.auriel.util.PinMessage
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.ComponentInteraction
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.util.*

class CloneHandler(private val auriel: Auriel, private val guild: AGuild) {
	
	private val queue = Collections.synchronizedList(mutableListOf<CloneInfo>())
	private val done = mutableMapOf<String, CloneInfo>()
	private val helpees = mutableMapOf<String, CloneInfo>()
	private val helpers = mutableMapOf<String, CloneInfo>()
	private val helperDMs = mutableMapOf<String, Message>()
	private val helpeeEditors = mutableMapOf<String, Message>()
	
	private var helpeeStartButton: Collection<LayoutComponent>? = null
	private var helpeeMessageButtons: Collection<LayoutComponent>? = null
	private var helpeeScamButton: Collection<LayoutComponent>? = null
	private var helpeeVouchButton: Collection<LayoutComponent>? = null
	private var helperStartButton: Collection<LayoutComponent>? = null
	private var helperMessageButtons: Collection<LayoutComponent>? = null
	private var helpeeMessage: PinMessage? = null
	private var helperMessage: PinMessage? = null
	
	private var helped: Int = 0
	private var fallbackChannelMention: String? = null
	
	private var running = false
	private var lastQueueUpdate = 0L
	
	fun stop() {
		running = false
		queue.clear()
		helpees.clear()
		helpers.clear()
		// TODO maybe edit the DMs?
		helperDMs.clear()
		helpeeStartButton = null
		helpeeMessageButtons = null
		helpeeScamButton = null
		helpeeVouchButton = null
		helperStartButton = null
		helperMessageButtons = null
		helpeeMessage?.let { auriel.specialMessageHandler.deletePinMessage(it) }
		helpeeMessage = null
		helperMessage?.let { auriel.specialMessageHandler.deletePinMessage(it) }
		helperMessage = null
		helped = 0
		lastQueueUpdate = 0L
	}
	
	fun start(helpeeChannel: GuildMessageChannel, helperChannel: GuildMessageChannel) {
		println("clone 0")
		helpeeStartButton = listOf(ActionRow.of(
			Button.primary("clone:helpee-request", guild.data.getAsString("guild:clone:request-help-button")),
			//Button.danger("clone:helpee-cancel", guild.data.getAsString("guild:clone:helpee-cancel-button"))
		))
		helpeeMessageButtons = listOf(ActionRow.of(
			Button.primary("clone:${guild.id}:helpee-edit", guild.data.getAsString("guild:clone:helpee-edit-button")),
			Button.danger("clone:${guild.id}:helpee-cancel", guild.data.getAsString("guild:clone:helpee-cancel-button"))
		))
		helpeeScamButton = listOf(ActionRow.of(
			Button.danger("clone:${guild.id}:helpee-scam", guild.data.getAsString("guild:clone:helpee-scam-button"))
		))
		helpeeVouchButton = listOf(ActionRow.of(
			Button.success("clone:${guild.id}:helpee-vouch", guild.data.getAsString("guild:clone:helpee-vouch-button"))
		))
		helperStartButton = listOf(ActionRow.of(
			Button.primary("clone:helper-begin", guild.data.getAsString("guild:clone:helper-begin-button")),
			//Button.primary("clone:helper-mention", guild.data.getAsString("guild:clone:helper-mention-button")),
		))
		helperMessageButtons = listOf(
			ActionRow.of(
				Button.primary("clone:${guild.id}:helper-next", guild.data.getAsString("guild:clone:helper-next-button")),
				Button.danger("clone:${guild.id}:helper-done", guild.data.getAsString("guild:clone:helper-done-button"))
			),
			ActionRow.of(
				StringSelectMenu("clone:${guild.id}:helper-error", "Error") {
					setRequiredRange(1, 1)
					option(guild.data.getAsString("guild:clone:afk-kill"), "clone:afk-kill")
					option(guild.data.getAsString("guild:clone:afk-nokill"), "clone:afk-nokill")
					option(guild.data.getAsString("guild:clone:alr-done"), "clone:alr-done")
					option(guild.data.getAsString("guild:clone:game-dne"), "clone:game-dne")
					option(guild.data.getAsString("guild:clone:game-lr"), "clone:game-lr")
					option(guild.data.getAsString("guild:clone:game-pc"), "clone:game-pc")
					//option(guild.data.getAsString("guild:clone:other"), "clone:other") TODO
				}
			)
		)
		println("clone 1")
		if (guild.data.has("guild:fallback-channel")) fallbackChannelMention = helpeeChannel.guild.getTextChannelById(guild.data.getAsString("guild:fallback-channel"))!!.asMention
		println("clone 2")
		helpeeMessage = auriel.specialMessageHandler.sendPinMessage(1, helpeeChannel,
			guild.data.getAsString("guild:clone:helpee-message").replace("%POSITION%", helped.toString()).replace("%REMAINING%", queue.size.toString()),
			helpeeStartButton!!)
		helperMessage = auriel.specialMessageHandler.sendPinMessage(1, helperChannel,
			guild.data.getAsString("guild:clone:helper-message").replace("%POSITION%", helped.toString()).replace("%REMAINING%", queue.size.toString()),
			helperStartButton!!)
		println("clone 3")
		
		running = true
	}
	
	fun openRequestHelpModal(event: ButtonInteractionEvent) {
		if (helpers.any { it.value.requesterId == event.user.id }) {
			event.reply_("It is too late to edit, someone is already helping you.").queue_()
			return
		}
		if (helpees.containsKey(event.user.id)) {
			if (event.componentId == "clone:helpee-request") {
				event.reply_("You have already requested help. If you need to edit or remove your submission or want to know your queue position, check your direct messages${
					if (fallbackChannelMention != null) " (or $fallbackChannelMention if you have direct messages disabled)" else ""
				}.").queue_()
				return
			}
			val info = helpees[event.user.id]!!
			if (!helpeeEditors.containsKey(event.user.id)) {
				queue.remove(info)
				if (event.guild != null) event.member!!.dm("Editing your Diablo Clone help request...\n\n" +
						"If you can read this message, you have not submitted your edit yet. Please return to the edit page and press `Confirm` to submit your edit." +
						" Your request cannot be processed until you have submitted your edit.", callback = { helpeeEditors[event.user.id] = it })
				else event.user.dm("Editing your Diablo Clone help request...\n\n" +
						"If you can read this message, you have not submitted your edit yet. Please return to the edit page and press `Confirm` to submit your edit." +
						" Your request cannot be processed until you have submitted your edit.").queue_ { helpeeEditors[event.user.id] = it }
			}
			println("modal id 1 clone:${guild.id}:request")
			event.replyModal("clone:${guild.id}:request", "Request Diablo Clone Help") {
				short("game-name", "Game Name", true, placeholder = "MyGame1", value = info.gameName)
				short("game-password", "Game Password", true, placeholder = "1", value = info.password)
				short("game-spawn", "Diablo Spawn", true, placeholder = "Did you spawn DClone already? If so, where?", value = info.spawned)
				short("game-spawn", "Diablo Spawn", true, placeholder = "Did you spawn DClone already? If so, where? If not, write no.", value = info.furthestAct)
				paragraph("game-other", "Other Info", false, placeholder = "(optional) Any other information you want to provide", value = info.otherInfo)
			}.queue_()
		} else {
			println("modal id 2 clone:${guild.id}:request")
			println(guild.id)
			event.replyModal("clone:${guild.id}:request", "Request Diablo Clone Help") {
				short("game-name", "Game Name", true, placeholder = "MyGame1")
				short("game-password", "Game Password", true, placeholder = "1")
				short("game-spawn", "Diablo Spawn", true, placeholder = "Did you spawn DClone already? If so, where? If not, write no.")
				short("game-act", "Act Access", true, placeholder = "What is the last Act in Hell you can access?")
				paragraph("game-other", "Other Info", false, placeholder = "(optional) Any other information you want to provide")
			}.queue_()
		}
	}
	
	fun isInteractionRelated(event: GenericComponentInteractionCreateEvent): InteractionRelation {
		val id = event.componentId
		if (!id.startsWith("clone")) return InteractionRelation.UNRELATED
		if (id.countMatches(":") > 1 && id.split(':')[1] != guild.id) return InteractionRelation.WRONG_GUILD
		if (id.countMatches(":") > 2 && id.split(':')[2] != event.user.id) return InteractionRelation.WRONG_USER
		return InteractionRelation.RELATED
	}
	
	fun isInteractionRelated(event: ModalInteractionEvent): InteractionRelation {
		val id = event.modalId
		if (!id.startsWith("clone")) return InteractionRelation.UNRELATED
		if (id.countMatches(":") > 1 && id.split(':')[1] != guild.id) return InteractionRelation.WRONG_GUILD
		if (id.countMatches(":") > 2 && id.split(':')[2] != event.user.id) return InteractionRelation.WRONG_USER
		return InteractionRelation.RELATED
	}
	
	fun onButtonPress(event: ButtonInteractionEvent): InteractionRelation {
		val relation = isInteractionRelated(event)
		if (relation == InteractionRelation.RELATED) {
			when (event.componentId) {
				"clone:helpee-request" -> openRequestHelpModal(event)
				"clone:${guild.id}:${event.user.id}:helpee-edit" -> openRequestHelpModal(event)
				"clone:${guild.id}:${event.user.id}:helpee-cancel" -> cancelRequest(event)
				"clone:${guild.id}:${event.user.id}:helpee-scam" -> scammed(event)
				"clone:${guild.id}:${event.user.id}:helpee-vouch" -> vouch(event)
				"clone:helper-begin" -> doHelping(event)
				"clone:${guild.id}:helper-next" -> helpNext(event)
				"clone:${guild.id}:helper-done" -> finishHelping(event)
			}
		}
		return relation
	}
	
	fun finishHelping(event: ComponentInteraction) {
		helperDMs[event.user.id]?.edit("Thanks for helping :)")?.queue_()
		helpers.remove(event.user.id)
		helperDMs.remove(event.user.id)
	}
	
	fun cancelRequest(event: ComponentInteraction) {
		if (helpees.containsKey(event.user.id)) {
			val info = helpees[event.user.id]!!
			queue.remove(info)
			helpees.remove(event.user.id)
			event.reply_("Your request has been canceled.").queue_()
		} else {
			event.reply_("You do not have a request to cancel.").queue_()
		}
	}
	
	fun scammed(event: ComponentInteraction) {
		if (done.containsKey(event.user.id)) {
			val info = done[event.user.id]!!
			val g = guild.jda()
			val requester = g.getMemberById(info.requesterId)!!
			val helper = g.getMemberById(info.helperId)!!
			event.deferReply().queue_()
			guild.privateChannelManager.createPrivateChannel(requester, listOf(helper), "his Annihilus was stolen by ${helper.asMention}", true) {
				event.reply_("A scam report has been made for you, please explain what happened in ${it.asMention}.").queue_()
				helper.dm("A scam report has been made against you, please explain what happened in ${it.asMention}.")
			}
			
		} else {
			event.reply_("You have not been helped to have been scammed.").queue_()
		}
	}
	
	fun vouch(event: ComponentInteraction) {
		event.reply_("was lazy").queue_()
	}
	
	fun onStringSelectSelection(event: StringSelectInteractionEvent): InteractionRelation {
		val relation = isInteractionRelated(event)
		if (relation == InteractionRelation.RELATED) {
			when (event.componentId) {
				"clone:${guild.id}:helper-error" -> handleHelperError(event)
			}
		}
		return relation
	}
	
	fun onModalSubmit(event: ModalInteractionEvent): InteractionRelation {
		val relation = isInteractionRelated(event)
		println("relation=${relation.name}, id=${event.modalId}")
		if (relation == InteractionRelation.RELATED) {
			println("modal received")
			when (event.modalId) {
				"clone:${guild.id}:request" -> submitModal(event)
			}
		}
		return relation
	}
	
	private fun handleHelperError(event: StringSelectInteractionEvent) {
		val helpInfo = helpers[event.user.id]!!
		when (event.selectedOptions[0].value) {
			"afk-kill" -> handleAFK(event)
			"afk-nokill" -> failedHelp(event, "you were AFK. The helper says they did not kill Diablo Clone")
			"alr-done" -> failedHelp(event, "the game was empty or Diablo Clone was already dead")
			"game-dne" -> failedHelp(event, "the game does not exist, please check the game name")
			"game-lr" -> failedHelp(event, "the helper does not meet the level requirements. You will have to look for someone who does")
			"game-pc" -> failedHelp(event, "the game is full. If there are players in the game, please ask one of them to leave. If you are alone, you are out of luck")
			//"other" -> doOther(event)
		}
	}
	
	private fun handleAFK(event: StringSelectInteractionEvent) {
		val info = helpers[event.user.id]!!
		val g = guild.jda()
		val helper = g.getMemberById(event.user.id)!!
		val requester = g.getMemberById(info.requesterId)!!
		event.deferReply().queue_()
		completedHelp(event)
		guild.privateChannelManager.createPrivateChannel(helper, listOf(requester), "${requester.asMention} was AFK so ${helper.asMention} is holding the Annihilus.", false) {
			event.reply_("${it.asMention} was created in the server to establish communication between you and ${requester.asMention}.").queue_()
			requester.dm("You were AFK so ${helper.asMention} is holding the Annihilus. They will not identify it. Please contact them in ${it.asMention}.")
		}
	}
	
	private fun rejoinQueue(helpInfo: CloneInfo) {
		val position = (helpInfo.position - helped).coerceAtLeast(0)
		queue.add(position, helpInfo)
		//updateMessages()
	}
	
	fun doHelping(event: ButtonInteractionEvent) {
		if (helpers.containsKey(event.user.id)) {
			event.reply_("You are already helping someone. Check your direct messages.").queue_()
		} else {
			synchronized(queue) {
				if (queue.isEmpty()) {
					updateMessages()
					event.reply_("Queue is empty.").queue_()
				} else {
					val helpInfo = queue.removeAt(0)
					if (helperDMs.containsKey(event.user.id)) {
						editHelperMessage(helperDMs[event.user.id]!!, event.user, event.guild!!, helpInfo)
					} else {
						event.deferReply(true).queue_ { reply ->
							event.member!!.dm("You are helping with Diablo Clones in **${event.guild!!.name}**.\n\nLoading..",
								failure = { "You must allow direct messages from server members to use this feature." }) { dm ->
								editHelperMessage(dm, event.user, event.guild!!, helpInfo)
								reply.editOriginal("You are now helping with Diablo Clones. Check your direct messages.").queue_()
							}
						}
					}
				}
			}
			
		}
	}
	
	fun helpNext(event: ButtonInteractionEvent) {
		if (helpers.containsKey(event.user.id)) {
			synchronized(queue) {
				if (queue.isEmpty()) {
					updateMessages()
					event.reply_("Queue is empty.").queue_()
				} else {
					editHelperMessage(helperDMs[event.user.id]!!, event.user, event.guild!!, queue.removeAt(0))
				}
			}
		} else {
			event.reply_("You are not helping anyone. Use the button in the Discord to start helping.").queue_()
		}
	}
	
	private fun editHelperMessage(dm: Message, helper: User, guild: Guild, helpInfo: CloneInfo?) {
		if (helpInfo == null) {
			dm.edit("You are helping with Diablo Clones in **${guild.name}**.\n\n**Queue is empty.**").queue_()
		} else {
			val requester = guild.getMemberById(helpInfo.requesterId)!!
			dm.edit("You are helping with Diablo Clones in **${guild.name}**.\n\n" +
					"__Player__: ${requester.asMention} `${requester.fullMention()}`\n" +
					"__Game Name__: `${helpInfo.gameName}`\n" +
					"__Game Password__: `${helpInfo.password}`\n" +
					"__Spawned__: `${helpInfo.spawned}`\n" +
					"__Other Info__: `${helpInfo.otherInfo}`",
				components = helperMessageButtons
			).queue_() // TODO buttons
			helpers[helper.id] = helpInfo
			requester.dm("**${helper.fullMention()}** is on the way to slay your Diablo Clone!")
			helped++
			if (helped <= 5 || helped % 5 == 0) updateMessages()
		}
	}
	
	private fun updateMessages() {
		helpeeMessage?.editContent(guild.data.getAsString("clone:helpee-message").replace("%POSITION%", helped.toString()).replace("%REMAINING%", queue.size.toString()))
		helperMessage?.editContent(guild.data.getAsString("clone:helper-message").replace("%POSITION%", helped.toString()).replace("%REMAINING%", queue.size.toString()))
	}
	
	fun failedHelp(event: ComponentInteraction, reason: String) {
		val info = helpers.remove(event.user.id)!!
		helpees.remove(info.requesterId)
		val requester = event.guild!!.getMemberById(info.requesterId)!!
		requester.dm("**${event.user.fullMention()}** was unable to help you because $reason. You have been removed from the queue. Feel free to rejoin it if you are able to fix the issue.")
		event.reply_("You have failed to help **${requester.fullMention()}**. Please try again later.").queue_()
		//rejoinQueue(info)
	}
	
	fun completedHelp(event: ComponentInteraction, increment: Boolean = true) {
		val previous = helpers.remove(event.user.id)!!
		helpees.remove(previous.requesterId)
		val requester = event.guild!!.getMemberById(previous.requesterId)!!
		requester.dm("**${event.user.fullMention()}** has concluded helping you. If you would like to show appreciation to your helper," +
				" give them a vouch using the `/vouch` command in the server! (Not here)")
		val helperUser = event.member!!.a()
		val cloneKills = helperUser.data.incrementInt("cloneKills")
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
		val isEdit = helpees.containsKey(userId)
		val gameName = event.getValue("game-name")!!.asString
		val password = event.getValue("game-password")!!.asString
		val spawned = event.getValue("game-spawn")!!.asString
		val furthestAct = event.getValue("game-act")!!.asString
		val otherInfo = event.getValue("game-other")?.asString
		val info = if (isEdit) helpees[userId]!! else {
			if (otherInfo.isNullOrEmpty()) CloneInfo(userId, helped + queue.size + 1, "", gameName, password, spawned, furthestAct)
			else CloneInfo(userId, helped + queue.size + 1, "", gameName, password, spawned, furthestAct, otherInfo)
		}
		if (!isEdit) {
			helpees[userId] = info
			queue.add(info)
		} else {
			info.gameName = gameName
			info.password = password
			info.spawned = spawned
			info.furthestAct = furthestAct
			info.otherInfo = otherInfo ?: "None"
			rejoinQueue(info)
		}
		if (System.currentTimeMillis() - lastQueueUpdate > 1000L) {
			updateMessages()
			lastQueueUpdate = System.currentTimeMillis()
		}
		if (isEdit) {
			event.reply_("Your request has been updated successfully.").queue_()
		} else {
			event.reply_("Your request has been submitted successfully.").queue_()
			event.member!!.dm("You are position #**${queue.size + helped}** in the queue to receive help.\n" +
					"Please be patient as there may be a lot of people waiting.")
		}
		println("modal received 1")
	}
	
	fun isRunning() = running
	
}