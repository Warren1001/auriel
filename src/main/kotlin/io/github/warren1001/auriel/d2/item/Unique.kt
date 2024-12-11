package io.github.warren1001.auriel.d2.item

import dev.minn.jda.ktx.messages.Embed
import io.github.warren1001.d2data.D2Sheet
import io.github.warren1001.d2data.enums.sheet.D2UniqueItems

class Unique(items: Items, names: Map<String, String>, rowIndex: Int, uniqueItems: D2Sheet, private val baseItem: Item?):
	Item(
		items,
		names,
		uniqueItems.asInt(rowIndex, D2UniqueItems.LVL, 0),
		uniqueItems.asInt(rowIndex, D2UniqueItems.LVL_REQ, 1)
	) {
	
	private val langStrings = items.properties.getUniquePropertiesString(rowIndex)
	
	override fun createEmbed(lang: String) = Embed {
		title = names[lang]!!
		description = langStrings.get(lang)
	}
	
	override fun getString(key: String, lang: String): String? {
		TODO("Not yet implemented")
	}
	
	
}