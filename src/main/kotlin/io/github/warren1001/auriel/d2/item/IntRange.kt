package io.github.warren1001.auriel.d2.item

data class IntRange(val min: Int, val max: Int) {
	
	fun isSame() = min == max
	
	fun getAsString() = if (isSame()) min.toString() else "[$min-$max]"
	
	fun divide(divisor: Int) = IntRange(min / divisor, max / divisor)
	
	fun multiply(multiplier: Double, rounding: (Double) -> Int = { it.toInt() }) = IntRange(rounding.invoke(min * multiplier), rounding.invoke(max * multiplier))
	
	override fun toString(): String {
		return "IntRange[min=$min, max=$max]"
	}
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false
		
		other as IntRange
		
		if (min != other.min) return false
		if (max != other.max) return false
		
		return true
	}
	
	override fun hashCode(): Int {
		var result = min
		result = 31 * result + max
		return result
	}
	
}