package io.github.warren1001.auriel.d2.item

class PropertySeparator(val propertyCode: String, private val separatedPropertyCodes: Set<String>, private val separator: (PropertyData, String) -> PropertyData): PropertyListTransformer {
	
	override fun contains(propertyCodes: Collection<PropertyData>) = propertyCodes.any { it.getPropertyCode() == propertyCode }
	
	override fun transform(propertyCodes: MutableCollection<PropertyData>) {
		val propertyData = propertyCodes.find { it.getPropertyCode() == propertyCode } ?: error("Property $propertyCode not found in $propertyCodes")
		propertyCodes.remove(propertyData)
		propertyCodes.addAll(separatedPropertyCodes.map { separator.invoke(propertyData, it) })
	}
	
}