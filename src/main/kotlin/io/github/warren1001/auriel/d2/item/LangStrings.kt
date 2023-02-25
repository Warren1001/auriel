package io.github.warren1001.auriel.d2.item

interface LangStrings {
	
	fun get(lang: String): String
	
	fun getKey(): String
	
	fun append(other: LangStrings, separator: String)
	
	fun clone(): LangStrings
	
}