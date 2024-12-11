package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.D2Sheet
import io.github.warren1001.d2data.enums.sheet.D2SkillDesc
import io.github.warren1001.d2data.enums.sheet.D2Skills
import io.github.warren1001.d2data.lang.LangString

open class OSkillPropertyData(protected val skills: D2Sheet, protected val skillDesc: D2Sheet, val items: Items, protected var skillIndex: Int = 0,
                             propertyCode: String, variable: Any, range: IntRange, template: TemplateStrings, priority: Int):
	SimpleVarPropertyData(propertyCode, variable, range, template, priority) {
	
	override fun format(): LangString {
		val template = getTemplate().insert(skillIndex, getSkillNames())
		return when (template.argumentCount) {
			1 -> template.format(range.getAsString())
			else -> template.format(range.min, range.max)
		}
	}
	
	protected fun getSkillNames(): TemplateStrings {
		val skillDescIndex: String = if (variable.isInt()) {
			getSkillDescIndex(variable.toString().toInt())
		} else {
			getSkillDescIndex(variable.toString())
		}
		val name = skillDesc[skillDescIndex, D2SkillDesc.STR_NAME]
		return TemplateStrings(name, items.skills[name].getStrings())
	}
	
	private fun getSkillDescIndex(index: Int) = skills[index, D2Skills.SKILL_DESC]
	
	private fun getSkillDescIndex(index: String) = skills[index, D2Skills.SKILL_DESC]
	
}