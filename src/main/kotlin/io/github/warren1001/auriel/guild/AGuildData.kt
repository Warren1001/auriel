package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.util.filter.SpamFilter
import io.github.warren1001.auriel.util.filter.WordFilter
import io.github.warren1001.auriel.util.youtube.YoutubeData

data class AGuildData(val _id: String, val configData: MutableMap<String, Any>) {
	
	val userDefaults = mutableMapOf<String, Any>()
	val wordFilters = mutableSetOf<WordFilter>()
	val spamFilters = mutableSetOf<SpamFilter>()
	val vouchBlacklist = mutableSetOf<String>()
	val privateChannelData = mutableMapOf<String, PrivateChannelData>()
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
	
	fun setUserDefault(key: String, value: Any) {
		userDefaults[key] = value
	}
	
	fun has(key: String) = configData.containsKey(key)
	fun get(key: String) = configData[key]
	fun getAsString(key: String) = get(key) as String
	fun getAsNumber(key: String) = get(key) as Number
	fun getAsBoolean(key: String) = get(key) as Boolean
	
}