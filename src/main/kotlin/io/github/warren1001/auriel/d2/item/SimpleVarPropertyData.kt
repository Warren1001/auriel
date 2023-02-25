package io.github.warren1001.auriel.d2.item

import kotlin.math.roundToInt

open class SimpleVarPropertyData(propertyCode: String, val variable: Any, range: IntRange, template: TemplateStrings, priority: Int):
	SimplePropertyData(propertyCode, range, template, priority) {
	
	override fun format(): LangStrings {
		val argCount = getTemplate().argumentCount
		var par = variable
		var range1 = range
		if (par.isInt()) {
			if (getPropertyCode() == "dmg-cold" || getPropertyCode() == "dmg-pois") {
				par = par.toString().toInt() / 25.0
			}
		}
		if (getPropertyCode() == "dmg-pois") {
			range1 = range1.multiply(variable.toString().toInt() / 256.0) { it.roundToInt() }
		}
		return when (argCount) {
			2 -> getTemplate().format(range1.getAsString(), par)
			4 -> getTemplate().format(range1.min, range1.max, par, par)
			else -> getTemplate().format(range1.min, range1.max, par)
		}
	}
	
	override fun toString(): String {
		return "SimpleVarPropertyData[propertyCode='${getPropertyCode()}',\n\tvariable=$variable,\n\trange=$range,\n\ttemplate=${getTemplate()},\n\tpriority=${getPriority()}]"
	}
	
}
