package io.github.warren1001.auriel.d2.clone

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.UpdateOptions
import dev.minn.jda.ktx.interactions.components.TextInput
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.d2.*
import io.github.warren1001.auriel.guild.AGuild
import io.github.warren1001.auriel.util.PinMessage
import net.dv8tion.jda.api.components.Component
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ComponentInteraction
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.updateOne
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

const val ID_BUTTON_HELPEE_REQUEST_MAKE: String = "clone:helpee-request:make"
const val ID_BUTTON_HELPEE_REQUEST_CANCEL: String = "clone:helpee-request:cancel"
const val ID_BUTTON_HELPEE_INPUT_OPEN: String = "clone:helpee:input:open"
const val ID_BUTTON_HELPEE_CONFIRM: String = "clone:helpee:confirm"
const val ID_BUTTON_HELPEE_REPORT: String = "clone:helpee:report"
const val ID_BUTTON_HELPEE_VOUCH: String = "clone:helpee:vouch"
const val ID_BUTTON_HELPEE_FINISH: String = "clone:helpee:finish"
const val ID_BUTTON_HELPEE_REQUEUE: String = "clone:helpee:requeue"
const val ID_BUTTON_HELPER_BEGIN: String = "clone:helper:begin"
const val ID_BUTTON_HELPER_STOP: String = "clone:helper:stop"
//const val ID_BUTTON_HELPER_RETRY_HELP: String = "clone:helper:retry-help"
const val ID_BUTTON_HELPER_NEXT: String = "clone:helper:next"
const val ID_BUTTON_HELPER_SUBMIT: String = "clone:helper:submit"
const val ID_BUTTON_HELPER_JOIN: String = "clone:helper:join"

const val ID_MESSAGE_HELPEE_INSTRUCTIONS: String = "guild:clone:helpee-instructions"
const val ID_MESSAGE_HELPER_INSTRUCTIONS: String = "guild:clone:helper-instructions"
const val ID_MESSAGE_HELPEE_GAME_INFO: String = "guild:clone:helpee-game-info"
const val ID_MESSAGE_HELPER_GAME_INFO: String = "guild:clone:helper-game-info"

const val ID_MODAL_HELPEE_INPUT: String = "clone:helpee:input"
const val ID_MODAL_HELPEE_VOUCH: String = "clone:helpee:vouch"
const val ID_MODAL_HELPEE_REPORT: String = "clone:helpee:report"

const val ID_STRING_SELECT_HELPEE_SELECT_PLATFORM = "clone:helpee:select:platform"
const val ID_STRING_SELECT_HELPEE_SELECT_SERVER = "clone:helpee:select:server"
const val ID_STRING_SELECT_HELPEE_SELECT_MODE = "clone:helpee:select:mode"
const val ID_STRING_SELECT_HELPEE_SELECT_CORE = "clone:helpee:select:core"
const val ID_STRING_SELECT_HELPEE_SELECT_TYPE = "clone:helpee:select:type"
val ID_STRING_SELECT_HELPEE_SELECT = setOf(
	ID_STRING_SELECT_HELPEE_SELECT_PLATFORM,
	ID_STRING_SELECT_HELPEE_SELECT_SERVER,
	ID_STRING_SELECT_HELPEE_SELECT_MODE,
	ID_STRING_SELECT_HELPEE_SELECT_CORE,
	ID_STRING_SELECT_HELPEE_SELECT_TYPE,
)

const val ID_STRING_SELECT_HELPER_SELECT_PLATFORM = "clone:helper:select:platform"
//const val ID_STRING_SELECT_HELPER_SELECT_SERVER = "clone:helper:select:server"
const val ID_STRING_SELECT_HELPER_SELECT_MODE = "clone:helper:select:mode"
const val ID_STRING_SELECT_HELPER_SELECT_CORE = "clone:helper:select:core"
const val ID_STRING_SELECT_HELPER_SELECT_TYPE = "clone:helper:select:type"
val ID_STRING_SELECT_HELPER_SELECT = setOf(
	ID_STRING_SELECT_HELPER_SELECT_PLATFORM,
	//ID_STRING_SELECT_HELPER_SELECT_SERVER,
	ID_STRING_SELECT_HELPER_SELECT_MODE,
	ID_STRING_SELECT_HELPER_SELECT_CORE,
	ID_STRING_SELECT_HELPER_SELECT_TYPE,
)

const val ID_STRING_SELECT_VALUE_HELPER_COMPLETED_GOOD = "clone:helper:compl:good"
const val ID_STRING_SELECT_VALUE_HELPER_COMPLETED_MULTIPLE = "clone:helper:compl:multi"
const val ID_STRING_SELECT_VALUE_HELPER_COMPLETED_AFK = "clone:helper:compl:afk"
const val ID_STRING_SELECT_VALUE_HELPER_COMPLETED_EMPTY = "clone:helper:compl:empty"
const val ID_STRING_SELECT_VALUE_HELPER_COMPLETED_FAIL = "clone:helper:compl:fail"
const val ID_STRING_SELECT_HELPER_COMPLETED = "clone:helper:compl"

