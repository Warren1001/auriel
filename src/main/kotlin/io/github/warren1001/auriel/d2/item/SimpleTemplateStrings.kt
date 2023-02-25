package io.github.warren1001.auriel.d2.item

class SimpleTemplateStrings(key: String, private val string: String): TemplateStrings(key, emptyMap()) {
	
	init {
		argumentCount = COUNT_REGEX.findAll(string).count()
	}
	
	override fun get(key: String) = string
	
	override fun format(vararg args: Any): LangStrings {
		return if (argumentCount == 0) SimpleLangStrings(key, string)
		else SimpleLangStrings(key, string.format(*args))
	}
	
}