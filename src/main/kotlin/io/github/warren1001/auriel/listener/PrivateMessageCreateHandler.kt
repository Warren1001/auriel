package io.github.warren1001.auriel.listener

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.NOTHING
import reactor.core.publisher.Mono

class PrivateMessageCreateHandler(private val auriel: Auriel) {
	
	private val commandManager = auriel.commandManager
	
	fun handle(event: MessageCreateEvent): Mono<out Any> {
		
		// if author is empty, the message was a clientside bot message
		if (!event.message.author.isPresent) {
			return NOTHING
		}
		
		val author = event.message.author.get()
		
		if (author.id == auriel.gateway.selfId) return NOTHING
		
		//val monos = mutableListOf<Mono<out Any>>()
		
		if (commandManager.isCommand(event)) return commandManager.handle(event)
		else {
			
			val privateData = auriel.userManager.getUserPrivateData(author.id)
			
			if (privateData != null) {
				if (privateData.startsWith("clone-")) {
					val guildId = Snowflake.of(privateData.substring(6))
					val guildManager = auriel.getGuildManager(guildId)
					/*monos.add(guildManager.userDataManager.getData(author.id).flatMap {
						if (guildManager.cloneQueue!!.addUserToQueue(it, event.message.content)) {
							event.message.reply("Added you to the clone queue! Remaining time: ${guildManager.cloneQueue!!.getTimeRemainingMessage(author.id)}")
						} else {
							event.message.reply(
								"You are already in the queue! You probably want to rejoin. Go back to the channel with the buttons, remove yourself from the queue, then add yourself again."
							)
						}
						
					})*/
					guildManager.cloneQueue!!.addUserToQueue(author.id, event.message.content)
				}
			}
			
		}
		
		
		
		return NOTHING
	}
	
}