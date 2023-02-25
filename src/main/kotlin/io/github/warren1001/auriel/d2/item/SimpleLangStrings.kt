package io.github.warren1001.auriel.d2.item

/**
 * mostly for strings where translations aren't available
 */
class SimpleLangStrings(private val key: String, private val string: String): LangStrings {
	
	override fun get(lang: String) = string
	
	override fun getKey() = key
	
	override fun append(other: LangStrings, separator: String) = throw UnsupportedOperationException("Cannot append to a SimpleLangStrings")
	
	override fun clone() = SimpleLangStrings(key, string)
	
	override fun toString() = "SimpleLangStrings[key='$key', string='$string']"
	
}