package io.github.warren1001.auriel.d2.clone

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel

class HelpeeCloneValues(
	member: Member,
	val data: CloneDataHelpee,
	thread: ThreadChannel,
	request: CloneRequest? = null,
	deleted: Boolean = false,
	var allowRequeue: Boolean = false,
) : CloneValues(member, thread, request, deleted)