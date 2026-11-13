package io.github.warren1001.auriel.user

import io.github.warren1001.auriel.d2.clone.CloneDataHelper

data class AUserData(
	val _id: String,
	val userData: MutableMap<String, Any> = mutableMapOf(),
	val vouches: MutableList<Vouch> = mutableListOf(),
	var lastVouch: Long = 0L,
	var dataCloneHelper: CloneDataHelper? = null,
) {
	
	fun verifyDefaults(defaults: Map<String, Any>): Boolean {
		var changes = false
		defaults.forEach { (key, value) ->
			if (!userData.containsKey(key)) {
				changes = true
				userData[key] = value
			}
		}
		return changes
	}
	
	fun set(key: String, value: Any): Boolean {
		if (userData[key] == value) return false
		userData[key] = value
		return true
	}
	
	fun setDefault(key: String, value: Any) {
		if (!userData.containsKey(key)) {
			userData[key] = value
		}
	}
	
	fun has(key: String) = userData.containsKey(key)
	fun get(key: String) = userData[key]
	fun getOrDefault(key: String, default: Any) = userData.getOrDefault(key, default)
	fun getAsString(key: String) = get(key) as String
	fun getAsNumber(key: String, default: Int = 0) = getOrDefault(key, default) as Number
	fun getAsBoolean(key: String) = get(key) as Boolean
	fun incrementInt(key: String, amount: Int = 1): Int {
		val newValue = getAsNumber(key).toInt() + amount
		set(key, newValue)
		return newValue
	}
	
	fun computeCloneData(): CloneDataHelper {
		if (dataCloneHelper == null) dataCloneHelper = CloneDataHelper()
		return dataCloneHelper!!
	}

}
