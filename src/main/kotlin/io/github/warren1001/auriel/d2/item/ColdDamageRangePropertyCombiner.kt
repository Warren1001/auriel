package io.github.warren1001.auriel.d2.item

import kotlin.math.max

class ColdDamageRangePropertyCombiner(private val propertyCode: String, items: Items): PropertyListTransformer {
	
	private val combinedTemplate = TemplateStrings("strModColdDamageRange", items.itemModifiers["strModColdDamageRange"]!!)
		.merge(TemplateStrings("timeSecondsFormatString", items.ui["timeSecondsFormatString"]!!), " ", "(%s)")
	
	override fun contains(propertyCodes: Collection<PropertyData>) = propertyCodes.any { it.getPropertyCode() == "cold-min" }
			&& propertyCodes.any { it.getPropertyCode() == "cold-max" }
			&& propertyCodes.any { it.getPropertyCode() == "cold-len" }
	
	override fun transform(propertyCodes: MutableCollection<PropertyData>) {
		val coldMin = propertyCodes.find { it.getPropertyCode() == "cold-min" } as? SimplePropertyData
		val coldMax = propertyCodes.find { it.getPropertyCode() == "cold-max" } as? SimplePropertyData
		val coldLen = propertyCodes.find { it.getPropertyCode() == "cold-len" } as? SimplePropertyData
		if (coldMin == null || coldMax == null || coldLen == null) error("Properties cold-min, cold-max, cold-len not all found in $propertyCodes")
		propertyCodes.remove(coldMin)
		propertyCodes.remove(coldMax)
		propertyCodes.remove(coldLen)
		propertyCodes.add(VarRangePropertyData(propertyCode, coldLen.range.divide(25), coldMin.range, coldMax.range, combinedTemplate, max(coldMin.getPriority(), coldMax.getPriority())))
	}
	
}