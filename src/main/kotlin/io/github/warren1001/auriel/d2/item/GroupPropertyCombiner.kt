package io.github.warren1001.auriel.d2.item

class GroupPropertyCombiner(private val propertyCode: String, private val separatedPropertyCodes: Set<String>, private val combinedTemplate: TemplateStrings): PropertyListTransformer {
	
	override fun contains(propertyCodes: Collection<PropertyData>) = separatedPropertyCodes.all { propertyCode -> propertyCodes.any { it.getPropertyCode() == propertyCode } }
	
	override fun transform(propertyCodes: MutableCollection<PropertyData>) {
		val toCombine = propertyCodes.filter { separatedPropertyCodes.contains(it.getPropertyCode()) }.map { it as SimplePropertyData }
		if (toCombine.size != separatedPropertyCodes.size) error("Properties $separatedPropertyCodes not all found in $propertyCodes")
		val first = toCombine[0]
		if (toCombine.any { it.range != first.range }) return
		propertyCodes.removeAll(toCombine.toSet())
		propertyCodes.add(SimplePropertyData(propertyCode, first.range, combinedTemplate, first.getPriority()))
	}
	
}