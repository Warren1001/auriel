package io.github.warren1001.auriel

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap

class ReactionMessageHandler(private val auriel: Auriel, private val guildId: Snowflake) {
	
	private val reactionMessages: ConcurrentHashMap<Snowflake, List<ReactionHandler>> = ConcurrentHashMap()
	
	fun onReactionAdd(event: ReactionAddEvent): Flux<out Any> {
		val handlers = reactionMessages[event.messageId]
		return if (handlers != null) Flux.fromIterable(handlers).flatMap { it.onReactionAdd.invoke(event) } else Flux.empty()
	}
	
	fun onReactionRemove(event: ReactionRemoveEvent): Flux<out Any> {
		val handlers = reactionMessages[event.messageId]
		return if (handlers != null) Flux.fromIterable(handlers).flatMap { it.onReactionRemove.invoke(event) } else Flux.empty()
	}
	
	fun registerReactionMessage(messageId: Snowflake, vararg handlers: ReactionHandler) {
		reactionMessages[messageId] = handlers.toList()
	}
	
}