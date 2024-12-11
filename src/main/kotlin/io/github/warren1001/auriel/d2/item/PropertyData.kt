package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.lang.LangString

interface PropertyData {
	
	fun getPropertyCode(): String
	
	fun getTemplate(): TemplateStrings
	
	fun getPriority(): Int
	
	fun format(): LangString
	
}