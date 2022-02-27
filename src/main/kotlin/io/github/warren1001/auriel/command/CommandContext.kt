package io.github.warren1001.auriel.command

import discord4j.core.event.domain.message.MessageCreateEvent

class CommandContext(val event: MessageCreateEvent, val arguments: String)