package io.github.warren1001.auriel.d2.item

import kotlin.math.max

class PoisonDamageRangePropertyCombiner(private val propertyCode: String, items: Items) : PropertyListTransformer {
	
	private val combinedTemplate = TemplateStrings("strModPoisonDamage", items.itemModifiers["strModPoisonDamage"]!!)
	
	override fun contains(propertyCodes: Collection<PropertyData>) = propertyCodes.any { it.getPropertyCode() == "pois-min" }
			&& propertyCodes.any { it.getPropertyCode() == "pois-max" }
			&& propertyCodes.any { it.getPropertyCode() == "pois-len" }
	
	override fun transform(propertyCodes: MutableCollection<PropertyData>) {
		val poisMin = propertyCodes.find { it.getPropertyCode() == "pois-min" } as? SimplePropertyData
		val poisMax = propertyCodes.find { it.getPropertyCode() == "pois-max" } as? SimplePropertyData
		val poisLen = propertyCodes.find { it.getPropertyCode() == "pois-len" } as? SimplePropertyData
		if (poisMin == null || poisMax == null || poisLen == null) error("Properties pois-min, pois-max, pois-len not all found in $propertyCodes")
		propertyCodes.remove(poisMin)
		propertyCodes.remove(poisMax)
		propertyCodes.remove(poisLen)
		val length = poisLen.range.min / 25
		propertyCodes.add(
			VarRangePropertyData(
				propertyCode, poisLen.range.divide(25), poisMin.range.multiply(length / 256.0), poisMax.range.multiply(length / 256.0),
				combinedTemplate, max(poisMin.getPriority(), poisMax.getPriority())
			)
		)
	}
	
}