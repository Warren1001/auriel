package io.github.warren1001.auriel.eventhandler

import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.a
import io.github.warren1001.auriel.countMatches
import io.github.warren1001.auriel.d2.clone.InteractionRelation
import io.github.warren1001.auriel.util.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData

class SpecialMessageHandler(private val auriel: Auriel) {
	
	private val multiSelectMessages: MutableMap<String, MultiSelectMenuMessage<*>> = mutableMapOf()
	private val chainMessages: MutableMap<String, ChainMessage<*, *>> = mutableMapOf()
	private val pinMessages: MutableMap<String, PinMessage> = mutableMapOf()
	private val singleMessages: MutableMap<String, SingleMessage> = mutableMapOf()

	fun handleSelectMenu(event: StringSelectInteractionEvent) {
		if (event.componentId.countMatches(":") > 2) {
			val guildId = event.componentId.split(':')[1]
			if (auriel.guilds.getGuild(guildId).cloneHandler.onStringSelectSelection(event) == InteractionRelation.RELATED) return
		}
		if (!event.isFromGuild) return
		if (event.componentId.contains(":")) {
			if (event.guild!!.a().cloneHandler.onStringSelectSelection(event) == InteractionRelation.RELATED) return
		}
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
		} else if (singleMessages.contains(event.author.id)) {
			singleMessages[event.author.id]?.handleMessageReceived(event)?.let { singleMessages.remove(event.author.id) }
		} else {
			if (!auriel.guilds.handleMessageReceived(event) && pinMessages.contains(event.channel.id)) {
				pinMessages[event.channel.id]!!.countMessage(event)
			}
		}
	}
	
	fun <T> replyMultiSelectMenuMessage(user: User, values: List<T>, format: String, finishMsg: String, onlyOne: Boolean = true, mustChoose: Boolean = true,
	                                    filter: (T, Int) -> Boolean = { _: T, _: Int -> true },
	                                    optionConverter: (T) -> List<SelectOption>, display: (T) -> String, finished: (MutableMap<T, List<String>>) -> Unit): MessageCreateData {
		val message = MultiSelectMenuMessage(user.id, mutableMapOf(), values, format, finishMsg, onlyOne, mustChoose, filter, optionConverter, display, finished)
		multiSelectMessages[user.id] = message
		return message.createInitialReply()
	}
	
	fun <T> replyMultiSelectMenuMessage(builder: MultiSelectMenuMessageBuilder<T>.() -> Unit): MessageCreateData {
		val build = MultiSelectMenuMessageBuilder<T>().apply(builder)
		val message = build.build()
		multiSelectMessages[build.userId] = message
		return message.createInitialReply()
	}
	
	fun <T, U> replyChainMessageCallback(user: User, values: List<T>, format: String, finishMsg: String, parse: (T, Message) -> U, validationMessage: String, display: (T) -> String,
			                             createMessage: (MessageCreateData, (InteractionHook) -> Unit) -> Unit, finished: (MutableMap<T, U>) -> Unit) {
		val message = ChainMessage(mutableMapOf(), values, format, finishMsg, parse, validationMessage, display, finished)
		chainMessages[user.id] = message
		message.createInitialReplyCallback(createMessage)
	}
	
	fun <T, U> replyChainMessageCallback(builder: ChainMessageBuilder<T, U>.() -> Unit) {
		val build = ChainMessageBuilder<T, U>().apply(builder)
		val message = build.build()
		chainMessages[build.userId!!] = message
		message.createInitialReplyCallback(build.createMessage!!)
	}
	
	fun sendPinMessage(repostAfter: Int, channel: GuildMessageChannel, content: String, components: Collection<LayoutComponent>): PinMessage {
		val message = PinMessage(repostAfter, channel, content, components)
		pinMessages[channel.id] = message
		return message
	}
	
	fun deletePinMessage(message: PinMessage) {
		pinMessages.remove(message.channelId)
		message.delete()
	}
	
	fun replySingleMessage(event: SlashCommandInteractionEvent, prompt: String, finished: (String) -> MessageEditData) {
		val message = SingleMessage(prompt, finished)
		singleMessages[event.user.id] = message
		message.prompt(event)
	}

}