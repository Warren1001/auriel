package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.enums.*
import kotlin.math.max

/**
 * class skills are in charstats.txt, self explanatory, skill value + 1 = index (incl header), skill value = index (no header)
 * tab skills are also in charstats.txt, tab value for strskilltab row wrapping. tab 9 would be row 4 column 1, paladin's strskltabitem6.
 */
class Properties(private val items: Items) {
	
	/**
	 * todo
	 * basemod
	 * skill-rand
	 * randclassskill
	 * dur (add directly to durability)
	 * stack, rep-quant (display additional info)
	 */
	
	private val propertyListTransformers = mutableListOf(
		PropertySeparator(
			"res-all-max", setOf(
				"res-fire-max",
				"res-ltng-max",
				"res-cold-max",
				"res-pois-max"
			)
		) { original, new ->
			getPropertyData(
				new,
				when (original) {
					is VarPropertyData -> original.variable.toString()
					is SimpleVarPropertyData -> original.variable.toString()
					else -> ""
				},
				if (original is SimplePropertyData) original.range else null,
				1
			)
		  },
		RangePropertyCombiner(
			"dmg-fire", setOf(
				"fire-min",
				"fire-max"
			),
			TemplateStrings("strModFireDamageRange", items.itemModifiers["strModFireDamageRange"]!!)
		),
		RangePropertyCombiner(
			"dmg-ltng", setOf(
				"ltng-min",
				"ltng-max"
			),
			TemplateStrings("strModLightningDamageRange", items.itemModifiers["strModLightningDamageRange"]!!)
		),
		ColdDamageRangePropertyCombiner("dmg-cold", items),
		PoisonDamageRangePropertyCombiner("dmg-pois", items),
		RangePropertyCombiner(
			"dmg-norm", setOf(
				"dmg-min",
				"dmg-max"
			),
			TemplateStrings("strModMinDamageRange", items.itemModifiers["strModMinDamageRange"]!!)
		),
		PropertySeparator(
			"dmg-elem", setOf(
				"dmg-fire",
				"dmg-ltng",
				"dmg-cold"
			)
		) { original, new ->
			getPropertyData(
				new,
				when (original) {
					is VarPropertyData -> original.variable.toString()
					is SimpleVarPropertyData -> original.variable.toString()
					else -> ""
				},
				if (original is SimplePropertyData) original.range else null,
				1
			)
		},
		GroupPropertyCombiner(
			"all-stats", setOf(
				"str",
				"dex",
				"vit",
				"enr"
			),
			TemplateStrings("Moditem2allattrib", items.itemModifiers["Moditem2allattrib"]!!)
		),
		RangePropertyCombiner(
			"ethereal-sock", setOf(
				"ethereal",
				"sock"
			),
			TemplateStrings("strItemModEtherealSocketed", items.itemModifiers["strItemModEtherealSocketed"]!!)
		)
	)
	
	fun getUniquePropertiesString(index: String): LangStrings {
		val uniqueItems = items.manager.loadSheet(D2UniqueItems.SHEET_NAME)
		val ilvlMin = getMinimumItemLevel(index)
		val propertyDataList = mutableListOf<PropertyData>()
		for (i in 1..MAXIMUM_PARAMETER_COUNT) {
			val prop = uniqueItems[index, "prop$i"]
			if (prop.isEmpty()) break
			if (prop.startsWith('*') || prop == "bloody") continue
			val par = uniqueItems[index, "par$i"].ifEmpty { if (prop == "dmg-cold") "0" else "" }
			val min = uniqueItems.asInt(index, "min$i", Int.MIN_VALUE)
			val max = uniqueItems.asInt(index, "max$i", Int.MIN_VALUE)
			val range = if (min == Int.MIN_VALUE || max == Int.MIN_VALUE) null else IntRange(min, max)
			propertyDataList.add(getPropertyData(prop, par, range, ilvlMin))
		}
		propertyListTransformers.filter { it.contains(propertyDataList) }.forEach { it.transform(propertyDataList) }
		val a = propertyDataList.sortedByDescending { it.getPriority() }.map {
			//"$it\n${it.format()}"
			it.format()
		}
		//println(a)
		val b = a[0]
		for (i in 1..a.lastIndex) {
			b.append(a[i], "\n")
		}
		return b
		//return SimpleLangStrings("", "")
	}
	
