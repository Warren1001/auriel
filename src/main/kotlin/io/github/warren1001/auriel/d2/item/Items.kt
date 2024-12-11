package io.github.warren1001.auriel.d2.item

import com.fasterxml.jackson.databind.JsonNode
import io.github.warren1001.auriel.d2.D2
import io.github.warren1001.d2data.enums.lang.*
import io.github.warren1001.d2data.enums.lang.D2Skills
import io.github.warren1001.d2data.enums.sheet.*

class Items {
	
	val itemNames = D2.files.loadLang(D2ItemNames.FILE_PATH)
	val itemModifiers = D2.files.loadLang(D2ItemModifiers.FILE_PATH)
	val skills = D2.files.loadLang(D2Skills.FILE_PATH)
	val ui = D2.files.loadLang(D2UI.FILE_PATH)
	val monsters = D2.files.loadLang(D2Monsters.FILE_PATH)
	
	/**
	 * In English: Sockets: %s
	 */
	val TEMPLATE_SOCKETS = TemplateStrings("ModStre8c", itemModifiers["ModStre8c"].map { _, value -> "${value.trim()}: %s" }.getStrings())
	/**
	 * In English: Range: %s
	 */
	val TEMPLATE_RANGE = TemplateStrings("StrSkill56", skills["StrSkill56"].map { _, value -> value.trun("%s") }.getStrings())
	/**
	 * Strictly the word "Bonus" to use with "Strength Bonus" and "Dexterity Bonus"
	 */
	val TEMPLATE_BONUS = TemplateStrings("TooltipResistanceBonus", ui["TooltipResistanceBonus"].map { _, value -> value.trun(Regex(":?[： ]%"))}.getStrings())
	/**
	 * In English: Strength Bonus: +%s%%
	 *
	 * 'Bonus' is taken from [TEMPLATE_BONUS]
	 */
	val TEMPLATE_STRENGTH_BONUS = TemplateStrings("strchrstr", ui["strchrstr"].map { key, value -> "$value ${TEMPLATE_BONUS.get(key)}" }.getStrings())
	/**
	 * In English: Dexterity Bonus: +%s%%
	 *
	 * 'Bonus' is taken from [TEMPLATE_BONUS]
	 */
	val TEMPLATE_DEXTERITY_BONUS = TemplateStrings("strchrdex", ui["strchrdex"].map { key, value -> "$value ${TEMPLATE_BONUS.get(key)}" }.getStrings())
	/**
	 * In English: One-Hand Damage: %s-%s (%s)
	 */
	val TEMPLATE_1H_DAMAGE = TemplateStrings("ItemStats1l", itemModifiers["ItemStats1l"].map { _, value -> value.trun("%s") + "-%s (%s)" }.getStrings())
	/**
	 * In English: Two-Hand Damage: %s-%s (%s)
	 */
	val TEMPLATE_2H_DAMAGE = TemplateStrings("ItemStats1m", itemModifiers["ItemStats1m"].map { _, value -> value.trun("%s") + "-%s (%s)" }.getStrings())
	/**
	 * In English: Throw Damage: %s-%s (%s)
	 */
	val TEMPLATE_THROW_DAMAGE = TemplateStrings("strItemStatThrowDamageRange", itemModifiers["strItemStatThrowDamageRange"].map { _, value -> value.trun("%s") + "-%s (%s)" }.getStrings())
	/**
	 * In English: Quantity: %s
	 */
	val TEMPLATE_QUANTITY = TemplateStrings("ItemStats1i", itemModifiers["ItemStats1i"].map { _, value -> value.trun("%s") }.getStrings())
	/**
	 * In English: Defense: %s-%s (%s)
	 */
	val TEMPLATE_DEFENSE = TemplateStrings("ItemStats1h", itemModifiers["ItemStats1h"].map { _, value -> "${value}-%s (%s)" }.getStrings())
	/**
	 * In English: Required Level: %s
	 */
	val TEMPLATE_REQUIRED_LEVEL = TemplateStrings("ItemStats1p", itemModifiers["ItemStats1p"].map { _, value -> value }.getStrings())
	/**
	 * In English: Required Strength: %s
	 */
	val TEMPLATE_REQUIRED_STRENGTH = TemplateStrings("ItemStats1e", itemModifiers["ItemStats1e"].map { _, value -> value }.getStrings())
	/**
	 * In English: Required Dexterity: %s
	 */
	val TEMPLATE_REQUIRED_DEXTERITY = TemplateStrings("ItemStats1f", itemModifiers["ItemStats1f"].map { _, value -> value }.getStrings())
	/**
	 * In English: Durability: %s
	 */
	val TEMPLATE_DURABILITY = TemplateStrings("ItemStats1d", itemModifiers["ItemStats1d"].map { _, value -> value.trun("%s") }.getStrings())
	/**
	 * In English: Smite Damage: %s-%s (%s)
	 */
	val TEMPLATE_SMITE_DAMAGE = TemplateStrings("ItemStats1o", itemModifiers["ItemStats1o"].map { _, value -> value.trun("%s") + "-%s (%s)" }.getStrings())
	/**
	 * In English: Kick Damage: %s-%s (%s)
	 */
	val TEMPLATE_KICK_DAMAGE = TemplateStrings("ModStre10k", itemModifiers["ModStre10k"].map { _, value -> value.trun("%s") + "-%s (%s)" }.getStrings())
	/**
	 * In English: Run/Walk: %s
	 */
	val TEMPLATE_MOVEMENT_SPEED = TemplateStrings("StrHelp14", ui["StrHelp14"].map { _, value -> "${value}: %s" }.getStrings())
	/**
	 * In English: Belt Size: %s
	 */
	val TEMPLATE_BELT_SIZE = TemplateStrings("BeltStorageModifierInfo", ui["BeltStorageModifierInfo"].map { _, value -> value.trun(Regex("[：:]")) + ": %s" }.getStrings())
	/**
	 * In English: (Amazon Only)
	 */
	val TEMPLATE_AMAZON_ONLY = TemplateStrings("AmaOnly", itemModifiers["AmaOnly"].getStrings())
	/**
	 * In English: (Sorceress Only)
	 */
	val TEMPLATE_SORCERESS_ONLY = TemplateStrings("SorOnly", itemModifiers["SorOnly"].getStrings())
	/**
	 * In English: (Necromancer Only)
	 */
	val TEMPLATE_NECROMANCER_ONLY = TemplateStrings("NecOnly", itemModifiers["NecOnly"].getStrings())
	/**
	 * In English: (Paladin Only)
	 */
	val TEMPLATE_PALADIN_ONLY = TemplateStrings("PalOnly", itemModifiers["PalOnly"].getStrings())
	/**
	 * In English: (Barbarian Only)
	 */
	val TEMPLATE_BARBARIAN_ONLY = TemplateStrings("BarOnly", itemModifiers["BarOnly"].getStrings())
	/**
	 * In English: (Druid Only)
	 */
	val TEMPLATE_DRUID_ONLY = TemplateStrings("DruOnly", itemModifiers["DruOnly"].getStrings())
	/**
	 * In English: (Assassin Only)
	 */
	val TEMPLATE_ASSASSIN_ONLY = TemplateStrings("AssOnly", itemModifiers["AssOnly"].getStrings())
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
	//val stats = Stats(D2.files.CHAR_STATS, D2.files.ITEM_STAT_COST, this)
	val properties = Properties(this)
	//val replacementRegex = Regex("%[d0i]")
	
