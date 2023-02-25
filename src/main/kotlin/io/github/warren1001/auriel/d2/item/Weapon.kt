package io.github.warren1001.auriel.d2.item

import dev.minn.jda.ktx.messages.Embed
import io.github.warren1001.d2data.D2Sheet
import io.github.warren1001.d2data.enums.D2ItemTypes
import io.github.warren1001.d2data.enums.D2Weapons

class Weapon(items: Items, names: Map<String, String>, rowIndex: Int, weapons: D2Sheet, itemTypes: D2Sheet):
	Item(items, names,
		qlvl = weapons.asInt(rowIndex, D2Weapons.LEVEL, 0),
		rlvl = weapons.asInt(rowIndex, D2Weapons.LEVEL_REQ, 0)
	) {
	
	private val strings = mutableMapOf<String, LangStrings>()
	
	val type = weapons[rowIndex, D2Weapons.TYPE]
	val rstr = weapons.asInt(rowIndex, D2Weapons.REQ_STR, 0)
	val rdex = weapons.asInt(rowIndex, D2Weapons.REQ_DEX, 0)
	val durability = weapons.asInt(rowIndex, D2Weapons.DURABILITY, 0)
	val hasDurability = weapons.asInt(rowIndex, D2Weapons.NO_DURABILITY, 0) == 1
	val min1h = weapons.asInt(rowIndex, D2Weapons.MIN_DAM, 0)
	val max1h = weapons.asInt(rowIndex, D2Weapons.MAX_DAM, 0)
	val min2h = weapons.asInt(rowIndex, D2Weapons.TWO_HAND_MIN_DAM, 0)
	val max2h = weapons.asInt(rowIndex, D2Weapons.TWO_HAND_MAX_DAM, 0)
	val minMisDam = weapons.asInt(rowIndex, D2Weapons.MIN_MIS_DAM, 0)
	val maxMisDam = weapons.asInt(rowIndex, D2Weapons.MAX_MIS_DAM, 0)
	val quantity = weapons.asInt(rowIndex, D2Weapons.MAX_STACK, 0)
	val range = weapons.asInt(rowIndex, D2Weapons.RANGE_ADDER, 0)
	val wsm = weapons.asInt(rowIndex, D2Weapons.SPEED, 0)
	val strBonus = weapons.asInt(rowIndex, D2Weapons.STR_BONUS, 0)
	val dexBonus = weapons.asInt(rowIndex, D2Weapons.DEX_BONUS, 0)
	val magiclvl = weapons.asInt(rowIndex, D2Weapons.MAGIC_LVL, 0)
	val normCode = weapons[rowIndex, D2Weapons.NORM_CODE]
	val excepCode = weapons[rowIndex, D2Weapons.UBER_CODE]
	val eliteCode = weapons[rowIndex, D2Weapons.ULTRA_CODE]
	val maxSockets = weapons.asInt(rowIndex, D2Weapons.GEM_SOCKETS, 0)
	val maxSockets25 = itemTypes.asInt(type, D2ItemTypes.MAX_SOCKETS_1, 0).coerceAtMost(maxSockets)
	val maxSockets40 = itemTypes.asInt(type, D2ItemTypes.MAX_SOCKETS_2, 0).coerceAtMost(maxSockets)
	
	init {
		if (type == "h2h" || type == "h2h2" || type == "orb" || type == "abow" || type == "aspe" || type == "ajav") {
			when (type) {
				"h2h", "h2h2" -> strings["classOnly"] = items.TEMPLATE_ASSASSIN_ONLY.format()
				"orb" -> strings["classOnly"] = items.TEMPLATE_SORCERESS_ONLY.format()
				"abow", "aspe", "ajav" -> strings["classOnly"] = items.TEMPLATE_AMAZON_ONLY.format()
			}
		}
		if (min1h > 0) strings["1hDmg"] = items.TEMPLATE_1H_DAMAGE.format(min1h, max1h, (min1h + max1h) / 2)
		if (min2h > 0) strings["2hDmg"] = items.TEMPLATE_2H_DAMAGE.format(min2h, max2h, (min2h + max2h) / 2)
		if (minMisDam > 0) strings["throwDmg"] = items.TEMPLATE_THROW_DAMAGE.format(minMisDam, maxMisDam, (minMisDam + maxMisDam) / 2)
		if (rstr > 0) strings["rstr"] = items.TEMPLATE_REQUIRED_STRENGTH.format(rstr)
		if (rdex > 0) strings["rdex"] = items.TEMPLATE_REQUIRED_DEXTERITY.format(rdex)
		if (rlvl > 1) strings["rlvl"] = items.TEMPLATE_REQUIRED_LEVEL.format(rlvl)
		if (hasDurability) strings["durability"] = items.TEMPLATE_DURABILITY.format(durability)
		if (quantity > 0) strings["quantity"] = items.TEMPLATE_QUANTITY.format(quantity)
		strings["range"] = items.TEMPLATE_RANGE.format(range)
		strings["wsm"] = items.TEMPLATE_WEAPON_SPEED_MODIFIER.format(wsm)
		if (strBonus > 0) strings["strBonus"] = items.TEMPLATE_STRENGTH_BONUS.format(strBonus)
		if (dexBonus > 0) strings["dexBonus"] = items.TEMPLATE_DEXTERITY_BONUS.format(dexBonus)
		if (maxSockets > 0) strings["sockets"] = items.TEMPLATE_SOCKETS.format("$maxSockets25/$maxSockets40/$maxSockets")
		strings["qlvl"] = items.TEMPLATE_QUALITY_LEVEL.format(qlvl)
		if (magiclvl > 0) strings["magiclvl"] = items.TEMPLATE_MAGIC_LEVEL.format(magiclvl)
	}
	
	override fun createEmbed(lang: String) = Embed {
		title = names[lang]
		val builder = StringBuilder()
		if (type == "h2h" || type == "h2h2" || type == "orb" || type == "abow" || type == "aspe" || type == "ajav")
			builder.appendLine("**${getString(KEY_CLASS_ONLY, lang)}**")
		if (min1h > 0) builder.appendLine(getString(KEY_1H_DAMAGE, lang))
		if (min2h > 0) builder.appendLine(getString(KEY_2H_DAMAGE, lang))
		if (minMisDam > 0) builder.appendLine(getString(KEY_THROW_DAMAGE, lang))
		if (rstr > 0) builder.appendLine(getString(KEY_REQUIRED_STRENGTH, lang))
		if (rdex > 0) builder.appendLine(getString(KEY_REQUIRED_DEXTERITY, lang))
		if (rlvl > 1) builder.appendLine(getString(KEY_REQUIRED_LEVEL, lang))
		if (hasDurability) builder.appendLine(getString(KEY_DURABILITY, lang))
		if (quantity > 0) builder.appendLine(getString(KEY_QUANTITY, lang))
		builder.appendLine(getString(KEY_RANGE, lang))
		builder.appendLine(getString(KEY_WEAPON_SPEED_MODIFIER, lang))
		if (strBonus > 0) builder.appendLine(getString(KEY_STRENGTH_BONUS, lang))
		if (dexBonus > 0) builder.appendLine(getString(KEY_DEXTERITY_BONUS, lang))
		if (maxSockets > 0) builder.appendLine(getString(KEY_SOCKETS, lang))
		builder.appendLine(getString(KEY_QUALITY_LEVEL, lang))
		if (magiclvl > 0) builder.appendLine(getString(KEY_MAGIC_LEVEL, lang))
		description = builder.toString()
	}
	
	override fun getString(key: String, lang: String) = strings[key]?.get(lang)
	
	companion object {
		const val KEY_CLASS_ONLY = "classOnly"
		const val KEY_1H_DAMAGE = "1hDmg"
		const val KEY_2H_DAMAGE = "2hDmg"
		const val KEY_THROW_DAMAGE = "throwDmg"
		const val KEY_REQUIRED_STRENGTH = "rstr"
		const val KEY_REQUIRED_DEXTERITY = "rdex"
		const val KEY_REQUIRED_LEVEL = "rlvl"
		const val KEY_DURABILITY = "durability"
		const val KEY_QUANTITY = "quantity"
		const val KEY_RANGE = "range"
		const val KEY_WEAPON_SPEED_MODIFIER = "wsm"
		const val KEY_STRENGTH_BONUS = "strBonus"
		const val KEY_DEXTERITY_BONUS = "dexBonus"
		const val KEY_SOCKETS = "sockets"
		const val KEY_QUALITY_LEVEL = "qlvl"
		const val KEY_MAGIC_LEVEL = "magiclvl"
	}
	
}