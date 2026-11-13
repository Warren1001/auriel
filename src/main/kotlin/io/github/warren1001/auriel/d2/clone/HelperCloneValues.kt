package io.github.warren1001.auriel.d2.clone

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel

class HelperCloneValues(
	member: Member,
	val data: CloneDataHelper,
	thread: ThreadChannel,
	request: CloneRequest? = null,
	deleted: Boolean = false,
) : CloneValues(member, thread, request, deleted)