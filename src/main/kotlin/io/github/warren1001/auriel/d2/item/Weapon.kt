package io.github.warren1001.auriel.d2.item

import dev.minn.jda.ktx.messages.Embed
import io.github.warren1001.d2data.impl.D2ItemTypes
import io.github.warren1001.d2data.impl.D2Weapons

class Weapon(names: Map<String, String>, rowIndex: Int, weapons: D2Weapons, itemTypes: D2ItemTypes):
	EquippableItem(names,
		qlvl = weapons.getValueAsInt(rowIndex, D2Weapons.LEVEL, 0),
		rlvl = weapons.getValueAsInt(rowIndex, D2Weapons.LEVEL_REQ, 0),
		rstr = weapons.getValueAsInt(rowIndex, D2Weapons.REQ_STR, 0),
		rdex = weapons.getValueAsInt(rowIndex, D2Weapons.REQ_DEX, 0),
		durability = weapons.getValueAsInt(rowIndex, D2Weapons.DURABILITY, 0),
		hasDurability = weapons.getValueAsInt(rowIndex, D2Weapons.NO_DURABILITY, 0) == 1
	) {
	
	val type = weapons.getValue(rowIndex, D2Weapons.TYPE)
	val min1h = weapons.getValueAsInt(rowIndex, D2Weapons.MIN_DAM, 0)
	val max1h = weapons.getValueAsInt(rowIndex, D2Weapons.MAX_DAM, 0)
	val min2h = weapons.getValueAsInt(rowIndex, D2Weapons.TWO_HAND_MIN_DAM, 0)
	val max2h = weapons.getValueAsInt(rowIndex, D2Weapons.TWO_HAND_MAX_DAM, 0)
	val range = weapons.getValueAsInt(rowIndex, D2Weapons.RANGE_ADDER, 0)
	val wsm = weapons.getValueAsInt(rowIndex, D2Weapons.SPEED, 0)
	val strBonus = weapons.getValueAsInt(rowIndex, D2Weapons.STR_BONUS, 0)
	val dexBonus = weapons.getValueAsInt(rowIndex, D2Weapons.DEX_BONUS, 0)
	val magiclvl = weapons.getValueAsInt(rowIndex, D2Weapons.MAGIC_LVL, 0)
	val normCode = weapons.getValue(rowIndex, D2Weapons.NORM_CODE)
	val excepCode = weapons.getValue(rowIndex, D2Weapons.UBER_CODE)
	val eliteCode = weapons.getValue(rowIndex, D2Weapons.ULTRA_CODE)
	val maxSockets = weapons.getValueAsInt(rowIndex, D2Weapons.GEM_SOCKETS, 0)
	val maxSockets25 = itemTypes.getValueAsInt(type, D2ItemTypes.MAX_SOCKETS_1, 0).coerceAtMost(maxSockets)
	val maxSockets40 = itemTypes.getValueAsInt(type, D2ItemTypes.MAX_SOCKETS_2, 0).coerceAtMost(maxSockets)
	
	override fun createEmbed(lang: String) = Embed {
		title = names[lang] ?: names["enUS"] ?: "Unknown"
		field("Weapon Specific") {
			inline = false
			value = (if (min1h > 0) "1H Damage: $min1h-$max1h\n" else "") +
					(if (min2h > 0) "2H Damage: $min2h-$max2h\n" else "") +
					"Range: $range\n" +
					"Speed: $wsm\n" +
					(if (strBonus > 0) "Strength Bonus: $strBonus%\n" else "") +
					(if (dexBonus > 0) "Dexterity Bonus: $dexBonus%\n" else "")
		}
		field("General") {
			inline = false
			value = (if (rlvl > 1) "Req Level: $rlvl\n" else "") +
					(if (rstr > 0) "Req Strength: $rstr\n" else "") +
					(if (rdex > 0) "Req Dexterity: $rdex\n" else "") +
					(if (hasDurability) "Durability: $durability\n" else "") +
					"Max Sockets: $maxSockets25/$maxSockets40/$maxSockets\n"
		}
		field("Technical") {
			inline = false
			value = "Quality Level: $qlvl\n" +
					(if (magiclvl > 0) "Magic Level: $magiclvl" else "")
		}
	}
	
}