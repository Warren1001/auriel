package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.D2Sheet

open class ChargesPropertyData(skills: D2Sheet, skillDesc: D2Sheet, items: Items, skillIndex: Int = 0,
                             propertyCode: String, variable: Any, range: IntRange, template: TemplateStrings, priority: Int):
	OSkillPropertyData(skills, skillDesc, items, skillIndex, propertyCode, variable, range, template, priority) {
	
	override fun format(): LangStrings {
		val template = getTemplate().insert(skillIndex, getSkillNames())
		return template.format(range.max, range.min)
	}
	
}