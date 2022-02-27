package io.github.warren1001.auriel.util

class Filter(val regex: Regex, val replacement: String = "") {
	
	fun containsMatchIn(input: CharSequence) = regex.containsMatchIn(input)
	
	fun getAllMatchedStrings(input: CharSequence) = regex.findAll(input).joinToString(separator =  ", ") { it.value }
	
	fun shouldReplace() = replacement.isNotBlank()
	
	fun replace(input: String) = if (shouldReplace()) regex.replace(input, replacement) else input
	
	override fun toString() = "Filter[regex=$regex,replacement=$replacement]"
	
}