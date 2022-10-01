package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.util.filter.Filter
import io.github.warren1001.auriel.util.filter.WordFilter
import io.github.warren1001.auriel.util.youtube.YoutubeData

data class AGuildData(val _id: String) {
	
	val wordFilters = mutableSetOf<WordFilter>()
	val spamFilters = mutableSetOf<Filter>()
	var logChannelId: String? = null
	var youtubeData: YoutubeData? = null
	
}