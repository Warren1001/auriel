package io.github.warren1001.auriel.user

import discord4j.common.util.Snowflake

data class ModerationData(val id: Snowflake, val guildId: Snowflake) {
	
	var swears: Int = 0
	val warnings: List<ModerationInfo> = mutableListOf()
	val kicks: List<ModerationInfo> = mutableListOf()
	val bans: List<ModerationInfo> = mutableListOf()
	val notes: List<ModerationInfo> = mutableListOf()
	
}