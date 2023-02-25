package io.github.warren1001.auriel.d2.item

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.warren1001.auriel.d2.D2
import io.github.warren1001.d2data.enums.*

class Items {
	
	val manager = D2.files
	val itemNames: Map<String, Map<String, String>> = parseJsonToMap(loadJson("item-names.json"))
	val itemModifiers: Map<String, Map<String, String>> = parseJsonToMap(loadJson("item-modifiers.json"))
	val skills: Map<String, Map<String, String>> = parseJsonToMap(loadJson("skills.json"))
	val ui: Map<String, Map<String, String>> = parseJsonToMap(loadJson("ui.json"))
	val monsters: Map<String, Map<String, String>> = parseJsonToMap(loadJson("monsters.json"))
	
	/**
	 * In English: Sockets: %s
	 */
	val TEMPLATE_SOCKETS = TemplateStrings("ModStre8c", itemModifiers["ModStre8c"]!!.mapValues { "${it.value.trim()}: %s" })
	/**
	 * In English: Range: %s
	 */
	val TEMPLATE_RANGE = TemplateStrings("StrSkill56", skills["StrSkill56"]!!.mapValues { it.value.trun("%s") })
	/**
	 * Strictly the word "Bonus" to use with "Strength Bonus" and "Dexterity Bonus"
	 */
	val TEMPLATE_BONUS = TemplateStrings("TooltipResistanceBonus", ui["TooltipResistanceBonus"]!!.mapValues { it.value.trun(Regex(":?[： ]%"))})
	/**
	 * In English: Strength Bonus: +%s%%
	 *
	 * 'Bonus' is taken from [TEMPLATE_BONUS]
	 */
	val TEMPLATE_STRENGTH_BONUS = TemplateStrings("strchrstr", ui["strchrstr"]!!.mapValues { "${it.value} ${TEMPLATE_BONUS.get(it.key)}" })
	/**
	 * In English: Dexterity Bonus: +%s%%
	 *
	 * 'Bonus' is taken from [TEMPLATE_BONUS]
	 */
	val TEMPLATE_DEXTERITY_BONUS = TemplateStrings("strchrdex", ui["strchrdex"]!!.mapValues { "${it.value} ${TEMPLATE_BONUS.get(it.key)}" })
	/**
	 * In English: One-Hand Damage: %s-%s (%s)
	 */
	val TEMPLATE_1H_DAMAGE = TemplateStrings("ItemStats1l", itemModifiers["ItemStats1l"]!!.mapValues { it.value.trun("%s") + "-%s (%s)" })
	/**
	 * In English: Two-Hand Damage: %s-%s (%s)
	 */
	val TEMPLATE_2H_DAMAGE = TemplateStrings("ItemStats1m", itemModifiers["ItemStats1m"]!!.mapValues { it.value.trun("%s") + "-%s (%s)" })
	/**
	 * In English: Throw Damage: %s-%s (%s)
	 */
	val TEMPLATE_THROW_DAMAGE = TemplateStrings("strItemStatThrowDamageRange", itemModifiers["strItemStatThrowDamageRange"]!!.mapValues { it.value.trun("%s") + "-%s (%s)" })
	/**
	 * In English: Quantity: %s
	 */
	val TEMPLATE_QUANTITY = TemplateStrings("ItemStats1i", itemModifiers["ItemStats1i"]!!.mapValues { it.value.trun("%s") })
	/**
	 * In English: Defense: %s-%s (%s)
	 */
	val TEMPLATE_DEFENSE = TemplateStrings("ItemStats1h", itemModifiers["ItemStats1h"]!!.mapValues { "${it.value}-%s (%s)" })
	/**
	 * In English: Required Level: %s
	 */
	val TEMPLATE_REQUIRED_LEVEL = TemplateStrings("ItemStats1p", itemModifiers["ItemStats1p"]!!)
	/**
	 * In English: Required Strength: %s
	 */
	val TEMPLATE_REQUIRED_STRENGTH = TemplateStrings("ItemStats1e", itemModifiers["ItemStats1e"]!!)
	/**
	 * In English: Required Dexterity: %s
	 */
	val TEMPLATE_REQUIRED_DEXTERITY = TemplateStrings("ItemStats1f", itemModifiers["ItemStats1f"]!!)
	/**
	 * In English: Durability: %s
	 */
	val TEMPLATE_DURABILITY = TemplateStrings("ItemStats1d", itemModifiers["ItemStats1d"]!!.mapValues { it.value.trun("%s") })
	/**
	 * In English: Smite Damage: %s-%s (%s)
	 */
	val TEMPLATE_SMITE_DAMAGE = TemplateStrings("ItemStats1o", itemModifiers["ItemStats1o"]!!.mapValues { it.value.trun("%s") + "-%s (%s)" })
	/**
	 * In English: Kick Damage: %s-%s (%s)
	 */
	val TEMPLATE_KICK_DAMAGE = TemplateStrings("ModStre10k", itemModifiers["ModStre10k"]!!.mapValues { it.value.trun("%s") + "-%s (%s)" })
	/**
	 * In English: Run/Walk: %s
	 */
	val TEMPLATE_MOVEMENT_SPEED = TemplateStrings("StrHelp14", ui["StrHelp14"]!!.mapValues { "${it.value}: %s" })
	/**
	 * In English: Belt Size: %s
	 */
	val TEMPLATE_BELT_SIZE = TemplateStrings("BeltStorageModifierInfo", ui["BeltStorageModifierInfo"]!!.mapValues { it.value.trun(Regex("[：:]")) + ": %s" })
	/**
	 * In English: (Amazon Only)
	 */
	val TEMPLATE_AMAZON_ONLY = TemplateStrings("AmaOnly", itemModifiers["AmaOnly"]!!)
	/**
	 * In English: (Sorceress Only)
	 */
	val TEMPLATE_SORCERESS_ONLY = TemplateStrings("SorOnly", itemModifiers["SorOnly"]!!)
	/**
	 * In English: (Necromancer Only)
	 */
	val TEMPLATE_NECROMANCER_ONLY = TemplateStrings("NecOnly", itemModifiers["NecOnly"]!!)
	/**
	 * In English: (Paladin Only)
	 */
	val TEMPLATE_PALADIN_ONLY = TemplateStrings("PalOnly", itemModifiers["PalOnly"]!!)
	/**
	 * In English: (Barbarian Only)
	 */
	val TEMPLATE_BARBARIAN_ONLY = TemplateStrings("BarOnly", itemModifiers["BarOnly"]!!)
	/**
	 * In English: (Druid Only)
	 */
	val TEMPLATE_DRUID_ONLY = TemplateStrings("DruOnly", itemModifiers["DruOnly"]!!)
	/**
	 * In English: (Assassin Only)
	 */
	val TEMPLATE_ASSASSIN_ONLY = TemplateStrings("AssOnly", itemModifiers["AssOnly"]!!)
	/**
	 * In all languages: Block: %s
	 *
	 * No available translations for this from the files
	 */
	val TEMPLATE_BLOCK = SimpleTemplateStrings("block", "Block: %s")
	/**
	 * In all languages: qlvl: %s
	 *
	 * No available translations for this from the files
	 */
	val TEMPLATE_QUALITY_LEVEL = SimpleTemplateStrings("qlvl", "qlvl: %s")
	/**
	 * In all languages: magiclvl: %s
	 *
	 * No available translations for this from the files
	 */
	val TEMPLATE_MAGIC_LEVEL = SimpleTemplateStrings("magiclvl", "magiclvl: %s")
	/**
	 * In all languages: WSM: %s
	 *
	 * No available translations for this from the files
	 */
	val TEMPLATE_WEAPON_SPEED_MODIFIER = SimpleTemplateStrings("wsm", "WSM: %s")
	
