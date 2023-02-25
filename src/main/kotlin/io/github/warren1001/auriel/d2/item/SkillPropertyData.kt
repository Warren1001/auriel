package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.D2Sheet
import io.github.warren1001.d2data.enums.D2CharStats
import io.github.warren1001.d2data.enums.D2PlayerClass
import io.github.warren1001.d2data.enums.D2Skills

open class SkillPropertyData(skills: D2Sheet, skillDesc: D2Sheet, items: Items, skillIndex: Int = 0, private var classOnlyIndex: Int = 1,
                             propertyCode: String, variable: Any, range: IntRange, template: TemplateStrings, priority: Int):
	OSkillPropertyData(skills, skillDesc, items, skillIndex, propertyCode, variable, range, template, priority) {
	
	override fun format(): LangStrings {
		val template = getTemplate().insert(classOnlyIndex, getClassOnlyNames()).insert(skillIndex, getSkillNames())
		return when (template.argumentCount) {
			1 -> template.format(range.getAsString())
			else -> template.format(range.min, range.max)
		}
	}
	
	protected fun getClassOnlyNames(): TemplateStrings {
		val playerClassIndex: String = if (variable.isInt()) {
			getPlayerClassIndex(variable.toString().toInt())
		} else {
			getPlayerClassIndex(variable.toString())
		}
		val charStatsIndex = items.manager.loadSheet(D2PlayerClass.SHEET_NAME)[playerClassIndex, D2PlayerClass.PLAYER_CLASS]
		val classOnlyName = items.manager.loadSheet(D2CharStats.SHEET_NAME)[charStatsIndex, D2CharStats.STR_CLASS_ONLY]
		return TemplateStrings(classOnlyName, items.itemModifiers[classOnlyName]!!)
	}
	
	private fun getPlayerClassIndex(index: Int) = skills[index, D2Skills.CHAR_CLASS]
	
	private fun getPlayerClassIndex(index: String) = skills[index, D2Skills.CHAR_CLASS]
	
}