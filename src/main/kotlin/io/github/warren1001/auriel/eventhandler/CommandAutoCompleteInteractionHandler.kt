package io.github.warren1001.auriel.eventhandler

import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent

class CommandAutoCompleteInteractionHandler {
	
	private val match = Regex("(^)")
	private val autocompletes = mutableMapOf<String, AutoCompleteData>()
	
	fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
		if (!event.isFromGuild) return
		val key = event.name + (if (event.subcommandName != null) ":" + event.subcommandName else "") + ":" + event.focusedOption.name
		if (autocompletes.contains(key)) {
			val list = autocompletes[key]!!.filter.invoke(event.member!!)
			event.replyChoiceStrings(list.filter { it.startsWith(event.focusedOption.value, ignoreCase = true) || it.contains(" ${event.focusedOption.value}", ignoreCase = true) }
				.sorted().take(25)).queue_()
		}
	}
	
	fun addAutocompleteStrings(command: String, option: String, value: String, vararg values: String) {
		val list = mutableListOf(value)
		list.addAll(values)
		autocompletes["$command:$option"] = AutoCompleteData(list)
	}
	
	fun addAutocompleteStrings(command: String, option: String, values: Collection<String>) {
		val list = values.toMutableList()
		autocompletes["$command:$option"] = AutoCompleteData(list)
	}
	
	fun addAutocompleteStrings(command: String, subCommand: String, option: String, value: String, vararg values: String) {
		val list = mutableListOf(value)
		list.addAll(values)
		autocompletes["$command:$subCommand:$option"] = AutoCompleteData(list)
	}
	
	fun addAutocompleteStrings(command: String, subCommand: String, option: String, values: Collection<String>) {
		val list = values.toMutableList()
		autocompletes["$command:$subCommand:$option"] = AutoCompleteData(list)
	}
	
	fun addAutocompleteStrings(command: String, option: String, valueFunction: (Member) -> List<String>) {
		autocompletes["$command:$option"] = AutoCompleteData(mutableListOf(), valueFunction)
	}
	
}