	private val allItemNamesByLang = mutableMapOf<String, MutableList<String>>()
	private val allUniqueItemNamesByLang = mutableMapOf<String, MutableList<String>>()
	val baseItems = mutableMapOf<String, Item>()
	val uniqueItems = mutableMapOf<String, Unique>()
	//val stats = Stats(manager.CHAR_STATS, manager.ITEM_STAT_COST, this)
	val properties = Properties(this)
	//val replacementRegex = Regex("%[d0i]")
	
	init {
		val weapons = manager.loadSheet(D2Weapons.SHEET_NAME)
		val itemTypes = manager.loadSheet(D2ItemTypes.SHEET_NAME)
		weapons.forEach {
			if (weapons.asInt(it, D2Weapons.SPAWNABLE, 0) == 0) return@forEach
			val key = weapons[it, D2Weapons.NAME_STR]
			val code = weapons[it, D2Weapons.CODE]
			val names = itemNames[key]!!
			val weapon = Weapon(this, names, it, weapons, itemTypes)
			names.forEach { (lang, name) ->
				allItemNamesByLang.computeIfAbsent(lang) { mutableListOf() }.add(name)
				baseItems[name.lowercase()] = weapon
			}
			baseItems[code.lowercase()] = weapon
		}
		val armors = manager.loadSheet(D2Armor.SHEET_NAME)
		val belts = manager.loadSheet(D2Belts.SHEET_NAME)
		armors.forEach {
			if (armors.asInt(it, D2Armor.SPAWNABLE, 0) == 0) return@forEach
			val key = armors[it, D2Armor.NAME_STR]
			val code = armors[it, D2Armor.CODE]
			val names = itemNames[key]!!
			val armor = Armor(this, names, it, armors, itemTypes, belts)
			names.forEach { (lang, name) ->
				allItemNamesByLang.computeIfAbsent(lang) { mutableListOf() }.add(name)
				baseItems[name.lowercase()] = armor
			}
			baseItems[code.lowercase()] = armor
		}
		val uniqueItems = manager.loadSheet(D2UniqueItems.SHEET_NAME)
		uniqueItems.forEach {
			if (uniqueItems.asInt(it, D2UniqueItems.ENABLED, 0) == 0) return@forEach
			val index = uniqueItems[it, D2UniqueItems.INDEX]
			val code = uniqueItems[it, D2UniqueItems.CODE]
			val names = itemNames[index]!!
			//println("Loading unique item $code")
			val baseItem = baseItems[code]
			val unique = Unique(this, names, it, uniqueItems, baseItem)
			names.forEach { (lang, name) ->
				allItemNamesByLang.computeIfAbsent(lang) { mutableListOf() }.add(name)
				allUniqueItemNamesByLang.computeIfAbsent(lang) { mutableListOf() }.add(name)
				this.uniqueItems[name.lowercase()] = unique
			}
		}
	}
	
