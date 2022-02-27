package io.github.warren1001.auriel.listener

import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.NOTHING
import io.github.warren1001.auriel.async
import io.github.warren1001.auriel.handleErrors
import reactor.core.publisher.Mono

class MessageCreateHandler(private val auriel: Auriel) {
	
	private val commandManager = auriel.commandManager
	
	fun handle(event: MessageCreateEvent): Mono<out Any> {
		
		if (event.message.author.orElseThrow().id == auriel.gateway.selfId) return NOTHING
		
		val monos = mutableListOf<Mono<out Any>>()
		
		if (commandManager.isCommand(event)) monos.add(commandManager.handle(event).async())
		monos.add(auriel.getGuildManager(event.guildId.orElseThrow()).handleMessageCreate(event))
		
		return if (monos.isEmpty()) NOTHING else Mono.`when`(monos).handleErrors()
	}
	
}