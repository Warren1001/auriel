package io.github.warren1001.auriel.util

import io.github.warren1001.auriel.queueDelete
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData

class PinMessage(private val repostAfter: Int, channel: GuildMessageChannel, private var data: MessageCreateData) {
	
	val channelId = channel.id
	private var message: Message? = null
	private var messageCount: Int = 0
	
	init {
		update(channel)
	}
	
	private fun update(channel: GuildMessageChannel) {
		messageCount = 0
		message?.delete()?.queueDelete()
		message = null
		channel.sendMessage(data).queue_ { message = it }
	}
	
	fun editContent(data: MessageCreateData) {
		this.data = data
		message?.editMessage(MessageEditData.fromCreateData(data))?.queue_()
	}
	
	fun countMessage(event: MessageReceivedEvent) {
		if (message != null) {
			messageCount++
			if (messageCount >= repostAfter && message!!.id != event.messageId) {
				update(event.channel.asGuildMessageChannel())
			}
		}
	}
	
	fun delete() {
		message?.delete()?.queueDelete()
	}
	
}