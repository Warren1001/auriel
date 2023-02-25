package io.github.warren1001.auriel.d2.item

interface PropertyData {
	
	fun getPropertyCode(): String
	
	fun getTemplate(): TemplateStrings
	
	fun getPriority(): Int
	
	fun format(): LangStrings
	
}