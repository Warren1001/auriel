package io.github.warren1001.auriel.util

import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.queueDelete
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageEditData

class SingleMessage(private val prompt: String, private val finished: (String) -> MessageEditData) {
	
	private var originalMessage: InteractionHook? = null
	
	fun prompt(event: SlashCommandInteractionEvent) = event.reply_(prompt).queue_ { originalMessage = it }
	
	fun handleMessageReceived(event: MessageReceivedEvent) {
		event.message.delete().queueDelete()
		val completed = finished.invoke(event.message.contentRaw)
		originalMessage!!.editOriginal(completed).queue_()
		originalMessage = null
	}
	
}