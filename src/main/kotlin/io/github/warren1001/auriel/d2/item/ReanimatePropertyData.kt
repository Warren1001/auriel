package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.D2Sheet
import io.github.warren1001.d2data.enums.D2MonStats

open class ReanimatePropertyData(protected val monStats: D2Sheet, val items: Items,
                              propertyCode: String, variable: Any, range: IntRange, template: TemplateStrings, priority: Int):
	SimpleVarPropertyData(propertyCode, variable, range, template, priority) {
	
	override fun format(): LangStrings {
		val template = getTemplate().insert(1, getMonsterNames())
		return when (template.argumentCount) {
			1 -> template.format(range.getAsString())
			else -> template.format(range.min, range.max)
		}
	}
	
	protected fun getMonsterNames(): TemplateStrings {
		val name = if (variable.isInt()) {
			getMonsterNamesStr(variable.toString().toInt())
		} else {
			getMonsterNamesStr(variable.toString())
		}
		return TemplateStrings(name, items.monsters[name]!!)
	}
	
	private fun getMonsterNamesStr(index: Int) = monStats[index.toString(), D2MonStats.HC_IDX, D2MonStats.NAME_STR]
	
	private fun getMonsterNamesStr(index: String) = monStats[index, D2MonStats.NAME_STR]
	
}