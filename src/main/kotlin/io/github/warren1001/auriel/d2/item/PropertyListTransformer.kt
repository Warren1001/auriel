package io.github.warren1001.auriel.d2.item

interface PropertyListTransformer {
	
	fun contains(propertyCodes: Collection<PropertyData>): Boolean
	
	fun transform(propertyCodes: MutableCollection<PropertyData>)
	
}