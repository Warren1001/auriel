package io.github.warren1001.auriel.util

import discord4j.common.util.Snowflake

data class YoutubeData(val _id: Snowflake, val playListId: String, var roleId: Snowflake, var channelId: Snowflake, var roleMessageChannel: Snowflake) {
	
	var lastUpdate: Long = 0L
	var message: String = "%TITLE% %URL%"
	var roleMessageId: Snowflake? = null
	
}