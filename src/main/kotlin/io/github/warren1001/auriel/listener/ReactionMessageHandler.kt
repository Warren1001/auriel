package io.github.warren1001.auriel.listener

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.reaction.ReactionHandler
import io.github.warren1001.auriel.reaction.ReactionMessageContext
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ReactionMessageHandler(private val auriel: Auriel) {
	
	private val reactionMessages: ConcurrentMap<ReactionMessageContext, List<ReactionHandler>> = ConcurrentHashMap()
	private val editTimestamps: HashMap<Snowflake, Long> = HashMap()
	
	fun onReactionAdd(event: ReactionAddEvent): Flux<out Any> {
		if (event.userId == auriel.gateway.selfId) return Flux.empty()
		val handlers = reactionMessages.entries.firstOrNull { it.key.messageId == event.messageId }?.value?.filter { it.emoji == event.emoji }
		return if (handlers != null && System.currentTimeMillis() - editTimestamps.getOrDefault(event.userId, 0L) >= 3000L) {
			Flux.fromIterable(handlers).flatMap { it.onReactionAdd.invoke(event) }.doOnComplete { editTimestamps[event.userId] = System.currentTimeMillis() }
		} else Flux.empty()
	}
	
	fun onReactionRemove(event: ReactionRemoveEvent): Flux<out Any> {
		if (event.userId == auriel.gateway.selfId) return Flux.empty()
		val handlers = reactionMessages.entries.firstOrNull { it.key.messageId == event.messageId }?.value?.filter { it.emoji == event.emoji }
		return if (handlers != null && System.currentTimeMillis() - editTimestamps.getOrDefault(event.userId, 0L) >= 3000L) {
			editTimestamps[event.userId] = System.currentTimeMillis()
			Flux.fromIterable(handlers).flatMap { it.onReactionRemove.invoke(event) }
		} else Flux.empty()
	}
	
	fun registerReactionMessage(ctx: ReactionMessageContext, vararg handlers: ReactionHandler) {
		reactionMessages[ctx] = handlers.toList()
	}
	
	fun removeIf(predicate: (MutableMap.MutableEntry<ReactionMessageContext, List<ReactionHandler>>) -> Boolean) {
		reactionMessages.entries.filter(predicate).forEach { reactionMessages.remove(it.key) }
	}
	
}