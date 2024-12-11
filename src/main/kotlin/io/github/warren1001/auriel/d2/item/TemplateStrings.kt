package io.github.warren1001.auriel.d2.item

import io.github.warren1001.d2data.lang.LangString
import io.github.warren1001.d2data.lang.MultiLangString

open class TemplateStrings(protected val key: String, private val strings: Map<String, String>) {
	
	var argumentCount = if (strings.isEmpty()) 0 else COUNT_REGEX.findAll(strings.values.first()).count()
	
	open fun get(key: String) = strings[key]
	
	open fun format(vararg args: Any): LangString {
		return if (argumentCount == 0) MultiLangString(key, strings.toMutableMap())
		else MultiLangString(key, strings.mapValues {
			try {
				it.value.format(*args).replace("+-", "-")
			} catch (e: Exception) {
				throw Exception("Error formatting string $key for language ${it.key}: args: '${args.joinToString(" ") }', 'value: ${it.value}'", e)
			}
		}.toMutableMap())
	}
	
	open fun insert(index: Int = 0, otherStrings: Map<String, String>): TemplateStrings {
		assert(strings.keys == otherStrings.keys) { "Cannot format strings with different languages" }
		return TemplateStrings("$key-insert$index", strings.mapValues {
			val otherStr = otherStrings[it.key]!!
			if (it.value.contains("%s")) {
				var startIndex = it.value.indexOf("%s")
				for (i in 0 until index) {
					startIndex = it.value.indexOf("%s", startIndex + 2)
				}
				it.value.replaceRange(startIndex, startIndex + 2, otherStr)
			} else it.value.replace("%${index + 1}\$s", otherStr).replace("%${index + 2}", "%${index + 1}").replace("%${index + 3}", "%${index + 2}").replace("%${index + 4}", "%${index + 3}")
		})
	}
	
	open fun insert(index: Int = 0, other: TemplateStrings) = insert(index, other.strings)
	
	fun merge(other: TemplateStrings, separator: String, format: String = ""): TemplateStrings {
		return TemplateStrings("$key-${other.key}", strings.mapValues { it.value + separator + if (format.isEmpty()) other.get(it.key)!! else format.format(other.get(it.key)!!) })
	}
	
	override fun toString() = "TemplateStrings[key='$key', strings=$strings]"
	
	companion object {
		val COUNT_REGEX = Regex("%[^% ]")
	}
	
}