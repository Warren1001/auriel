package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.util.filter.Filter
import io.github.warren1001.auriel.util.filter.WordFilter
import io.github.warren1001.auriel.util.youtube.YoutubeData

data class AGuildData(val _id: String, val configData: MutableMap<String, Any> = mutableMapOf()) {
	
	val wordFilters = mutableSetOf<WordFilter>()
	val spamFilters = mutableSetOf<Filter>()
	var youtubeData: YoutubeData = YoutubeData()
	var nextVouchId = 0L
	
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