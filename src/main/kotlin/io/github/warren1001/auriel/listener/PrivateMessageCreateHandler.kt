package io.github.warren1001.auriel.listener

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.MessageEditSpec
import io.github.warren1001.auriel.*
import reactor.core.publisher.Mono

class PrivateMessageCreateHandler(private val auriel: Auriel) {
	
	//private val commandManager = auriel.commandManager
	
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
				return if (guildManager.cloneQueue!!.addUserToQueue(author.id, event.message.content)) {
					event.message.reply(
						"Added you to the Diablo Clone queue! I will respond with who your helper is once you've been assigned a helper.\n" +
								"Position in queue: ${guildManager.cloneQueue!!.getPosition(author.id)}\n" +
								"(Roughly) Estimated remaining time: ${guildManager.cloneQueue!!.getTimeRemainingMessage(author.id)}\n" +
								"You can (un)react with ✅ to update your position and the remaining time. Cooldown of 3 seconds."
					).doOnSuccess {
						val emoji = ReactionEmoji.unicode("✅")
						it.addReaction(emoji).subscribe()
						val ctx = ReactionMessageContext(it.id)
						ctx.userId = author.id
						ctx.guildId = guildId
						ctx.addCtx = "clone-${guildId.asString()}-request"
						auriel.reactionMessageHandler.registerReactionMessage(ctx, ReactionHandler(emoji,
							{ event ->
								event.message.flatMap {
									it.edit(
										MessageEditSpec.builder().contentOrNull(
											"Added you to the Diablo Clone queue! I will respond with who your helper is once you've been assigned a helper.\n" +
													"Position in queue: ${guildManager.cloneQueue!!.getPosition(author.id)}\n" +
													"(Roughly) Estimated remaining time: ${guildManager.cloneQueue!!.getTimeRemainingMessage(author.id)}\n" +
													"You can (un)react with ✅ to update your position and the remaining time. Cooldown of 3 seconds."
										).build()
									)
								}
							}, { event ->
								event.message.flatMap {
									it.edit(
										MessageEditSpec.builder().contentOrNull(
											"Added you to the Diablo Clone queue! I will respond with who your helper is once you've been assigned a helper.\n" +
													"Position in queue: ${guildManager.cloneQueue!!.getPosition(author.id)}\n" +
													"(Roughly) Estimated remaining time: ${guildManager.cloneQueue!!.getTimeRemainingMessage(author.id)}\n" +
													"You can (un)react with ✅ to update your position and the remaining time. Cooldown of 3 seconds."
										).build()
									)
								}
							})
						)
					}
				} else {
					event.message.reply(
						"You are already in the queue! If you are trying to rejoin, go back to the channel with the reaction message and (un)react twice to rejoin the queue."
					)
				}
			}
		}
		
		//}
		return NOTHING
	}
	
}