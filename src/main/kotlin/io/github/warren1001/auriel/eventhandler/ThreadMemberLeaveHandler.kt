package io.github.warren1001.auriel.eventhandler

import io.github.warren1001.auriel.a
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.events.thread.member.ThreadMemberLeaveEvent

class ThreadMemberLeaveHandler {
	
	fun onThreadMemberLeave(event: ThreadMemberLeaveEvent) {
		val thread = event.thread
		val member = event.member
		if (thread.parentChannel.asGuildMessageChannel().a().privateThreadsById.containsKey(thread.id)) {
			thread.addThreadMember(member).queue_()
		}
	}
	
}