class CloneHandler(
	private val auriel: Auriel,
	private val guild: AGuild,
	private val channelHelpee: GuildMessageChannel,
	private val channelHelper: GuildMessageChannel,
) {
	
	val helpeeDataCollection: MongoCollection<CloneDataHelpee> = auriel.database.getCollection("${guild.id}-clone-helpee", CloneDataHelpee::class.java)
	val data: CloneData
	
	val queuesById: MutableMap<String, CloneQueue> = mutableMapOf()
	val queues: MutableList<CloneQueue> = mutableListOf()
	
	val helpeeDataById: MutableMap<String, CloneDataHelpee> = mutableMapOf()
	
	val messageHelpeeInstructions: MessageCreateData = guild.data.getAsMessageCreateData(ID_MESSAGE_HELPEE_INSTRUCTIONS)!!
	val messageHelperInstructions: MessageCreateData = guild.data.getAsMessageCreateData(ID_MESSAGE_HELPER_INSTRUCTIONS)!!
	val messageHelpeeGameInfo: MessageCreateData = guild.data.getAsMessageCreateData(ID_MESSAGE_HELPEE_GAME_INFO)!!
	val messageHelperGameInfo: MessageCreateData = guild.data.getAsMessageCreateData(ID_MESSAGE_HELPER_GAME_INFO)!!
	
	private var messagePinHelpee: PinMessage? = null
	private var messagePinHelper: PinMessage? = null
	
	private var lastHelpeeMessageUpdate: Long = 0L
	
	private val valuesById: MutableMap<String, CloneValues> = mutableMapOf()
	
	fun saveData(dataHelpee: CloneDataHelpee) = helpeeDataCollection.updateOne(dataHelpee, options = UpdateOptions().upsert(true))
	
	fun removeData(dataHelpee: CloneDataHelpee) = helpeeDataCollection.deleteOneById(dataHelpee._id)
	
	fun onModalInteraction(event: ModalInteractionEvent): Boolean {
		if (!event.channelType.isMessage) return false
		return when (event.modalId) {
			ID_MODAL_HELPEE_INPUT -> {
				submitRequest(event)
				true
			}
			ID_MODAL_HELPEE_VOUCH -> {
				giveVouch(event)
				true
			}
			ID_MODAL_HELPEE_REPORT -> {
				report(event)
				true
			}
			else -> false
		}
	}
	
	fun report(event: ModalInteractionEvent) {
		val member = event.member!!
		val helpee = valuesById[member.id]
		if (helpee is HelpeeCloneValues) {
			val request = helpee.request
			if (request != null) {
				val helper = request.helper
				helper.data.restricted = true
				val actions = mutableListOf<RestAction<out Any>>()
				actions.add(
					event.editMessage(MessageEdit {
						content = "${helpee.member.effectiveName} is reporting a problem."
					}).flatMap {
						event.channel.sendMessage("${auriel.warren.asMention} ${helper.member.asMention}")
					}
				)
				if (!helper.deleted) actions.add(helper.thread.removeThreadMember(helper.member))
				helper.request?.let { actions.add(it.helpee.thread.removeThreadMember(helper.member)) }
				//actions.add(helpee.thread.addThreadMember(auriel.warren))
				//actions.add(helpee.thread.addThreadMember(helper.member))
				RestAction.allOf(actions).queue_()
			} else {
				event.reply_("You were not being helped with Diablo Clones.").queue_()
			}
		} else {
			event.reply_("You do not have an active request.").queue_()
		}
	}
	
	fun unrestrict(member: Member): Boolean {
		val data = member.a().data.dataCloneHelper
		if (data != null && data.restricted) {
			data.restricted = false
			return true
		} else {
			return false
		}
	}
	
	fun giveVouch(event: ModalInteractionEvent) {
		val member = event.member!!
		val helpee = valuesById[member.id]
		if (helpee is HelpeeCloneValues) {
			val request = helpee.request
			if (request != null) {
				val helper = request.helper
				request.hasBeenVouched = true
				val reason = event.getValue("vouch")!!.asString
				if (helper.member.a().giveVouch(helpee.member, reason, true)) {
					event.deferReply().flatMap { hook ->
						channelHelpee.sendMessage("${helpee.member.asMention} has given ${helper.member.asMention} a vouch for: **$reason**.").flatMap {
							hook.editOriginal(MessageEdit {
								content = "You gave **${helper.member.effectiveName}** a vouch with the reason: `$reason`\n\n" +
										"Your request is finished and this channel will be deleted momentarily..."
							})
						}
					}.delay(3, TimeUnit.SECONDS).queue_ { finishRequest(request) }
				} else {
					event.reply_("Unable to give vouch, there's a mistake somewhere.\n\n" +
							"Your request is finished and this channel will be deleted momentarily...")
						.delay(3, TimeUnit.SECONDS).queue_ { finishRequest(request) }
				}
			} else {
				event.reply_("You were not being helped with Diablo Clones.").queue_()
			}
		} else {
			event.reply_("You do not have an active request.").queue_()
		}
	}
	
	fun finishRequest(request: CloneRequest) {
		val helpee = request.helpee
		val helper = request.helper
		/*helpee.data.queue?.let {
			helpee.data.queue = null
		}*/
		helper.request?.let { if (it == request) helper.request = null }
		deleteHelpeeData(helpee.member)
		valuesById.remove(helpee.member.id)
		helpee.deleted = true
		helpee.thread.let {
			it.parentMessageChannel.asTextChannel().deletePersonalThread(helpee.member)
		}
		queueUpdated()
	}
	
	fun onButtonInteraction(event: ButtonInteractionEvent): Boolean {
		if (!event.channelType.isMessage) return false
		val member = event.member!!
		when (event.customId) {
			ID_BUTTON_HELPEE_REQUEST_MAKE -> {
				makeRequest(event)
				return true
			}
			ID_BUTTON_HELPEE_REQUEST_CANCEL -> {
				cancelRequest(event)
				return true
			}
			ID_BUTTON_HELPEE_INPUT_OPEN -> {
				val helpee = valuesById[member.id]
				if (helpee is HelpeeCloneValues) {
					if (helpee.request != null) {
						event.reply_("You already have a helper assigned, so it is too late to update the information.").queue_()
						return true
					}
					event.replyModal(createModalHelpeeInput(helpee)).queue_()
				} else {
					event.reply_("You do not have a pending request.").queue_()
				}
				return true
			}
			ID_BUTTON_HELPER_BEGIN -> {
				beginHelping(event)
				return true
			}
			ID_BUTTON_HELPER_STOP -> {
				stopHelping(event)
				return true
			}
			ID_BUTTON_HELPER_SUBMIT -> {
				tryFinishHelperSelections(event)
				return true
			}
			ID_BUTTON_HELPER_JOIN -> {
				val helper = valuesById[member.id]
				if (helper is HelperCloneValues) {
					if (helper.data.restricted) {
						event.reply_("Restricted.").queue_()
						return true
					}
					val request = helper.request
					if (request != null) {
						val helpee = request.helpee
						val thread = helpee.thread
						thread.addThreadMember(helper.member).flatMap {
							event.reply_ {
								content = "Click the button below to go to ${thread.asMention}, *${helpee.member.effectiveName}*'s private thread."
								actionRow {
									linkButton("https://discord.com/channels/${thread.guild.id}/${thread.id}", "Go to Private Thread")
								}
							}
						}.queue_()
					} else {
						event.reply_ {
							content = "You are not currently helping with Diablo Clones."
						}.queue_()
					}
				} else {
					event.reply_ {
						content = "You are not currently helping with Diablo Clones."
					}.queue_()
				}
				return true
			}
			ID_BUTTON_HELPEE_CONFIRM -> {
				val helpee = valuesById[member.id]
				if (helpee is HelpeeCloneValues) {
					val request = helpee.request
					if (request != null) {
						val helper = request.helper
						event.editMessage(MessageEdit {
							content = if (helpee.allowRequeue) {
								"Your request is complete. Since the request was marked as a failure, you can jump back into the queue at the front of the line, or just finish the request."
							} else {
								"Your request is complete. You can give a vouch to **${helper.member.effectiveName}** as thanks, or just finish the request."
							}
							actionRow {
								if (helpee.allowRequeue) {
									button(ButtonStyle.SUCCESS, ID_BUTTON_HELPEE_REQUEUE, "Re-queue")
								} else {
									button(ButtonStyle.SUCCESS, ID_BUTTON_HELPEE_VOUCH, "Give Vouch")
								}
								button(ButtonStyle.SECONDARY, ID_BUTTON_HELPEE_FINISH, "Finish")
							}
						}).queue_()
					} else {
						event.reply_ {
							content = "You were not being helped with Diablo Clones."
						}.queue_()
					}
				} else {
					event.reply_ {
						content = "You do not have an active request."
					}.queue_()
				}
				return true
			}
			ID_BUTTON_HELPEE_REPORT -> {
				val helpee = valuesById[member.id]
				if (helpee is HelpeeCloneValues) {
					val request = helpee.request
					if (request != null) {
						val helper = request.helper
						event.replyModal(
							Modal.create(ID_MODAL_HELPEE_REPORT, "Report a problem")
								.addComponents(
									TextDisplay.of("Are you sure there's a big problem?\n" +
											"If you were AFK, ${helper.member.effectiveName} is likely holding the Annihilus for you.\n\n" +
											"**Note:** The moment you press Submit, you will lock the helper out of the system. Deliberate abuse of this system will result in an instant ban. " +
											"This system is for emergencies only.\n" +
											"If this is in error, press Cancel and nothing will happen.\n\n" +
											"If you're sure you were scammed, stolen from, or something similar, hit Submit then describe the problem. Do not press Submit for any other reason.")
								)
								.build()
						).queue_()
					} else {
						event.reply_ {
							content = "You were not being helped with Diablo Clones."
						}.queue_()
					}
				} else {
					event.reply_ {
						content = "You do not have an active request."
					}.queue_()
				}
				return true
			}
			ID_BUTTON_HELPER_NEXT -> {
				val helper = valuesById[member.id]
				if (helper is HelperCloneValues) {
					if (helper.data.restricted) {
						event.reply_("Restricted.").queue_()
						return true
					}
					helpNext(helper, event)
				} else {
					event.reply_ {
						content = "You are not currently helping with Diablo Clones."
					}.queue_()
				}
				return true
			}
			ID_BUTTON_HELPEE_VOUCH -> {
				val helpee = valuesById[member.id]
				if (helpee is HelpeeCloneValues) {
					val request = helpee.request
					if (request != null) {
						val helper = request.helper
						if (!request.hasBeenVouched) {
							event.replyModal(
								Modal.create(ID_MODAL_HELPEE_VOUCH, "Give a Vouch")
									.addComponents(
										TextDisplay.of("If you'd like to give *${helper.member.effectiveName}* a vouch, which adds to their record of helping the community, you can do so here.\n" +
												"Write a vouch reason you would like to be included. It can be a simple thanks, the stats of your Annihilus, or nothing at all.\n" +
												"Empty reasons will default to **Diablo Clone** as the reason.\n" +
												"The vouch will be visible to all in the clone assistance channel."),
										Label.of(
											"Vouch Reason",
											TextInput("vouch", TextInputStyle.SHORT, required = false, placeholder = "Diablo Clone")
										)
									).build()
							).queue_()
						} else {
							event.reply_ {
								content = "Nice try, but you already vouched *${helper.member.effectiveName}*."
							}.queue_()
						}
					} else {
						event.reply_ {
							content = "You were not being helped with Diablo Clones."
						}.queue_()
					}
				} else {
					event.reply_ {
						content = "You do not have an active request."
					}.queue_()
				}
				return true
			}
			ID_BUTTON_HELPEE_FINISH -> {
				val helpee = valuesById[member.id]
				if (helpee is HelpeeCloneValues) {
					val request = helpee.request
					if (request != null) {
						event.deferEdit().queue_ { finishRequest(request) }
					} else {
						event.reply_ {
							content = "You were not being helped with Diablo Clones."
						}.queue_()
					}
				} else {
					event.reply_ {
						content = "You do not have an active request."
					}.queue_()
				}
				return true
			}
			ID_BUTTON_HELPEE_REQUEUE -> {
				val helpee = valuesById[member.id]
				if (helpee is HelpeeCloneValues) {
					if (helpee.allowRequeue) {
						requeue(helpee, event)
						helpee.allowRequeue = false
					} else {
						event.reply_ {
							content = "You are not allowed to re-queue from this state."
						}.queue_()
					}
				} else {
					event.reply_ {
						content = "You do not have an active request."
					}.queue_()
				}
				return true
			}
		}
		return false
	}
	
	fun helpNext(helper: HelperCloneValues, event: ComponentInteraction? = null) {
		val queue = helper.data.queue!!
		val helpee = queue.pollHelpee()
		val isHelpeeNull = helpee == null
		val messageEmpty =
			if (event != null) "The ${queue.getDisplayName()} queue is currently empty, you will be notified once you're assigned to someone that joins the queue."
			else "The requester suddenly cancelled their request. The ${queue.getDisplayName()} queue is currently empty, you will be notified once you're assigned to someone that joins the queue."
		if (event != null) {
			event.editMessage(MessageEdit {
				useComponentsV2 = true
				container {
					if (isHelpeeNull) {
						text { content = messageEmpty }
					} else {
						text { content = "You have been assigned to help **${helpee.member.effectiveName}**." }
					}
				}
			}).queue_()
		} else {
			if (isHelpeeNull) {
				helper.thread.sendMessage(MessageCreate {
					useComponentsV2 = true
					container {
						text { content = messageEmpty }
					}
				}).queue_()
			}
		}
		if (isHelpeeNull) {
			queue.helpersWaiting.add(helper)
		} else {
			startHelpProcess(helpee, helper)
		}
		queueUpdated()
	}
	
	fun requeue(helpee: HelpeeCloneValues, event: ComponentInteraction? = null) {
		val queue = helpee.data.queue!!
		var helper: HelperCloneValues? = null
		synchronized(queue.helpersWaiting) {
			if (queue.helpersWaiting.isNotEmpty()) {
				helper = queue.pollRandomWaitingHelper()!!
			}
		}
		val isHelperNull = helper == null
		val message =
			if (isHelperNull) "You have been returned to the front of the queue. You should receive help shortly."
			else "The previous helper spontaneously left. **${helper.member.effectiveName}** is now your helper."
		if (event != null) {
			event.editMessage(message).setComponents().queue_()
		} else {
			helpee.thread.sendMessage(message).queue_()
		}
		if (isHelperNull) {
			queue.addHelpeeToFront(helpee)
			queueUpdated()
		} else {
			startHelpProcess(helpee, helper, true)
		}
	}
	
	fun onStringSelectInteraction(event: StringSelectInteractionEvent): Boolean {
		if (!event.channelType.isMessage) return false
		val customId = event.customId
		return when (customId) {
			in ID_STRING_SELECT_HELPEE_SELECT -> {
				onStringSelectInteractionHelpee(event)
				true
			}
			in ID_STRING_SELECT_HELPER_SELECT -> {
				onStringSelectInteractionHelper(event)
				true
			}
			ID_STRING_SELECT_HELPER_COMPLETED -> {
				onStringSelectInteractionHelperCompleted(event)
				true
			}
			else -> false
		}
	}
	
	fun onStringSelectInteractionHelperCompleted(event: StringSelectInteractionEvent) {
		val member = event.member!!
		val idMember = member.id
		val helper = valuesById[idMember]
		if (helper is HelperCloneValues) {
			if (helper.data.restricted) {
				event.reply_("Restricted.").queue_()
				return
			}
			if (helper.request != null) {
				val request = helper.request!!
				val helpee = request.helpee
				var textHelpee: String?
				var textHelper: String?
				var success: Boolean
				var includeLink = false
				val threadHelpee = helpee.thread
				when (event.values[0]) {
					ID_STRING_SELECT_VALUE_HELPER_COMPLETED_GOOD -> {
						textHelpee = "**${helper.member.effectiveName}** has marked the request as completed successfully.\n" +
								"You can either confirm the result or report a scam/thief."
						textHelper = "You marked ${helpee.member.effectiveName}'s request as completed successfully."
						success = true
						threadHelpee.removeThreadMember(helper.member).queue_()
					}
					ID_STRING_SELECT_VALUE_HELPER_COMPLETED_MULTIPLE -> {
						textHelpee = "**${helper.member.effectiveName}** has marked the request as completed successfully, " +
								"but there were multiple players in the game so the Annihilus may have been claimed by another.\n" +
								"You can either confirm the result or report a scam/thief."
						textHelper = "You marked ${helpee.member.effectiveName}'s request as completed successfully but uncertain if the requester got the Annihilus due to multiple players present."
						success = true
						threadHelpee.removeThreadMember(helper.member).queue_()
					}
					ID_STRING_SELECT_VALUE_HELPER_COMPLETED_AFK -> {
						textHelpee = "**${helper.member.effectiveName}** has marked the request as completed successfully but you were AFK.\n" +
								"They are holding your Annihilus for you until you return. Simply @mention them here (see their username in the member list on the right) and " +
								"let them know you've returned and the two of you can organize meeting up for your Annihilus once they are available to.\n" +
								"You can either confirm the result or report a scam/thief."
						textHelper = "You marked ${helpee.member.effectiveName}'s request as completed successfully but the player was AFK."
						success = true
						includeLink = true
						threadHelpee.addThreadMember(helper.member).queue_()
					}
					ID_STRING_SELECT_VALUE_HELPER_COMPLETED_EMPTY -> {
						textHelpee = "**${helper.member.effectiveName}** has marked the request as unable to be completed due to the game being empty or Diablo Clone was already slain.\n" +
								"You can either confirm the result or report a scam/thief."
						textHelper = "You marked ${helpee.member.effectiveName}'s request as unable to be completed due to the game being empty or Diablo Clone was already slain."
						success = false
						threadHelpee.removeThreadMember(helper.member).queue_()
					}
					ID_STRING_SELECT_VALUE_HELPER_COMPLETED_FAIL -> {
						textHelpee = "**${helper.member.effectiveName}** has marked the request as unable to be completed due to not being able to kill Diablo Clone or urgently needing to leave.\n" +
								"You can either confirm the result or report a scam/thief."
						textHelper = "You marked ${helpee.member.effectiveName}'s request as unable to be completed due to not being able to kill Diablo Clone or urgently needing to leave."
						success = false
						helpee.allowRequeue = true
						threadHelpee.removeThreadMember(helper.member).queue_()
					}
					else -> {
						event.reply_("Unknown interaction, Warren broke something.").queue_()
						return
					}
				}
				
				helpee.thread.send {
					content = textHelpee
					actionRow {
						button(ButtonStyle.SUCCESS, ID_BUTTON_HELPEE_CONFIRM, "Confirm")
						button(ButtonStyle.DANGER, ID_BUTTON_HELPEE_REPORT, "Report a problem")
					}
				}.queue_()
				
				event.editMessage(MessageEdit {
					useComponentsV2 = true
					container {
						text { content = textHelper }
						actionRow {
							button(ButtonStyle.PRIMARY, ID_BUTTON_HELPER_NEXT, "Next Request")
							button(ButtonStyle.DANGER, ID_BUTTON_HELPER_STOP, "Stop Helping")
							if (includeLink) linkButton("https://discord.com/channels/${threadHelpee.guild.id}/${threadHelpee.id}", "Go to Private Thread")
						}
					}
				}).queue_()
				if (success) {
					helper.data.completed += 1
					helper.member.a().saveData()
					helpee.data.queue!!.completed += 1
				}
				helper.request = null
				
			} else {
				event.reply_("You are not currently assigned to help anyone.").queue_()
			}
		} else {
			event.reply_("You are not currently helping with Diablo Clones.").queue_()
		}
	}
	
	fun onStringSelectInteractionHelper(event: StringSelectInteractionEvent) {
		val member = event.member!!
		val idMember = member.id
		val userA = member.a()
		val helper = valuesById[idMember]
		if (helper is HelperCloneValues) {
			val data = helper.data
			if (data.restricted) {
				event.reply_("Restricted.").queue_()
				return
			}
			if (data.selections == null) {
				data.selections = CloneDataHelperSelection()
			}
			val selections = data.selections!!
			when (event.customId) {
				ID_STRING_SELECT_HELPER_SELECT_PLATFORM -> {
					selections.platform = Platform.valueOf(event.selectedOptions[0].value)
					userA.saveData()
					//tryFinishHelperSelections(event, helper)
					event.deferEdit().queue_()
				}
				ID_STRING_SELECT_HELPER_SELECT_MODE -> {
					selections.mode = Mode.valueOf(event.selectedOptions[0].value)
					userA.saveData()
					//tryFinishHelperSelections(event, helper)
					event.deferEdit().queue_()
				}
				ID_STRING_SELECT_HELPER_SELECT_CORE -> {
					selections.core = Core.valueOf(event.selectedOptions[0].value)
					userA.saveData()
					//tryFinishHelperSelections(event, helper)
					event.deferEdit().queue_()
				}
				ID_STRING_SELECT_HELPER_SELECT_TYPE -> {
					selections.type = LadderType.valueOf(event.selectedOptions[0].value)
					userA.saveData()
					//tryFinishHelperSelections(event, helper)
					event.deferEdit().queue_()
				}
				else -> {
					event.reply_("Unknown interaction, Warren broke something.").queue_()
				}
			}
		} else {
			event.reply_("You are not currently helping with Diablo Clones.").queue_()
		}
	}
	
	fun onStringSelectInteractionHelpee(event: StringSelectInteractionEvent) {
		val member = event.member!!
		val idMember = member.id
		val helpee = valuesById[idMember]
		if (helpee is HelpeeCloneValues) {
			val data = helpee.data
			if (data.selections == null) {
				data.selections = CloneDataHelpeeSelection()
			}
			val selections = data.selections!!
			when (event.customId) {
				ID_STRING_SELECT_HELPEE_SELECT_PLATFORM -> {
					selections.platform = Platform.valueOf(event.selectedOptions[0].value)
					saveData(data)
					tryFinishHelpeeSelections(event, helpee)
				}
				ID_STRING_SELECT_HELPEE_SELECT_SERVER -> {
					selections.server = Server.valueOf(event.selectedOptions[0].value)
					saveData(data)
					tryFinishHelpeeSelections(event, helpee)
				}
				ID_STRING_SELECT_HELPEE_SELECT_MODE -> {
					selections.mode = Mode.valueOf(event.selectedOptions[0].value)
					saveData(data)
					tryFinishHelpeeSelections(event, helpee)
				}
				ID_STRING_SELECT_HELPEE_SELECT_CORE -> {
					selections.core = Core.valueOf(event.selectedOptions[0].value)
					saveData(data)
					tryFinishHelpeeSelections(event, helpee)
				}
				ID_STRING_SELECT_HELPEE_SELECT_TYPE -> {
					selections.type = LadderType.valueOf(event.selectedOptions[0].value)
					saveData(data)
					tryFinishHelpeeSelections(event, helpee)
				}
				else -> {
					event.reply_("Unknown interaction, Warren broke something.").queue_()
				}
			}
		} else {
			event.reply_("You do not have a pending request.").queue_()
		}
	}
	
	init {
		if (guild.data.cloneData != null) {
			data = guild.data.cloneData!!
		} else {
			data = CloneData()
			guild.data.cloneData = data
		}
		messagePinHelpee = auriel.specialMessageHandler.sendPinMessage(2, channelHelpee, formatMessageHelpeeInstructions())
		messagePinHelper = auriel.specialMessageHandler.sendPinMessage(4, channelHelper, messageHelperInstructions)
	}
	
	fun tryFinishHelpeeSelections(event: StringSelectInteractionEvent, helpee: HelpeeCloneValues) {
		if (helpee.data.selections != null) {
			val selections = helpee.data.selections!!
			if (selections.hasSelectedAll()) {
				if (selections.platform!! == Platform.PC && selections.server!! == Server.NOT_APPLICABLE) {
					event.reply_("PC players must specify a server!").queue_()
				} else {
					event.replyModal(createModalHelpeeInput(helpee)).flatMap {
						event.message.editMessage(MessageEdit {
							content = "You have selected your game options, now provide the remaining information in the pop-up menu found by clicking the button below."
							useComponentsV2 = false
							actionRow {
								primaryButton(ID_BUTTON_HELPEE_INPUT_OPEN, "Submit Game Info")
							}
						})
					}.queue_()
				}
			} else {
				event.deferEdit().queue_()
			}
		} else {
			event.deferEdit().queue_()
		}
	}
	
	fun tryFinishHelperSelections(event: ButtonInteractionEvent) {
		val member = event.member!!
		val idMember = member.id
		val helper = valuesById[idMember]
		if (helper is HelperCloneValues) {
			if (helper.data.restricted) {
				event.reply_("Restricted.").queue_()
				return
			}
			if (helper.data.selections != null) {
				val selections = helper.data.selections!!
				if (selections.hasSelectedAll()) {
					val queue = computeQueue(selections)
					queue.helpers.add(idMember)
					helper.data.queue = queue
					val helpee = queue.pollHelpee()
					if (helpee != null) {
						event.editMessage("You are now helping players in the ${queue.getDisplayName()} queue.").setComponents().queue_()
						startHelpProcess(helpee, helper)
					} else {
						queue.helpersWaiting.add(helper)
						event.editMessage("The ${queue.getDisplayName()} queue is currently empty, you will be notified once you're assigned to someone that joins the queue.").setComponents().queue_()
					}
					queueUpdated()
				} else {
					event.reply_("You must select an option from each category to continue.").queue_()
				}
			} else {
				event.reply_("You must select an option from each category to continue.").queue_()
			}
		} else {
			event.reply_("You are not currently helping with Diablo Clones.").queue_()
		}
	}
	
	fun startHelpProcess(helpee: HelpeeCloneValues, helper: HelperCloneValues, notify: Boolean = false) {
		/*
		val dataHelpee = helpee.getCloneHelpeeData()
		val dataHelper = helper.getCloneHelperData()
		val threadHelpee = dataHelpee?.idPersonalThread.let { channelHelpee.a().privateThreadsById[it] }
		val threadHelper = dataHelper?.idPersonalThread.let { channelHelper.a().privateThreadsById[it] }
		val isNullDataHelpee = dataHelpee == null
		val isNullDataHelper = dataHelper == null
		if (isNullDataHelpee || isNullDataHelper || threadHelpee == null || threadHelper == null) {
			if (isNullDataHelpee) {
				auriel.warren(NullPointerException("helpeeData was null when starting the help process, ${helpee.fullMention()} is probably stuck in limbo now.").stackTraceToString())
			} else if (threadHelpee != null) {
				dataHelpee.queue!!.addPlayerToFront(helpee)
				threadHelpee.send {
					content = "An error has occured when trying to assign you to a helper. You have been placed back in the front of the queue."
				}.queue_()
			} else {
				auriel.warren("${helpee.fullMention()} has helpee data but not a private thread, they're stuck in some limbo state.")
			}
			if (isNullDataHelper) {
				auriel.warren(NullPointerException("helperData was null when starting the help process, ${helper.fullMention()} is probably stuck in limbo now.").stackTraceToString())
			} else if (threadHelper != null) {
				threadHelper.send {
					content = "An error has occured when trying to assign you to a helpee."
					actionRow {
						button(ButtonStyle.SUCCESS, "clone:helper:retry-help", "Retry")
					}
				}.queue_()
			} else {
				auriel.warren("${helper.fullMention()} has helper data but not a private thread, they're stuck in some limbo state.")
			}
			return
		}
		*/
		val request = CloneRequest(helpee, helper)
		helpee.request = request
		helper.request = request
		helpee.thread.send {
			content = "**${helper.member.effectiveName}** has been assigned as your helper. They should join your game shortly. They will enter this channel and message you if there is a problem."
		}.queue_()
		var actionChain: RestAction<out Any> = helper.thread.sendMessage(formatMessageHelperGameInfo(request))
		if (notify) actionChain = actionChain.flatMap { helper.thread.sendMessage(helper.member.asMention) }
		actionChain.queue_()
	}
	
	fun createModalHelpeeInput(helpee: HelpeeCloneValues): Modal {
		val inputs = helpee.data.inputs
		return Modal.create(ID_MODAL_HELPEE_INPUT, "Game Information")
			.addComponents(
				TextDisplay.of(
					" - Quick Play game names and game passwords can be revealed in the Party Screen (P) by pressing the eye.\n" +
							" - For Quick Play games, the game name is the same as the password in the case sensitivity of **Game####..**"
				),
				Label.of(
					"Game Name",
					"The name of the game.",
					TextInput("gameName", TextInputStyle.SHORT) {
						placeholder = "Enter your game name"
						required = true
						inputs?.gameName?.let { if (it.isNotBlank()) value = it }
					}
				),
				Label.of(
					"Password",
					"The password of the game. Press the eye on the Party Screen (P) to reveal it.",
					TextInput("password", TextInputStyle.SHORT) {
						placeholder = "Write game password, or nothing if public"
						required = false
						inputs?.password?.let { if (it.isNotBlank()) value = it }
					}
				),
				Label.of(
					"Spawn Location",
					"Where did Diablo Clone spawn if he has already been spawned?",
					TextInput("location", TextInputStyle.SHORT) {
						placeholder = "Write area or superunique, or nothing if not spawned"
						required = false
						inputs?.location?.let { if (it.isNotBlank()) value = it }
					}
				),
				Label.of(
					"Furthest Act",
					"The furthest act you can travel to in Hell.",
					dev.minn.jda.ktx.interactions.components.StringSelectMenu("act") {
						option("Act 1", "Act 1")
						option("Act 2", "Act 2")
						option("Act 3", "Act 3")
						option("Act 4", "Act 4")
						option("Act 5", "Act 5")
						require(true)
						setRequiredRange(1, 1)
						inputs?.furthestAct?.let { if (it.isNotBlank()) setDefaultValues(it) }
					}
				)
			).build()
	}
	
	fun stop() {
		messagePinHelpee?.let {
			it.delete()
			messagePinHelpee = null
		}
		messagePinHelper?.let {
			it.delete()
			messagePinHelper = null
		}
		guild.data.cloneData = null
		guild.saveData()
		helpeeDataCollection.drop()
		if (valuesById.isNotEmpty()) {
			RestAction.allOf(valuesById.values.map {
				if (it is HelperCloneValues) {
					val data = it.data
					data.restricted = false
					data.idPersonalThread = null
					it.member.a().saveData()
				}
				it.thread.delete()
			}).queue_()
		}
		channelHelpee.let {
			val privateThreadsById = it.a().privateThreadsById
			if (privateThreadsById.isNotEmpty()) {
				privateThreadsById.clear()
			}
		}
		channelHelper.let {
			val privateThreadsById = it.a().privateThreadsById
			if (privateThreadsById.isNotEmpty()) {
				privateThreadsById.clear()
			}
		}
		valuesById.clear()
	}
	
	private fun createHelpeeData(member: Member, thread: ThreadChannel): CloneDataHelpee {
		val helpeeData = CloneDataHelpee(member.id, thread.id)
		helpeeDataById[member.id] = helpeeData
		saveData(helpeeData)
		return helpeeData
	}
	
	private fun deleteHelpeeData(member: Member) {
		if (helpeeDataById.containsKey(member.id)) {
			val helpeeData = helpeeDataById.remove(member.id)!!
			removeData(helpeeData)
		}
	}
	
	fun beginHelping(event: ButtonInteractionEvent) {
		val member = event.member!!
		val userA = member.a()
		val helperData = userA.data.computeCloneData()
		val helper = valuesById[member.id]
		if (helper != null) {
			if (helper is HelperCloneValues) {
				if (helper.data.restricted) {
					event.reply_("Restricted.").queue_()
					return
				}
				event.deferReply(true).submit().thenCompose { hook ->
					val thread = helper.thread
					thread.addThreadMember(member).submit().thenCompose {
						hook.editOriginal(MessageEdit {
							content = "Click the button below to go to ${thread.asMention}, your private thread, to start helping others."
							useComponentsV2 = false
							actionRow {
								linkButton("https://discord.com/channels/${thread.guild.id}/${thread.id}", "Go to Private Thread")
							}
						}).submit()
					}
				}
			} else {
				event.reply_("Cannot help while being helped.").queue_()
			}
		} else {
			event.deferReply(true).flatMap { hook ->
				event.channel.asTextChannel().createPersonalThread(member).flatMap { thread ->
					val values = HelperCloneValues(member, helperData, thread)
					valuesById[member.id] = values
					helperData.idPersonalThread = thread.id
					thread.send {
						content = "To begin, select the category information for the category you want to help with. If the values are already filled with what you want, then re-select one of the " +
								"options to force an update."
						useComponentsV2 = false
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPER_SELECT_PLATFORM) {
								placeholder = "Select your platform"
								setRequiredRange(1, 1)
								Platform.entries.forEach {
									option(it.display, it.name)
								}
								helperData.selections?.platform?.let { setDefaultValues(it.name) }
							}
						}
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPER_SELECT_MODE) {
								placeholder = "Select your game mode"
								setRequiredRange(1, 1)
								Mode.entries.filter { it.hasClone }.forEach {
									option(it.display, it.name)
								}
								helperData.selections?.mode?.let { setDefaultValues(it.name) }
							}
						}
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPER_SELECT_CORE) {
								placeholder = "Select your core"
								setRequiredRange(1, 1)
								Core.entries.forEach {
									option(it.display, it.name)
								}
								helperData.selections?.core?.let { setDefaultValues(it.name) }
							}
						}
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPER_SELECT_TYPE) {
								placeholder = "Select your ladder type"
								setRequiredRange(1, 1)
								LadderType.entries.forEach {
									option(it.display, it.name)
								}
								helperData.selections?.type?.let { setDefaultValues(it.name) }
							}
						}
						actionRow {
							primaryButton(ID_BUTTON_HELPER_SUBMIT, "Submit")
						}
					}.flatMap {
						hook.editOriginal(MessageEdit {
							content = "To start helping others, head to ${thread.asMention}, your private thread."
							useComponentsV2 = false
							actionRow {
								linkButton("https://discord.com/channels/${thread.guild.id}/${thread.id}", "Go to Private Thread")
							}
						})
					}
				}
			}.queue_()
		}
	}
	
	fun stopHelping(event: ButtonInteractionEvent) {
		val member = event.member!!
		val helper = valuesById[member.id]
		if (helper is HelperCloneValues) {
			if (helper.data.restricted) {
				event.reply_("Restricted.").queue_()
				return
			}
			val idMember = helper.member.id
			if (event.channel.asGuildMessageChannel() == channelHelper) {
				event.deferReply(true)
					.submit()
					.thenAccept { hook ->
						event.channel.asTextChannel().deletePersonalThread(helper.member).thenAccept { hook.editOriginal("You are no longer helping others.").submit() }
					}
			} else {
				event.reply_("You will be withdrawn from helping others momentarily...")
					.submit()
					.thenComposeAsync(
						{ event.channel.asThreadChannel().parentChannel.asTextChannel().deletePersonalThread(helper.member) },
						CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
					)
			}
			helper.data.idPersonalThread = null
			helper.data.queue?.let {
				it.helpers.remove(idMember)
				it.helpersWaiting.remove(helper)
				//print("Queue updated when helper stopped helping")
				queueUpdated()
				helper.data.queue = null
			}
			helper.deleted = true
			valuesById.remove(idMember)
			if (helper.request != null) {
				val request = helper.request!!
				helper.request = null
				val helpee = request.helpee
				helpee.thread.removeThreadMember(helper.member).queue_()
				requeue(helpee)
			}
			helper.member.a().saveData()
		} else {
			event.reply_("You do not have a pending request.").queue_()
		}
	}
	
	fun makeRequest(event: ButtonInteractionEvent) {
		val member = event.member!!
		val idMember = member.id
		val helpee = valuesById[idMember]
		if (helpee is HelpeeCloneValues) {
			val thread = helpee.thread
			event.deferReply(true).submit().thenCompose { hook ->
				thread.addThreadMember(member).submit().thenCompose {
					hook.editOriginal(MessageEdit {
						content = "Click the button below to go to ${thread.asMention}, your private thread, to submit your help request."
						useComponentsV2 = false
						actionRow {
							linkButton("https://discord.com/channels/${thread.guild.id}/${thread.id}", "Go to Private Thread")
						}
					}).submit()
				}
			}.errorToWarren()
		} else {
			event.deferReply(true).flatMap { hook ->
				event.channel.asTextChannel().createPersonalThread(member).flatMap { thread ->
					val dataHelpee = createHelpeeData(member, thread)
					val helpee = HelpeeCloneValues(member, dataHelpee, thread)
					valuesById[idMember] = helpee
					thread.send {
						content = "To begin, select the value that pertains to you for each option. A menu will pop up after you have filled out the form.\n" +
								"If the bot does not respond in time, just select it again until it sticks."
						useComponentsV2 = false
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPEE_SELECT_PLATFORM) {
								placeholder = "Select your platform"
								setRequiredRange(1, 1)
								Platform.entries.forEach {
									option(it.display, it.name)
								}
							}
						}
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPEE_SELECT_SERVER) {
								placeholder = "Select your server"
								setRequiredRange(1, 1)
								Server.entries.forEach {
									option(it.display, it.name)
								}
							}
						}
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPEE_SELECT_MODE) {
								placeholder = "Select your game mode"
								setRequiredRange(1, 1)
								Mode.entries.filter { it.hasClone }.forEach {
									option(it.display, it.name)
								}
							}
						}
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPEE_SELECT_CORE) {
								placeholder = "Select your core"
								setRequiredRange(1, 1)
								Core.entries.forEach {
									option(it.display, it.name)
								}
							}
						}
						actionRow {
							stringSelectMenu(ID_STRING_SELECT_HELPEE_SELECT_TYPE) {
								placeholder = "Select your ladder type"
								setRequiredRange(1, 1)
								LadderType.entries.forEach {
									option(it.display, it.name)
								}
							}
						}
					}.flatMap {
						hook.editOriginal(MessageEdit {
							content = "To submit your request, head to ${thread.asMention}, your private thread."
							useComponentsV2 = false
							actionRow {
								linkButton("https://discord.com/channels/${thread.guild.id}/${thread.id}", "Go to Private Thread")
							}
						})
					}
				}
			}.queue_()
		}
		
	}
	
	fun computeQueue(selections: CloneDataSelection): CloneQueue {
		val id = selections.getUniqueIdentifier()
		return if (queuesById.containsKey(id)) {
			queuesById[id]!!
		} else {
			val queue = CloneQueue(selections.platform!!, selections.mode!!, selections.core!!, selections.type!!)
			queuesById[id] = queue
			queues.addLast(queue)
			queue
		}
	}
	
	fun submitRequest(event: ModalInteractionEvent) {
		val member = event.member!!
		val idMember = member.id
		val helpee = valuesById[idMember]!! as HelpeeCloneValues
		val gameName = event.getValue("gameName")!!.asString
		val password = event.getValue("password")?.asString?.let { it.ifEmpty { null } }
		val location = event.getValue("location")?.asString?.let { it.ifBlank { null } }
		val furthestAct = event.getValue("act")!!.asStringList[0]
		if (helpee.data.inputs == null) helpee.data.inputs = CloneDataHelpeeInput()
		val inputs = helpee.data.inputs!!
		inputs.gameName = gameName
		inputs.password = password
		inputs.location = location
		inputs.furthestAct = furthestAct
		saveData(helpee.data)
		if (helpee.request != null) {
			event.reply_("You already have a helper assigned, so it is too late to update the information.").queue_()
			return
		} else if (helpee.data.queue == null) {
			val selections = helpee.data.selections!!
			val queue = computeQueue(selections)
			var helper: HelperCloneValues? = null
			synchronized(queue.helpersWaiting) {
				if (queue.helpersWaiting.isNotEmpty()) {
					helper = queue.pollRandomWaitingHelper()!!
				}
			}
			if (helper != null) {
				event.editMessage(MessageEditData.fromCreateData(formatMessageHelpeeGameInfo(helpee))).queue_()
				startHelpProcess(helpee, helper, true)
				return
			} else {
				queue.addHelpee(helpee)
				queueUpdated()
			}
		}
		event.editMessage(MessageEditData.fromCreateData(formatMessageHelpeeGameInfo(helpee))).queue_()
	}
	
	private fun queueUpdated() {
		queues.sortByDescending { it.getTotalSize() }
		val current = System.currentTimeMillis()
		if (current - lastHelpeeMessageUpdate > (5 * 1000L)) {
			lastHelpeeMessageUpdate = current
			updateHelpeeMessage()
		}
	}
	
	private fun updateHelpeeMessage() {
		messagePinHelpee?.editContent(formatMessageHelpeeInstructions())
	}
	
	private fun formatMessage(message: MessageCreateData, variables: Map<String, String>): MessageCreateData {
		val builder = MessageCreateBuilder.from(message)
		val components = builder.components.map { it as MessageTopLevelComponent }.toMutableList()
		var changedComponents = false
		for ((index, component) in components.withIndex()) {
			if (component.type == Component.Type.CONTAINER) {
				val container = component as Container
				val children = container.components.map { it as ContainerChildComponent }.toMutableList()
				var changedChild = false
				for ((indexChild, child) in children.withIndex()) {
					if (child.type == Component.Type.TEXT_DISPLAY) {
						val textDisplay = child as TextDisplay
						val contentOld = textDisplay.content
						var contentNew = contentOld
						variables.forEach { (variable, value) ->
							contentNew = contentNew.replace("{${variable}}", value)
						}
						if (contentNew != contentOld) {
							children[indexChild] = textDisplay.withContent(contentNew)
							changedChild = true
						}
					} else if (child.type == Component.Type.ACTION_ROW) {
						val actionRow = child as ActionRow
						val componentsRow = actionRow.components.map { it as ActionRowChildComponent }.toMutableList()
						var changedComponentsRow = false
						for ((indexRow, component) in componentsRow.withIndex()) {
							if (component.type == Component.Type.BUTTON) {
								val button = component as Button
								if (button.style == ButtonStyle.LINK) {
									val contentOld = button.url!!
									var contentNew = contentOld
									variables.forEach { (variable, value) ->
										contentNew = contentNew.replace("{${variable}}", value)
									}
									if (contentNew != contentOld) {
										componentsRow[indexRow] = button.withUrl(contentNew)
										changedComponentsRow = true
									}
								}
							}
						}
						if (changedComponentsRow) {
							children[indexChild] = actionRow.withComponents(componentsRow)
							changedChild = true
						}
					}
				}
				if (changedChild) {
					components[index] = container.withComponents(children)
					changedComponents = true
				}
			} else if (component.type == Component.Type.TEXT_DISPLAY) {
				val textDisplay = component as TextDisplay
				val contentOld = textDisplay.content
				var contentNew = contentOld
				variables.forEach { (variable, value) ->
					contentNew = contentNew.replace("{${variable}}", value)
				}
				if (contentNew != contentOld) {
					components[index] = textDisplay.withContent(contentNew)
					changedComponents = true
				}
			}
		}
		if (changedComponents) {
			builder.setComponents(components)
			return builder.build()
		}
		return message
	}
	
	private fun formatMessageHelpeeInstructions(): MessageCreateData {
		val queueList = if (queues.isNotEmpty()) queues.joinToString("\n") { it.getDisplayString() } else "Empty"
		val variables = mapOf(Pair("queueList", queueList))
		return formatMessage(messageHelpeeInstructions, variables)
	}
	
	private fun formatMessageHelpeeGameInfo(helpee: HelpeeCloneValues): MessageCreateData {
		val data = helpee.data
		val inputs = data.inputs
		val variables = mapOf(
			Pair("queueName", data.queue?.getDisplayName() ?: "Unknown"),
			Pair("position", data.position?.toString() ?: "Unknown"),
			Pair("gameName", inputs?.gameName ?: "Unknown"),
			Pair("password", inputs?.password ?: "Public game/no password provided"),
			Pair("location", inputs?.location ?: "Not spawned/no location provided"),
			Pair("furthestAct", inputs?.furthestAct ?: "Unknown"),
			Pair("helpeeChannel", channelHelpee.asMention)
		)
		return formatMessage(messageHelpeeGameInfo, variables)
	}
	
	private fun formatMessageHelperGameInfo(request: CloneRequest): MessageCreateData {
		val helpee = request.helpee
		//val helper = request.helper
		val data = helpee.data
		val inputs = data.inputs
		val variables = mapOf(
			Pair("gameName", inputs?.gameName ?: "Unknown"),
			Pair("password", inputs?.password ?: "Public game/no password provided"),
			Pair("server", data.selections?.server?.display ?: "Unknown"),
			Pair("location", inputs?.location ?: "Not spawned/no location provided"),
			Pair("furthestAct", inputs?.furthestAct ?: "Unknown"),
			Pair("helpeeName", helpee.member.effectiveName),
			//Pair("url", "https://discord.com/channels/${thread.guild.id}/${thread.id}")
		)
		val message = formatMessage(messageHelperGameInfo, variables)
		return message
	}
	
	fun cancelRequest(event: ButtonInteractionEvent) {
		val member = event.member!!
		val idMember = member.id
		val helpee = valuesById[idMember]
		if (helpee is HelpeeCloneValues) {
			helpee.data.queue?.let {
				it.removeHelpee(helpee)
				queueUpdated()
			}
			if (event.channel.asGuildMessageChannel() == channelHelpee) {
				event.deferReply(true)
					.submit()
					.thenAccept { hook ->
						event.channel.asTextChannel().deletePersonalThread(member).thenAccept { hook.editOriginal("Your request has been cancelled.").submit() }
					}
			} else {
				event.reply_("Your request will be deleted momentarily...")
					.submit()
					.thenComposeAsync(
						{ event.channel.asThreadChannel().parentChannel.asTextChannel().deletePersonalThread(member) },
						CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
					)
			}
			deleteHelpeeData(member)
			valuesById.remove(idMember)
			helpee.deleted = true
			if (helpee.request != null) {
				val request = helpee.request!!
				helpee.request = null
				val helper = request.helper
				helpNext(helper)
			}
		} else {
			event.reply_("You do not have a pending request.").queue_()
		}
		
	}
	
	fun hasHelpeeData(id: String) = helpeeDataById.containsKey(id)
	
	fun getHelpeeData(id: String) = helpeeDataById[id]!!
	
}