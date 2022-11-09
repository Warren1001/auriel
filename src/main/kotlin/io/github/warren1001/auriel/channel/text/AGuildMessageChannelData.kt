package io.github.warren1001.auriel.channel.text

import io.github.warren1001.auriel.util.filter.WordFilter

data class AGuildMessageChannelData(val _id: String) {
	
	val configData = mutableMapOf<String, Any>()
	val wordFilters = mutableSetOf<WordFilter>()
	
	fun set(key: String, value: Any): Boolean {
		if (configData[key] == value) return false
		configData[key] = value
		return true
	}
	
	fun setDefault(key: String, value: Any) {
		if (!configData.containsKey(key)) {
			configData[key] = value
		}
	}
	
	fun has(key: String) = configData.containsKey(key)
	fun get(key: String) = configData[key]
	fun getAsString(key: String) = get(key) as String
	fun getAsNumber(key: String) = get(key) as Number
	fun getAsBoolean(key: String) = get(key) as Boolean

}
