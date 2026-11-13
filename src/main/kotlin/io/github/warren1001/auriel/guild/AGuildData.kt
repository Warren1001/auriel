package io.github.warren1001.auriel.guild

import com.fasterxml.jackson.annotation.JsonInclude
import io.github.warren1001.auriel.d2.clone.CloneData
import io.github.warren1001.auriel.toMessageCreateData
import io.github.warren1001.auriel.util.filter.SpamFilter
import io.github.warren1001.auriel.util.filter.WordFilter
import io.github.warren1001.auriel.util.youtube.YoutubeData
import net.dv8tion.jda.api.utils.messages.MessageCreateData

data class AGuildData(
	val _id: String,
	val configData: MutableMap<String, Any>,
	val userDefaults: MutableMap<String, Any> = mutableMapOf(),
	val wordFilters: MutableSet<WordFilter> = mutableSetOf(),
	val spamFilters: MutableSet<SpamFilter> = mutableSetOf(),
	val vouchBlacklist: MutableSet<String> = mutableSetOf(),
	val privateChannelData: MutableMap<String, PrivateChannelData> = mutableMapOf(),
	var youtubeData: YoutubeData = YoutubeData(),
	var nextVouchId: Long = 0L,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	var cloneData: CloneData? = null,
) {
	
	fun set(key: String, value: Any): Boolean {
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
	fun getAsString(key: String) = get(key) as String?
	fun getAsNumber(key: String) = get(key) as Number?
	fun getAsBoolean(key: String) = get(key) as Boolean?
	fun getAsMessageCreateData(key: String): MessageCreateData? {
		var value = get(key) ?: return null
		when (value) {
			is MessageCreateData -> return value
			is String -> {
				value = value.toMessageCreateData()
				set(key, value)
				return value
			}
			else -> throw IllegalStateException("Invalid data at key $key: $value")
		}
	}
	
}