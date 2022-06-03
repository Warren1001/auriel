package io.github.warren1001.auriel.listener

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.component.TextInput
import discord4j.core.spec.MessageEditSpec
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import java.time.Duration

class ModalSubmitInteractionHandler {
	
	fun handle(event: ModalSubmitInteractionEvent): Publisher<out Any> {
		println(event.getComponents(TextInput::class.java)[0].value.orElseThrow())
		return event.deferEdit().withEphemeral(true).then(event.createFollowup("test 1").withEphemeral(true).flatMap {
			Mono.delay(Duration.ofSeconds(2)).then(it.edit(MessageEditSpec.builder().contentOrNull("test 2").build()))
		})
		/*return event.deferEdit().withEphemeral(true)
			.then(Mono.delay(Duration.ofSeconds(2)))
			.then(event.createFollowup("test 1").withEphemeral(true).flatMap { it. })
			.then(Mono.delay(Duration.ofSeconds(2)))
			.then(event.createFollowup("test 2").withEphemeral(true))*/
	}
	
}