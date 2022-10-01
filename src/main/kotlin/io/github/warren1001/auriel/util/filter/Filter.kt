package io.github.warren1001.auriel.util.filter

interface Filter {
	
	fun containsMatchIn(input: CharSequence): Boolean
	
}