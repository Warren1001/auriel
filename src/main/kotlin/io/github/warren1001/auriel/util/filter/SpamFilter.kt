package io.github.warren1001.auriel.util.filter

data class SpamFilter(val name: String, val regexes: MutableSet<Regex>): Filter {
	
	override fun containsMatchIn(input: CharSequence) = regexes.all { it.containsMatchIn(input) }
	
	override fun toString() = "SpamFilter[name=$name,regexes=$regexes]"
	
}