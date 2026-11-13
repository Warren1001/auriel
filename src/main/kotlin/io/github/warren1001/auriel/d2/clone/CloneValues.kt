package io.github.warren1001.auriel.d2.clone

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel

open class CloneValues(
	val member: Member,
	val thread: ThreadChannel,
	var request: CloneRequest? = null,
	var deleted: Boolean = false,
) {
	fun hasRequest() = request != null
}