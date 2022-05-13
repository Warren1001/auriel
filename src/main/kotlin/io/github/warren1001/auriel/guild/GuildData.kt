package io.github.warren1001.auriel.guild

import discord4j.common.util.Snowflake
import io.github.warren1001.auriel.util.Filter
import io.github.warren1001.auriel.util.YoutubeData

data class GuildData(val _id: Snowflake) {
	
	val filters = mutableSetOf<Filter>()
	var muteRoleId: Snowflake? = null
	var logChannelId: Snowflake? = null
	var youtubeData: YoutubeData? = null
	
}