package io.github.warren1001.auriel.d2.item

import kotlin.math.max

class RangePropertyCombiner(private val propertyCode: String, private val separatedPropertyCodes: Set<String>, private val combinedTemplate: TemplateStrings): PropertyListTransformer {
	
	override fun contains(propertyCodes: Collection<PropertyData>) = separatedPropertyCodes.all { propertyCode -> propertyCodes.any { it.getPropertyCode() == propertyCode } }
	
	override fun transform(propertyCodes: MutableCollection<PropertyData>) {
		val toCombine = propertyCodes.filter { separatedPropertyCodes.contains(it.getPropertyCode()) }
		if (toCombine.size != separatedPropertyCodes.size) error("Properties $separatedPropertyCodes not all found in $propertyCodes")
		if (toCombine.size != 2) error("Property $propertyCode can only be combined from the two properties $separatedPropertyCodes")
		propertyCodes.removeAll(toCombine.toSet())
		val c1 = toCombine[0] as SimplePropertyData
		val c2 = toCombine[1] as SimplePropertyData
		propertyCodes.add(RangePropertyData(propertyCode, c1.range, c2.range, combinedTemplate, max(c1.getPriority(), c2.getPriority())))
	}
	
}