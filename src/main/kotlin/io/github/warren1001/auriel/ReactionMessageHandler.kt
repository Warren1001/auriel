package io.github.warren1001.auriel

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ReactionMessageHandler(private val auriel: Auriel) {
	
	private val reactionMessages: ConcurrentMap<ReactionMessageContext, List<ReactionHandler>> = ConcurrentHashMap()
	private val editTimestamps: HashMap<Snowflake, Long> = HashMap()
	
	fun onReactionAdd(event: ReactionAddEvent): Flux<out Any> {
		if (event.userId == auriel.gateway.selfId) return Flux.empty()
		//println("onReactionAdd called")
		val handlers = reactionMessages.entries.firstOrNull { it.key.messageId == event.messageId }?.value
		return if (handlers != null) {
			//println("ReactionHandlers found")
			if (System.currentTimeMillis() - editTimestamps.getOrDefault(event.userId, 0L) >= 3000L) {
				//println("not on cooldown")
				editTimestamps[event.userId] = System.currentTimeMillis()
				Flux.fromIterable(handlers).filter { it.emoji == event.emoji }.flatMap {
					//println("invoking for emoji: ${it.emoji.asEmojiData()}")
					it.onReactionAdd.invoke(event)
				}
			} else Flux.empty()
		} else Flux.empty()
	}
	
	fun onReactionRemove(event: ReactionRemoveEvent): Flux<out Any> {
		if (event.userId == auriel.gateway.selfId) return Flux.empty()
		//println("onReactionRemove called")
		val handlers = reactionMessages.entries.firstOrNull { it.key.messageId == event.messageId }?.value
		return if (handlers != null) {
			//println("ReactionHandlers found")
			if (System.currentTimeMillis() - editTimestamps.getOrDefault(event.userId, 0L) >= 3000L) {
				//println("not on cooldown")
				editTimestamps[event.userId] = System.currentTimeMillis()
				Flux.fromIterable(handlers).filter { it.emoji == event.emoji }.flatMap {
					//println("invoking for emoji: ${it.emoji.asEmojiData()}")
					it.onReactionRemove.invoke(event)
				}
			} else Flux.empty()
		} else Flux.empty()
	}
	
	fun registerReactionMessage(ctx: ReactionMessageContext, vararg handlers: ReactionHandler) {
		reactionMessages[ctx] = handlers.toList()
	}
	
	fun removeIf(predicate: (MutableMap.MutableEntry<ReactionMessageContext, List<ReactionHandler>>) -> Boolean) {
		//reactionMessages.entries.removeIf(predicate)
		//println("removeIf called")
		reactionMessages.entries.filter(predicate).forEach {
			//println("Removed=${reactionMessages.remove(it.key)}")
			reactionMessages.remove(it.key)
		}
	}
	
}