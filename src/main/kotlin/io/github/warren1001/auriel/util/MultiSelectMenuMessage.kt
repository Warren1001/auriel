package io.github.warren1001.auriel.util

import dev.minn.jda.ktx.interactions.components.SelectMenu
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.editMessage_
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.utils.messages.MessageCreateData

class MultiSelectMenuMessage<T>(private val id: String, private val data: MutableMap<T, List<String>>, private val values: List<T>,
                                private val format: String, private val finishMsg: String, private val onlyOne: Boolean = true, private val mustChoose: Boolean = true,
                                private val filter: (T, Int) -> Boolean = { _: T, _: Int -> true }, private val optionConverter: (T) -> List<SelectOption>,
                                private val display: (T) -> String, private val finished: (MutableMap<T, List<String>>) -> Unit) {
	
	private var index = 0
	
	fun handleSelectMenu(event: SelectMenuInteractionEvent): Boolean {
		val chosen = event.selectedOptions.map { it.value }
		data[values[index]] = if (chosen.any { it == "none" }) emptyList() else chosen
		index++
		if (index < values.size) {
			val options = values.filter { filter.invoke(it, index) }.map { optionConverter.invoke(it) }.flatten().toMutableList()
			if (!mustChoose) options.add(0, SelectOption("None", "none"))
			event.editMessage_(format.format(display.invoke(values[index])), components = listOf(ActionRow.of(
				SelectMenu("$id-$index") {
					setRequiredRange(1, if (onlyOne) 1 else options.size)
					addOptions(options)
				}
			))).queue()
			return false
		} else {
			event.editMessage_(finishMsg, components = emptyList()).queue()
			finished.invoke(data)
			return true
		}
	}
	
	fun createInitialReply(): MessageCreateData {
		val options = values.filter { filter.invoke(it, index) }.map { optionConverter.invoke(it) }.flatten().toMutableList()
		if (!mustChoose) options.add(SelectOption("None", "none"))
		return MessageCreate(format.format(display.invoke(values[index])), components = listOf(ActionRow.of(
			SelectMenu("$id-$index") {
				setRequiredRange(1, if (onlyOne) 1 else options.size)
				addOptions(options)
			}
		)))
	}

}