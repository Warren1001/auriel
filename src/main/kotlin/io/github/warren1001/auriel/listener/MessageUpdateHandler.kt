package io.github.warren1001.auriel.listener

import discord4j.core.event.domain.message.MessageUpdateEvent
import io.github.warren1001.auriel.NOTHING
import io.github.warren1001.auriel.reply
import reactor.core.publisher.Mono

class MessageUpdateHandler {
	
	fun handle(event: MessageUpdateEvent): Mono<out Any> = event.message.flatMap { message ->
		when (message.content) {
			"!ping 1" -> message.reply("Pong!")
			"!ping 2" -> message.delete()
			"!ping 3" -> message.reply("Pong!")
			else -> NOTHING
		}
	}
	
}