	fun getUniquePropertiesString(index: Int): LangStrings {
		val uniqueItems = items.manager.loadSheet(D2UniqueItems.SHEET_NAME)
		val ilvlMin = getMinimumItemLevel(index)
		val propertyDataList = mutableListOf<PropertyData>()
		for (i in 1..MAXIMUM_PARAMETER_COUNT) {
			val prop = uniqueItems[index, "prop$i"]
			if (prop.isEmpty()) break
			if (prop.startsWith('*') || prop == "bloody") continue
			val par = uniqueItems[index, "par$i"].ifEmpty { if (prop == "dmg-cold") "0" else "" }
			val min = uniqueItems.asInt(index, "min$i", Int.MIN_VALUE)
			val max = uniqueItems.asInt(index, "max$i", Int.MIN_VALUE)
			val range = if (min == Int.MIN_VALUE || max == Int.MIN_VALUE) null else IntRange(min, max)
			propertyDataList.add(getPropertyData(prop, par, range, ilvlMin))
		}
		propertyListTransformers.filter { it.contains(propertyDataList) }.forEach { it.transform(propertyDataList) }
		val a = propertyDataList.sortedByDescending { it.getPriority() }.map {
			//"$it\n${it.format()}"
			it.format()
		}
		//println(a)
		val b = a[0].clone()
		for (i in 1..a.lastIndex) {
			b.append(a[i], "\n")
		}
		return b
	}
	
	private fun getMinimumItemLevel(index: Int): Int {
		val uniqueItems = items.manager.loadSheet(D2UniqueItems.SHEET_NAME)
		val ulvl = uniqueItems.asInt(index, D2UniqueItems.LVL, 1)
		val code = uniqueItems[index, D2UniqueItems.CODE]
		//println("code: $code")
		val qlvl = items.getBaseItem(code)?.qlvl ?: 1
		return max(ulvl, qlvl)
	}
	
	private fun getMinimumItemLevel(index: String): Int {
		val uniqueItems = items.manager.loadSheet(D2UniqueItems.SHEET_NAME)
		val ulvl = uniqueItems.asInt(index, D2UniqueItems.LVL, 1)
		val qlvl = items.getBaseItem(uniqueItems[index, D2UniqueItems.CODE])!!.qlvl
		return max(ulvl, qlvl)
	}
	
	private fun getPropertyData(code: String, par: String, range: IntRange?, ilvlMin: Int): PropertyData {
		val templateAndPriority = getTemplateAndPriority(code, par.toIntOrNull() ?: -1)
		return if (par.isNotEmpty()) {
			if (code == "skill") {
				SkillPropertyData(items.manager.loadSheet(D2Skills.SHEET_NAME), items.manager.loadSheet(D2SkillDesc.SHEET_NAME), items, 1, 2, code, par, range!!,
					templateAndPriority.first, templateAndPriority.second)
			} else if (code == "oskill") {
				OSkillPropertyData(items.manager.loadSheet(D2Skills.SHEET_NAME), items.manager.loadSheet(D2SkillDesc.SHEET_NAME), items, 1, code, par, range!!, templateAndPriority.first, templateAndPriority.second)
			} else if (code == "gethit-skill" || code == "hit-skill" || code == "att-skill" || code == "kill-skill") {
				ProcPropertyData(items.manager.loadSheet(D2Skills.SHEET_NAME), items.manager.loadSheet(D2SkillDesc.SHEET_NAME), items, 2, ilvlMin, code, par, range!!, templateAndPriority.first, templateAndPriority.second)
			} else if (code == "charged") {
				ChargesPropertyData(items.manager.loadSheet(D2Skills.SHEET_NAME), items.manager.loadSheet(D2SkillDesc.SHEET_NAME), items, 1, code, par, range!!, templateAndPriority.first, templateAndPriority.second)
			} else if (code == "reanimate") {
				ReanimatePropertyData(items.manager.loadSheet(D2MonStats.SHEET_NAME), items, code, par, range!!, templateAndPriority.first, templateAndPriority.second)
			} else if (range != null) {
				SimpleVarPropertyData(code, par, range, templateAndPriority.first, templateAndPriority.second)
			} else {
				VarPropertyData(code, par.toInt(), templateAndPriority.first, templateAndPriority.second)
			}
		} else {
			SimplePropertyData(code, range!!, templateAndPriority.first, templateAndPriority.second)
		}
	}
	
