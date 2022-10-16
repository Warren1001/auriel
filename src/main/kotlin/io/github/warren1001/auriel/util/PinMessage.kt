package io.github.warren1001.auriel.util

import io.github.warren1001.auriel.message
import io.github.warren1001.auriel.queueDelete
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.LayoutComponent

class PinMessage(private val repostAfter: Int, channel: GuildMessageChannel, private var content: String, private var components: Collection<LayoutComponent> = emptyList()) {
	
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
		channel.message(content, components).queue_(success = { message = it })
	}
	
	fun editContent(content: String) {
		this.content = content
		message?.editMessage(content)?.queue_()
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