package io.github.warren1001.auriel.d2.clone

import io.github.warren1001.auriel.d2.Core
import io.github.warren1001.auriel.d2.LadderType
import io.github.warren1001.auriel.d2.Mode
import io.github.warren1001.auriel.d2.Platform
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

class CloneQueue(
	val platform: Platform,
	val mode: Mode,
	val core: Core,
	val type: LadderType,
	private val queue: ConcurrentLinkedDeque<HelpeeCloneValues> = ConcurrentLinkedDeque<HelpeeCloneValues>(),
	val helpers: MutableSet<String> = mutableSetOf(),
	val helpersWaiting: MutableSet<HelperCloneValues> = Collections.synchronizedSet(mutableSetOf<HelperCloneValues>()),
	var completed: Int = 0,
) {
	fun getDisplayString() = "${getDisplayName()}: $completed/${completed + queue.size}, ${helpers.size} helpers"
	fun getDisplayName() = "${platform.display} - ${mode.display} ${core.display} ${type.display}"
	
	fun addHelpeeToFront(helpee: HelpeeCloneValues) {
		queue.addFirst(helpee)
		val data = helpee.data
		if (data.queue == null) data.queue = this
		if (data.position == null) data.position = completed + 1
	}
	
	fun addHelpee(helpee: HelpeeCloneValues) {
		queue.add(helpee)
		val data = helpee.data
		data.queue = this
		data.position = getTotalSize()
	}
	
	fun removeHelpee(helpee: HelpeeCloneValues) {
		queue.remove(helpee)
		val data = helpee.data
		data.queue = null
		data.position = null
	}
	
	fun pollRandomWaitingHelper(): HelperCloneValues? {
		synchronized(helpersWaiting) {
			val helper = helpersWaiting.randomOrNull()
			helper?.let { helpersWaiting.remove(it) }
			return helper
		}
	}
	
	fun pollHelpee(): HelpeeCloneValues? = queue.poll()
	
	fun getTotalSize() = queue.size + completed
	
}