	private fun getTemplateAndPriority(code: String, value: Int): Pair<TemplateStrings, Int> {
		return when (code) {
			"res-all" -> TemplateStrings("strModAllResistances", items.itemModifiers["strModAllResistances"]!!) to getPriorityOfStat("fireresist")
			"dmg%" -> TemplateStrings("strModEnhancedDamage", items.itemModifiers["strModEnhancedDamage"]!!) to getPriorityOfStat("item_mindamage_percent")
			"dmg-fire" -> TemplateStrings("strModFireDamageRange", items.itemModifiers["strModFireDamageRange"]!!) to getPriorityOfStat("firemindam")
			"dmg-ltng" -> TemplateStrings("strModLightningDamageRange", items.itemModifiers["strModLightningDamageRange"]!!) to getPriorityOfStat("lightmindam")
			"dmg-mag" -> TemplateStrings("strModMagicDamageRange", items.itemModifiers["strModMagicDamageRange"]!!) to getPriorityOfStat("magicmindam")
			"dmg-cold" -> TemplateStrings("strModColdDamageRange", items.itemModifiers["strModColdDamageRange"]!!).merge(TemplateStrings("timeSecondsFormatString", items
				.ui["timeSecondsFormatString"]!!), " ", "(%s)") to getPriorityOfStat("coldmindam")
			"dmg-pois" -> TemplateStrings("strModPoisonDamage", items.itemModifiers["strModPoisonDamage"]!!) to getPriorityOfStat("poisonmindam")
			"dmg-norm" -> TemplateStrings("strModMinDamageRange", items.itemModifiers["strModMinDamageRange"]!!) to getPriorityOfStat("mindamage")
			"sock" -> TemplateStrings("Socketable", items.itemModifiers["Socketable"]!!) to 0
			"dur" -> TemplateStrings("ModStr2i", items.itemModifiers["ModStr2i"]!!.mapValues { it.value.replace("%%", "") }) to getPriorityOfStat("item_maxdurability_percent")
			"pois-len" -> TemplateStrings("", mapOf()) to -1
			"cold-len" -> TemplateStrings("", mapOf()) to -1
			"skilltab" -> {
				val row = value / 3
				val col = value % 3 + 1
				val charStats = items.manager.loadSheet(D2CharStats.SHEET_NAME)
				val strings = charStats[row, "StrSkillTab$col"]
				val only = charStats[row, D2CharStats.STR_CLASS_ONLY]
				TemplateStrings(strings, items.itemModifiers[strings]!!).merge(TemplateStrings(only, items.itemModifiers[only]!!), " ") to getPriorityOfStat("item_addskill_tab")
			}
			else -> {
				val properties = items.manager.loadSheet(D2Properties.SHEET_NAME)
				val stat = when (code) {
					"indestruct" -> "item_indesctructible"
					"ethereal" -> code
					"dmg-min" -> "mindamage"
					"dmg-max" -> "maxdamage"
					else -> properties[code, D2Properties.STAT_1]
				}
				val statVal = properties.asInt(code, D2Properties.VAL_1, -1)
				val descriptionKey = getDescriptionKeyOfStat(stat, statVal)
				val description2Key = getDescription2KeyOfStat(stat)
				val template = if (description2Key.isNotEmpty()) {
					TemplateStrings(descriptionKey, items.itemModifiers[descriptionKey]!!).merge(TemplateStrings(description2Key, items.itemModifiers[description2Key]!!), " ")
				} else {
					//println("$code, $stat: $descriptionKey")
					if (stat == "item_charged_skill") {
						TemplateStrings(descriptionKey, items.itemModifiers[descriptionKey]!!.mapValues { it.value.replace("%s/%s", "%s").replace("%3\$s/%4\$s", "%3\$s") })
					}
					else TemplateStrings(descriptionKey, items.itemModifiers[descriptionKey]!!)
				}
				template to getPriorityOfStat(stat)
			}
		}
	}
	
	private fun getPriorityOfStat(statCode: String): Int {
		return if (statCode == "ethereal") 0 else items.manager.loadSheet(D2ItemStatCost.SHEET_NAME).asInt(statCode, D2ItemStatCost.DESC_PRIORITY)
	}
	
	private fun getDescriptionKeyOfStat(statCode: String, value: Int): String {
		return when (statCode) {
			"ethereal" -> "strethereal"
			"item_addclassskills" -> items.manager.loadSheet(D2CharStats.SHEET_NAME)[value, D2CharStats.STR_ALL_SKILLS]
			else -> items.manager.loadSheet(D2ItemStatCost.SHEET_NAME)[statCode, D2ItemStatCost.DESC_STR_POS]
		}
	}
	
	private fun getDescription2KeyOfStat(statCode: String): String {
		return if (statCode == "ethereal") "" else items.manager.loadSheet(D2ItemStatCost.SHEET_NAME)[statCode, D2ItemStatCost.DESC_STR_2]
	}
	
	companion object {
		const val MAXIMUM_STAT_COUNT = 7
		const val MAXIMUM_PARAMETER_COUNT = 12
	}
	
}