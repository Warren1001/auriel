package io.github.warren1001.auriel.util

import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.queueDelete
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook

class SingleMessage(private val prompt: String, private val complete: String, private val finished: (String) -> Unit) {
	
	private var originalMessage: InteractionHook? = null
	
	fun prompt(event: SlashCommandInteractionEvent) = event.reply_(prompt).queue_ { originalMessage = it }
	
	fun handleMessageReceived(event: MessageReceivedEvent) {
		event.message.delete().queueDelete()
		finished.invoke(event.message.contentRaw)
		originalMessage!!.editMessage(content = complete).queue_()
		originalMessage = null
	}
	
}