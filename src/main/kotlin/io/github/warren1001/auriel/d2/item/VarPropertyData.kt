package io.github.warren1001.auriel.d2.item

data class VarPropertyData(private val propertyCode: String, val variable: Int, private val template: TemplateStrings, private val priority: Int): PropertyData {
	
	override fun getPropertyCode() = propertyCode
	
	override fun getTemplate() = template
	
	override fun getPriority() = priority
	
	override fun format() = template.format(
		if (propertyCode.endsWith("/lvl")) {
			val v = variable / 8.0
			val va = if (v == v.toInt().toDouble()) v.toInt() else v
			val min = v.toInt()
			val max = (v * 99).toInt()
			"($va [$min-$max])"
		}
		else if (propertyCode == "rep-dur" || propertyCode == "rep-quant") 100 / variable
		else variable
	)
	
	override fun toString(): String {
		return "VarPropertyData[propertyCode='$propertyCode',\n\tvariable=$variable,\n\ttemplate=$template,\n\tpriority=$priority]"
	}
	
}