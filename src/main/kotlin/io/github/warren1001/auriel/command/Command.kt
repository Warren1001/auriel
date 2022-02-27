package io.github.warren1001.auriel.command

import io.github.warren1001.auriel.Auriel
import reactor.core.publisher.Mono

class Command(private val auriel: Auriel, private val name: String, val permission: Int, val action: (CommandContext) -> Mono<out Any>)