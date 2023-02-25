package io.github.warren1001.auriel.d2.item

class MultiLangStrings(private val key: String, private val strings: MutableMap<String, String>): LangStrings {
	
	override fun get(lang: String) = strings[lang] ?: error("No string in $key for language $lang")
	
	override fun getKey() = key
	
	override fun append(other: LangStrings, separator: String) {
		for (string in strings) {
			strings[string.key] = string.value + separator + other.get(string.key)
		}
	}
	
	override fun clone() = MultiLangStrings(key, strings.toMutableMap())
	
	override fun toString() = "MultiLangStrings[key='$key', strings=$strings]"
	
}