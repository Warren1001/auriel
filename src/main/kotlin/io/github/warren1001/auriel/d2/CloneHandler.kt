package io.github.warren1001.auriel.d2

import dev.minn.jda.ktx.interactions.components.replyModal
import io.github.warren1001.auriel.guild.AGuild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class CloneHandler(private val guild: AGuild) {
	
	fun openRequestHelpModal(event: SlashCommandInteractionEvent) {
		event.replyModal("${guild.id}-request", "Request Diablo Clone Help (PC Only)") {
			short("game-name", "Game Name", true, placeholder = "MyClone1")
			short("game-password", "Game Password", true, placeholder = "1")
			paragraph("game-other", "Other Info", false, placeholder = "(optional) Any other information you want to provide")
		}.queue()
	}
	
}