package io.github.warren1001.auriel.util.filter

import io.github.warren1001.auriel.replaceOtherAlphabets

data class WordFilter(val name: String, val regex: Regex, val replacement: String = ""): Filter {
	
	override fun containsMatchIn(input: String) = regex.containsMatchIn(input.replaceOtherAlphabets())
	
	fun shouldReplace() = replacement.isNotBlank()
	
	fun replace(input: String) = if (shouldReplace()) regex.replace(input.replaceOtherAlphabets(), replacement) else input
	
	override fun toString() = "WordFilter[name=$name,regex=$regex,replacement=$replacement]"
	
}