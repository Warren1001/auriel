package io.github.warren1001.auriel.eventhandler

import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent

class CommandAutoCompleteInteractionHandler {
	
	private val autocompletes = mutableMapOf<String, MutableList<String>>()
	
	fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
		if (!event.isFromGuild) return
		val key = event.name + (if (event.subcommandName != null) ":" + event.subcommandName else "") + ":" + event.focusedOption.name
		if (autocompletes.contains(key)) {
			event.replyChoiceStrings(autocompletes[key]!!.filter { it.startsWith(event.focusedOption.value) }.sortedDescending().take(25)).queue_()
		}
	}
	
	fun addAutocompleteStrings(command: String, option: String, value: String, vararg values: String) {
		val list = autocompletes.computeIfAbsent("$command:$option") { mutableListOf() }
		list.add(value)
		list.addAll(values)
	}
	
	fun addAutocompleteStrings(command: String, subCommand: String, option: String, value: String, vararg values: String) {
		val list = autocompletes.computeIfAbsent("$command:$subCommand:$option") { mutableListOf() }
		list.add(value)
		list.addAll(values)
	}
	
}