	init {
		val weapons = D2.files.loadSheet(D2Weapons.FILE_PATH)
		val itemTypes = D2.files.loadSheet(D2ItemTypes.FILE_PATH)
		weapons.forEach {
			if (weapons.asInt(it, D2Weapons.SPAWNABLE, 0) == 0) return@forEach
			val key = weapons[it, D2Weapons.NAME_STR]
			val code = weapons[it, D2Weapons.CODE]
			val names = itemNames[key].getStrings()
			val weapon = Weapon(this, names, it, weapons, itemTypes)
			names.forEach { (lang, name) ->
				allItemNamesByLang.computeIfAbsent(lang) { mutableListOf() }.add(name)
				baseItems[name.lowercase()] = weapon
			}
			baseItems[code.lowercase()] = weapon
		}
		val armors = D2.files.loadSheet(D2Armor.FILE_PATH)
		val belts = D2.files.loadSheet(D2Belts.FILE_PATH)
		armors.forEach {
			if (armors.asInt(it, D2Armor.SPAWNABLE, 0) == 0) return@forEach
			val key = armors[it, D2Armor.NAME_STR]
			val code = armors[it, D2Armor.CODE]
			val names = itemNames[key].getStrings()
			val armor = Armor(this, names, it, armors, itemTypes, belts)
			names.forEach { (lang, name) ->
				allItemNamesByLang.computeIfAbsent(lang) { mutableListOf() }.add(name)
				baseItems[name.lowercase()] = armor
			}
			baseItems[code.lowercase()] = armor
		}
		val uniqueItems = D2.files.loadSheet(D2UniqueItems.FILE_PATH)
		uniqueItems.forEach {
			if (uniqueItems.asInt(it, D2UniqueItems.ENABLED, 0) == 0) return@forEach
			val index = uniqueItems[it, D2UniqueItems.INDEX]
			val code = uniqueItems[it, D2UniqueItems.CODE]
			val names = itemNames[index].getStrings()
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

fun <T> JsonNode.mapValues(map: (String, JsonNode) -> T): Map<String, T> = fields().asSequence().map { it.key to map(it.key, it.value) }.toMap()