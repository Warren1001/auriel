package io.github.warren1001.auriel.d2.item

open class RangePropertyData(private val propertyCode: String, val min: IntRange, val max: IntRange, private val template: TemplateStrings, private val priority: Int): PropertyData {
	
	override fun getPropertyCode() = propertyCode
	
	override fun getTemplate() = template
	
	override fun getPriority() = priority
	
	override fun format(): LangStrings {
		val argCount = template.argumentCount
		return if (argCount == 1) {
			if (min.isSame() && max.isSame()) {
				template.format(min.getAsString())
			} else {
				template.format("${min.getAsString()}-${max.getAsString()}")
			}
		} else {
			template.format(min.getAsString(), max.getAsString())
		}
	}
	
}