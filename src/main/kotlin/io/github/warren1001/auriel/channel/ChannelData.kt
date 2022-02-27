package io.github.warren1001.auriel.channel

import discord4j.common.util.Snowflake
import io.github.warren1001.auriel.util.Filter

data class ChannelData(val _id: Snowflake, val guildId: Snowflake) {
	
	var onlyOneMessage = false
	var maxMessageAge = 0L
	var messageAgeInterval = 0L
	var allowBotReposts = true
	var lineLimit = 2000
	val filters = mutableSetOf<Filter>()
	
}