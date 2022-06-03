package io.github.warren1001.auriel.listener

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.NOTHING
import reactor.core.publisher.Mono

class PrivateMessageCreateHandler(private val auriel: Auriel) {
	
	fun handle(event: MessageCreateEvent): Mono<out Any> {
		
		// if author is empty, the message was a clientside bot message
		if (!event.message.author.isPresent) return NOTHING
		
		val author = event.message.author.get()
		
		if (author.id == auriel.gateway.selfId) return NOTHING
		
		val privateData = auriel.userManager.getUserPrivateData(author.id)
		
		if (privateData != null) {
			auriel.userManager.clearUserPrivateData(author.id)
			if (privateData.startsWith("clone-")) {
				val guildId = Snowflake.of(privateData.substring(6))
				val guildManager = auriel.getGuildManager(guildId)
				val joined = guildManager.cloneQueue!!.addUserToQueue(author.id, event.message.content)
				return guildManager.cloneQueue!!.sendHelpeeReply(joined, event, author)
			}
		}
		
		return NOTHING
	}
	
}