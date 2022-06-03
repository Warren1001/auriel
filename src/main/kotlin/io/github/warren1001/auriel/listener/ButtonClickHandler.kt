package io.github.warren1001.auriel.listener

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.TextInput
import discord4j.core.spec.InteractionPresentModalSpec
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.NOTHING
import org.reactivestreams.Publisher

class ButtonClickHandler(private val auriel: Auriel) {
	
	fun handle(event: ButtonInteractionEvent): Publisher<out Any> {
		if (event.customId == "modal-test") {
			return event.presentModal(
				InteractionPresentModalSpec.builder().customId("modal-test").title("test").components(
					ActionRow.of(
						TextInput.paragraph("modal-test-p", "test", 3, 1000).placeholder("MyGameName//MyPassWord did not spawn him yet").required()
					)//, ActionRow.of(Button.primary("modal-test-2", "test 2"))
				).build()
			)
		}
		if (event.customId == "modal-test-2") {
			return event.presentModal(
				InteractionPresentModalSpec.builder().customId("modal-test-2").title("test 2").components(
					ActionRow.of(
						TextInput.paragraph("modal-test-p-2", "test 2", 3, 1000).placeholder("MyGameName//MyPassWord did not spawn him yet").required()
					)//, ActionRow.of(Button.primary("modal-test-2", "test 2"))
				).build()
			)
		}
		val args = event.customId.split("-")
		val member = event.interaction.member.orElseThrow()
		return if (args[0] == "r") {
			val roleId = Snowflake.of(args[1])
			if (args[2] == "g") {
				if (member.roleIds.contains(roleId)) {
					event.reply("You already have the role.").withEphemeral(true)
				} else {
					member.addRole(roleId).then(event.reply("You now have the role.").withEphemeral(true))
				}
			} else if (args[2] == "r") {
				if (member.roleIds.contains(roleId)) {
					member.removeRole(roleId).then(event.reply("You no longer have the role.").withEphemeral(true))
				} else {
					event.reply("You do not have the role.").withEphemeral(true)
				}
			} else NOTHING
		} else NOTHING
	}
	
}