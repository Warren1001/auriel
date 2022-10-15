package io.github.warren1001.auriel.util

import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.editMessage
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateData

class ChainMessage<T, U>(private val data: MutableMap<T, U>, private val values: List<T>,
                         private val format: String, private val finishMsg: String, private val parse: (T, Message) -> U,
                         private val display: (T) -> String, private val finished: (MutableMap<T, U>) -> Unit) {
	
	private var index = 0
	private var originalMessage: InteractionHook? = null
	
	fun handleMessageReceived(event: MessageReceivedEvent): Boolean {
		data[values[index]] = parse.invoke(values[index], event.message)
		event.message.delete().queue_()
		index++
		return if (index < values.size) {
			originalMessage!!.editMessage(content = format.format(display.invoke(values[index]))).queue_()
			false
		} else {
			originalMessage!!.editMessage(content = finishMsg).queue_()
			finished.invoke(data)
			true
		}
	}
	
	fun createInitialReply(createMessage: (MessageCreateData) -> InteractionHook) {
		originalMessage = createMessage.invoke(MessageCreate(format.format(display.invoke(values[index]))))
	}
	
	fun createInitialReplyCallback(createMessage: (MessageCreateData, (InteractionHook) -> Unit) -> Unit) {
		createMessage.invoke(MessageCreate(format.format(display.invoke(values[index])))) { originalMessage = it }
	}
	
}