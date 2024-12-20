package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.lang.LangString

class VarRangePropertyData(propertyCode: String, private val variable: IntRange, min: IntRange, max: IntRange, template: TemplateStrings, priority: Int):
	RangePropertyData(propertyCode, min, max, template, priority) {
	
	override fun format(): LangString {
		val argCount = getTemplate().argumentCount
		return if (argCount == 2) {
			if (min.isSame() && max.isSame()) {
				getTemplate().format(min.getAsString(), variable.getAsString())
			} else {
				getTemplate().format("${min.getAsString()}-${max.getAsString()}", variable.getAsString())
			}
		} else {
			getTemplate().format(min.getAsString(), max.getAsString(), variable.getAsString())
		}
	}
	
}