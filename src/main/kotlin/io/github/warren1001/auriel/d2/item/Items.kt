package io.github.warren1001.auriel.d2.item

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.warren1001.d2data.D2Sheets
import io.github.warren1001.d2data.impl.D2Weapons
import java.io.File

class Items {
	
	val manager = D2Sheets(File("d2"))
	val itemNames: Map<String, Map<String, String>>
	private val allItemNamesByLang = mutableMapOf<String, MutableList<String>>()
	val baseItems = mutableMapOf<String, EquippableItem>()
	
	init {
		val itemNamesNode = this::class.java.classLoader.getResource("item-names.json")!!.readText(Charsets.UTF_8).replace("\uFEFF", "").let { ObjectMapper().readTree(it) }
		itemNames = itemNamesNode.associate {
			it["Key"].asText() to it.fields().asSequence().filter { it.key != "id" && it.key != "Key" }.map { it.key to it.value.asText() }.toMap()
		}
		val weapons = manager.WEAPONS
		val itemTypes = manager.ITEM_TYPES
		weapons.forEach {
			val key = weapons.getValue(it, D2Weapons.NAME_STR)
			val names = itemNames[key]!!
			val weapon = Weapon(names, it, weapons, itemTypes)
			names.forEach { (lang, name) ->
				allItemNamesByLang.computeIfAbsent(lang) { mutableListOf() }.add(name)
				baseItems[name] = weapon
			}
		}
	}
	
	fun getAllItems(lang: String) = allItemNamesByLang[lang]!!
	
	fun getBaseItem(name: String) = baseItems[name]
	
}