package io.github.warren1001.auriel

import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent
import discord4j.core.`object`.reaction.ReactionEmoji
import org.reactivestreams.Publisher

data class ReactionHandler(val emoji: ReactionEmoji, val onReactionAdd: (ReactionAddEvent) -> Publisher<out Any>, val onReactionRemove: (ReactionRemoveEvent) -> Publisher<out Any> = { NOTHING })