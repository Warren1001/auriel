package io.github.warren1001.auriel.eventhandler

import io.github.warren1001.auriel.a
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class ModalInteractionHandler {
	
	fun onModalInteraction(event: ModalInteractionEvent) {
		if (!event.isFromGuild) return
		//val modalId = event.modalId
		val guild = event.guild!!.a()
		val cloneHandler = guild.cloneHandler
		if (cloneHandler != null && cloneHandler.onModalInteraction(event)) return
		//print("modal submitted")
	}
	
}