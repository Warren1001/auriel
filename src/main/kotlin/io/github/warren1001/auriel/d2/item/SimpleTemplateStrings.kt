package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.lang.LangString
import io.github.warren1001.d2data.lang.SingleLangString

class SimpleTemplateStrings(key: String, private val string: String): TemplateStrings(key, emptyMap()) {
	
	init {
		argumentCount = COUNT_REGEX.findAll(string).count()
	}
	
	override fun get(key: String) = string
	
	override fun format(vararg args: Any): LangString {
		return if (argumentCount == 0) SingleLangString(key, string)
		else SingleLangString(key, string.format(*args))
	}
	
}