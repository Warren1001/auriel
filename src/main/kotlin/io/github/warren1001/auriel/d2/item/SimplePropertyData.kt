package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.lang.LangString

open class SimplePropertyData(private val propertyCode: String, val range: IntRange, private val template: TemplateStrings, private val priority: Int): PropertyData {
	
	override fun getPropertyCode() = propertyCode
	
	override fun getTemplate() = template
	
	override fun getPriority() = priority
	
	override fun format(): LangString {
		val argCount = template.argumentCount
		return if (argCount == 1) {
			val ras = range.getAsString()
			//println("$propertyCode: $ras")
			template.format(ras)
		} else {
			//println("$propertyCode: $argCount")
			template.format(range.min, range.max)
		}
	}
	
	override fun toString(): String {
		return "SimplePropertyData[propertyCode='$propertyCode',\n\trange=$range,\n\ttemplate=$template,\n\tpriority=$priority]"
	}
	
}