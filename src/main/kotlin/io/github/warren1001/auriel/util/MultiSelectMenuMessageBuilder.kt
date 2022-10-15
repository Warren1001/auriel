package io.github.warren1001.auriel.util

import net.dv8tion.jda.api.interactions.components.selections.SelectOption

class MultiSelectMenuMessageBuilder<T> {
	
	lateinit var userId: String
	val data: MutableMap<T, List<String>> = mutableMapOf()
	lateinit var values: List<T>
	lateinit var format: String
	lateinit var finishMsg: String
	var onlyOne: Boolean = true
	var mustChoose: Boolean = true
	var filter: (T, Int) -> Boolean = { _: T, _: Int -> true }
	lateinit var optionConverter: (T) -> List<SelectOption>
	lateinit var display: (T) -> String
	lateinit var finished: (MutableMap<T, List<String>>) -> Unit
	
	fun build() = MultiSelectMenuMessage(userId, data, values, format, finishMsg, onlyOne, mustChoose, filter, optionConverter, display, finished)

	
}