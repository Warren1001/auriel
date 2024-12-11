package io.github.warren1001.auriel.d2.item

import dev.minn.jda.ktx.messages.Embed
import io.github.warren1001.d2data.D2Sheet
import io.github.warren1001.d2data.enums.sheet.D2Armor
import io.github.warren1001.d2data.enums.sheet.D2Belts
import io.github.warren1001.d2data.enums.sheet.D2ItemTypes
import io.github.warren1001.d2data.enums.sheet.D2Weapons
import io.github.warren1001.d2data.lang.LangString

class Armor(items: Items, names: Map<String, String>, rowIndex: Int, armor: D2Sheet, itemTypes: D2Sheet, belts: D2Sheet):
	Item(items, names,
		qlvl = armor.asInt(rowIndex, D2Weapons.LEVEL, 0),
		rlvl = armor.asInt(rowIndex, D2Weapons.LEVEL_REQ, 0)
	) {
	
	private val strings = mutableMapOf<String, LangString>()
	
	val type = armor[rowIndex, D2Armor.TYPE]
	val rstr = armor.asInt(rowIndex, D2Weapons.REQ_STR, 0)
	val durability = armor.asInt(rowIndex, D2Weapons.DURABILITY, 0)
	val minDefense = armor.asInt(rowIndex, D2Armor.MIN_DEFENSE, 0)
	val maxDefense = armor.asInt(rowIndex, D2Armor.MAX_DEFENSE, 0)
	val speedPenalty = -armor.asInt(rowIndex, D2Armor.SPEED, 0)
	val magiclvl = armor.asInt(rowIndex, D2Armor.MAGIC_LVL, 0)
	val block = armor.asInt(rowIndex, D2Armor.BLOCK, 0)
	val strBonus = armor.asInt(rowIndex, D2Armor.STR_BONUS, 0)
	val minDmg = armor.asInt(rowIndex, D2Armor.MIN_DAM, 0)
	val maxDmg = armor.asInt(rowIndex, D2Armor.MAX_DAM, 0)
	val beltRows = armor.asInt(rowIndex, D2Armor.BELT, 2).let { if (it == 2) 0 else belts[it, D2Belts.NUM_BOXES] }
	val normCode = armor[rowIndex, D2Armor.NORM_CODE]
	val excepCode = armor[rowIndex, D2Armor.UBER_CODE]
	val eliteCode = armor[rowIndex, D2Armor.ULTRA_CODE]
	val maxSockets = armor.asInt(rowIndex, D2Armor.GEM_SOCKETS, 0)
	val maxSockets25 = itemTypes.asInt(type, D2ItemTypes.MAX_SOCKETS_1, 0).coerceAtMost(maxSockets)
	val maxSockets40 = itemTypes.asInt(type, D2ItemTypes.MAX_SOCKETS_2, 0).coerceAtMost(maxSockets)
	
	init {
		if (type == "phlm" || type == "ashd" || type == "pelt" || type == "head") {
			when (type) {
				"phlm" -> strings["classOnly"] = items.TEMPLATE_BARBARIAN_ONLY.format()
				"ashd" -> strings["classOnly"] = items.TEMPLATE_PALADIN_ONLY.format()
				"pelt" -> strings["classOnly"] = items.TEMPLATE_DRUID_ONLY.format()
				"head" -> strings["classOnly"] = items.TEMPLATE_NECROMANCER_ONLY.format()
			}
		}
		strings["defense"] = items.TEMPLATE_DEFENSE.format(minDefense, maxDefense, (minDefense + maxDefense) / 2)
		if (rstr > 0) strings["rstr"] = items.TEMPLATE_REQUIRED_STRENGTH.format(rstr)
		if (rlvl > 1) strings["rlvl"] = items.TEMPLATE_REQUIRED_LEVEL.format(rlvl)
		strings["durability"] = items.TEMPLATE_DURABILITY.format(durability)
		if (type == "tors" || type == "shie" || type == "ashd" || type == "head")
			strings["speedPenalty"] = items.TEMPLATE_MOVEMENT_SPEED.format(speedPenalty)
		if (type == "shie" || type == "ashd" || type == "head") {
			strings["block"] = items.TEMPLATE_BLOCK.format(block)
			if (type != "head") {
				if (strBonus > 0) strings["strBonus"] = items.TEMPLATE_STRENGTH_BONUS.format(strBonus)
				strings["smiteDmg"] = items.TEMPLATE_SMITE_DAMAGE.format(minDmg, maxDmg, (minDmg + maxDmg) / 2)
			}
		} else if (type == "boot") {
			strings["strBonus"] = items.TEMPLATE_STRENGTH_BONUS.format(strBonus)
			strings["kickDmg"] = items.TEMPLATE_KICK_DAMAGE.format(minDmg, maxDmg, (minDmg + maxDmg) / 2)
		} else if (type == "belt") {
			strings["belt"] = items.TEMPLATE_BELT_SIZE.format(beltRows)
		}
		if (maxSockets > 0) strings["sockets"] = items.TEMPLATE_SOCKETS.format("$maxSockets25/$maxSockets40/$maxSockets")
		strings["qlvl"] = items.TEMPLATE_QUALITY_LEVEL.format(qlvl)
		if (magiclvl > 0) strings["magiclvl"] = items.TEMPLATE_MAGIC_LEVEL.format(magiclvl)
	}
	
	override fun createEmbed(lang: String) = Embed {
		title = names[lang]
		val builder = StringBuilder()
		if (type == "phlm" || type == "ashd" || type == "pelt" || type == "head")
			builder.appendLine("**${getString(KEY_CLASS_ONLY, lang)}**")
		builder.appendLine(getString(KEY_DEFENSE, lang))
		if (rstr > 0) builder.appendLine(getString(KEY_REQUIRED_STRENGTH, lang))
		if (rlvl > 1) builder.appendLine(getString(KEY_REQUIRED_LEVEL, lang))
		builder.appendLine(getString(KEY_DURABILITY, lang))
		if (type == "tors" || type == "shie" || type == "ashd" || type == "head")
			builder.appendLine(getString(KEY_MOVEMENT_SPEED, lang))
		if (type == "shie" || type == "ashd" || type == "head") {
			builder.appendLine(getString(KEY_BLOCK, lang))
			if (type != "head") {
				if (strBonus > 0) builder.appendLine(getString(KEY_STRENGTH_BONUS, lang))
				builder.appendLine(getString(KEY_SMITE_DAMAGE, lang))
			}
		} else if (type == "boot") {
			builder.appendLine(getString(KEY_STRENGTH_BONUS, lang))
			builder.appendLine(getString(KEY_KICK_DAMAGE, lang))
		} else if (type == "belt") {
			builder.appendLine(getString(KEY_BELT_SIZE, lang))
		}
		if (maxSockets > 0) builder.appendLine(getString(KEY_SOCKETS, lang))
		builder.appendLine(getString(KEY_QUALITY_LEVEL, lang))
		if (magiclvl > 0) builder.appendLine(getString(KEY_MAGIC_LEVEL, lang))
		description = builder.toString()
	}
	
	override fun getString(key: String, lang: String) = strings[key]?.get(lang)
	
	companion object {
		const val KEY_CLASS_ONLY = "classOnly"
		const val KEY_DEFENSE = "defense"
		const val KEY_REQUIRED_STRENGTH = "rstr"
		const val KEY_REQUIRED_LEVEL = "rlvl"
		const val KEY_DURABILITY = "durability"
		const val KEY_MOVEMENT_SPEED = "speedPenalty"
		const val KEY_BLOCK = "block"
		const val KEY_STRENGTH_BONUS = "strBonus"
		const val KEY_SMITE_DAMAGE = "smiteDmg"
		const val KEY_KICK_DAMAGE = "kickDmg"
		const val KEY_BELT_SIZE = "belt"
		const val KEY_SOCKETS = "sockets"
		const val KEY_QUALITY_LEVEL = "qlvl"
		const val KEY_MAGIC_LEVEL = "magiclvl"
	}
	
}