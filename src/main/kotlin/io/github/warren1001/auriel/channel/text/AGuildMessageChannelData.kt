package io.github.warren1001.auriel.channel.text

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.warren1001.auriel.util.filter.WordFilter

data class AGuildMessageChannelData(
	val _id: String,
	val configData: MutableMap<String, Any> = mutableMapOf(),
	val wordFilters: MutableSet<WordFilter> = mutableSetOf(),
	val personalThreadDataByChannel: MutableMap<String, PersonalThreadData> = mutableMapOf(),
	@JsonIgnore val personalThreadDataByMember: MutableMap<String, PersonalThreadData> = mutableMapOf(),
) {
	
	init {
		personalThreadDataByChannel.values.forEach {
			personalThreadDataByMember[it.ownerId] = it
		}
	}
	
	fun set(key: String, value: Any): Boolean {
		//if (configData[key] == value) return false
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
	fun getAsString(key: String) = get(key) as String?
	fun getAsNumber(key: String) = get(key) as Number?
	fun getAsBoolean(key: String) = get(key) as Boolean?
	
	fun addPersonalThreadData(data: PersonalThreadData) {
		personalThreadDataByChannel[data._id] = data
		personalThreadDataByMember[data.ownerId] = data
	}
	
	fun removePersonalThreadData(data: PersonalThreadData) {
		personalThreadDataByChannel.remove(data._id)
		personalThreadDataByMember.remove(data.ownerId)
	}

}
