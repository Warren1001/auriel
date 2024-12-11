package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.D2Sheet
import io.github.warren1001.d2data.enums.sheet.D2Skills
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

open class ProcPropertyData(skills: D2Sheet, skillDesc: D2Sheet, items: Items, skillIndex: Int = 0, protected val ilvlMin: Int,
                             propertyCode: String, variable: Any, range: IntRange, template: TemplateStrings, priority: Int):
	OSkillPropertyData(skills, skillDesc, items, skillIndex, propertyCode, variable, range, template, priority) {
	
	override fun format() = getTemplate().insert(skillIndex, getSkillNames())
		.format(range.min, if (range.max == 0) getVariableString() else range.max)
	
	protected fun getVariableString() = "([(ilvl-${getRequiredLevel()})/4]+1 [${getSkillLevelForItemLevel(ilvlMin)}-${getSkillLevelForItemLevel(99)}])"
	
	protected fun getSkillLevelForItemLevel(itemLevel: Int): Int {
		val skillLevel = floor((itemLevel - getRequiredLevel()) / 4.0).toInt() + 1
		return max(1, min(skillLevel, getMaxLevel()))
	}
	
	private fun getRequiredLevel() = if (variable.isInt()) {
		skills.asInt(variable.toString().toInt(), D2Skills.REQ_LEVEL, 1)
	} else {
		skills.asInt(variable.toString(), D2Skills.REQ_LEVEL, 1)
	}
	
	private fun getMaxLevel() = if (variable.isInt()) {
		skills.asInt(variable.toString().toInt(), D2Skills.MAX_LVL, 1)
	} else {
		skills.asInt(variable.toString(), D2Skills.MAX_LVL, 1)
	}
	
}