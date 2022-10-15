package io.github.warren1001.auriel.eventhandler

import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class ModalInteractionHandler(private val auriel: Auriel) {
	
	fun onModalInteraction(event: ModalInteractionEvent) {
		if (!event.isFromGuild) return
		val id = event.modalId
		val guild = auriel.guilds.getGuild(event.guild!!.id)
		if (id == "${guild.id}-request") {
			val gameName = event.getValue("game-name")!!.asString
			val password = event.getValue("game-password")!!.asString
			val otherInfo = event.getValue("game-other")?.asString
			guild.cloneHandler.submitModal(event.user.id, gameName, password, otherInfo)
			event.reply_("Your request has been submitted. Please be patient as there may be a lot of people waiting for only a few helpers. This message will disappear on its own eventually.")
				.queue_()
		} else if (id == "${guild.id}-help") {
			guild.cloneHandler.completedHelp(event)
		}
		
	}
	
}