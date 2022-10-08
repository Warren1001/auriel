package io.github.warren1001.auriel.util

import io.github.warren1001.auriel.Auriel
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.utils.messages.MessageCreateData

class SpecialMessageHandler(private val auriel: Auriel) {
	
	private val multiSelectMessages: MutableMap<String, MultiSelectMenuMessage<*>> = mutableMapOf()
	private val chainMessages: MutableMap<String, ChainMessage<*,*>> = mutableMapOf()

	fun handleSelectMenu(event: SelectMenuInteractionEvent) {
		if (!event.isFromGuild) return
		if (event.componentId.contains("-")) {
			val id = event.componentId.substringBefore("-")
			multiSelectMessages[id]?.handleSelectMenu(event)?.let {
				if (it) multiSelectMessages.remove(id)
			}
		} else {
			auriel.guilds.handleSelectMenuInteraction(event)
		}
	}
	
	fun handleMessageReceived(event: MessageReceivedEvent) {
		if (!event.isFromGuild) return
		if (chainMessages.contains(event.author.id)) {
			chainMessages[event.author.id]?.handleMessageReceived(event)?.let {
				if (it) chainMessages.remove(event.author.id)
			}
		} else {
			auriel.guilds.handleMessageReceived(event)
		}
	}
	
	fun <T> replyMultiSelectMenuMessage(user: User, values: List<T>, format: String, finishMsg: String, onlyOne: Boolean = true, mustChoose: Boolean = true,
	                                    filter: (T, Int) -> Boolean = { _: T, _: Int -> true },
	                                    optionConverter: (T) -> List<SelectOption>, display: (T) -> String, finished: (MutableMap<T, List<String>>) -> Unit): MessageCreateData {
		val message = MultiSelectMenuMessage(user.id, mutableMapOf(), values, format, finishMsg, onlyOne, mustChoose, filter, optionConverter, display, finished)
		multiSelectMessages[user.id] = message
		return message.createInitialReply()
	}
	
	fun <T, U> replyChainMessageCallback(user: User, values: List<T>, format: String, finishMsg: String, parse: (T, Message) -> U, display: (T) -> String,
			                             createMessage: (MessageCreateData, (InteractionHook) -> Unit) -> Unit, finished: (MutableMap<T, U>) -> Unit) {
		val message = ChainMessage(mutableMapOf(), values, format, finishMsg, parse, display, finished)
		chainMessages[user.id] = message
		message.createInitialReplyCallback(createMessage)
	}

}