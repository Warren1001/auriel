package io.github.warren1001.auriel

import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import discord4j.core.`object`.reaction.ReactionEmoji
import reactor.core.publisher.Mono

data class ReactionHandler(val emoji: ReactionEmoji, val onReactionAdd: (ReactionAddEvent) -> Mono<out Any>, val onReactionRemove: (ReactionRemoveEvent) -> Mono<out Any> = { NOTHING })