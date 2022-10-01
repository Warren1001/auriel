package io.github.warren1001.auriel.util.filter

data class WordFilter(val source: String, val regex: Regex, val replacement: String = ""): Filter {
	
	override fun containsMatchIn(input: CharSequence) = regex.containsMatchIn(input)
	
	fun shouldReplace() = replacement.isNotBlank()
	
	fun replace(input: String) = if (shouldReplace()) regex.replace(input, replacement) else input
	
	override fun toString() = "WordFilter[source=$source,regex=$regex,replacement=$replacement]"
	
}