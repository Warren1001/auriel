package io.github.warren1001.auriel.listener

import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.NOTHING
import io.github.warren1001.auriel.async
import io.github.warren1001.auriel.handleErrors
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GuildMessageCreateHandler(private val auriel: Auriel) {
	
	private val commandManager = auriel.commandManager
	
	fun handle(event: MessageCreateEvent): Publisher<out Any> {
		
		// if author is empty, the message was a clientside bot message
		if (!event.message.author.isPresent || event.message.author.orElseThrow().id == auriel.gateway.selfId) return NOTHING
		
		val monos = mutableListOf<Mono<out Any>>()
		
		if (commandManager.isCommand(event)) monos.add(commandManager.handle(event).handleErrors(auriel, "GuildMessageCreateHandler,commandHandle").async())
		monos.add(auriel.getGuildManager(event.guildId.orElseThrow()).handleMessageCreate(event).async())
		
		return if (monos.isEmpty()) NOTHING else Flux.merge(monos)
	}
	
}