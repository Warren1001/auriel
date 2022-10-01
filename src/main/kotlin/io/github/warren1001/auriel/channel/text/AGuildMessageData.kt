package io.github.warren1001.auriel.channel.text

import io.github.warren1001.auriel.util.filter.WordFilter

data class AGuildMessageData(val _id: String) {
	
	val wordFilters = mutableSetOf<WordFilter>()
	var onlyOneMessage = false
	var maxMessageAge = 0L
	var messageAgeInterval = 0L
	var allowBotReposts = true
	var lineLimit = 2000

}
