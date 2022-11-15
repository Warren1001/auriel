package io.github.warren1001.auriel.eventhandler

import net.dv8tion.jda.api.entities.Member

class AutoCompleteData(val list: MutableList<String>, val filter: (Member) -> List<String> = { list }) {
	
	fun add(value: String, vararg values: String) {
		list.add(value)
		list.addAll(values)
	}
	
	fun add(values: Collection<String>) {
		list.addAll(values)
	}
	
}