	fun getAllItems(lang: String) = allItemNamesByLang[lang]!!
	
	fun getAllUniqueItems(lang: String) = allUniqueItemNamesByLang[lang]!!
	
	fun getBaseItem(name: String) = baseItems[name.lowercase()]
	
	fun getUniqueItem(name: String) = uniqueItems[name.lowercase()]
	
	private fun loadJson(name: String) = this::class.java.classLoader.getResource(name)!!.readText(Charsets.UTF_8).replace("\uFEFF", "")
		.let { ObjectMapper().readTree(it) }
	
	private fun parseJsonToMap(node: JsonNode) = node.associate { it["Key"].asText() to it.fields().asSequence()
		.filter { it.key != "id" && it.key != "Key" }.map {
			it.key to it.value.asText().replace(Regex("\\(?%\\+[id]\\)?"), "+%s").replace(Regex("%\\+?[id]"), "%s")
				.replace("%3", "%4\$s").replace("%2", "%3\$s").replace("%1", "%2\$s").replace("%0", "%1\$s")
				.replace("%+3", "+%4\$s").replace("%+2", "+%3\$s").replace("%+1", "+%2\$s").replace("%+0", "+%1\$s")
		}.toMap() }
	
}

fun String.trun(value: String, inclusive: Boolean = true): String {
	val index = indexOf(value)
	return if (index == -1) this else substring(0, index + if (inclusive) value.length else 0)
}

fun String.trun(regex: Regex): String {
	val match = regex.find(this)
	return if (match == null) this else substring(0, match.range.first)
}

fun String.format(vararg args: Any) = if (isBlank()) this else String.format(this, *args)

fun Any.isInt() = try {
	this as Int
	true
} catch (e: ClassCastException) {
	this.toString().toIntOrNull() != null
}

fun main(args: Array<String>) {
	val items = Items()
	items.properties.getUniquePropertiesString("Tomb Reaver")
}