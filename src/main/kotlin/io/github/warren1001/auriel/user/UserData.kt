package io.github.warren1001.auriel.user

import discord4j.common.util.Snowflake
import io.github.warren1001.auriel.Auriel

data class UserData(val _id: Snowflake, private val guildId: Snowflake) {
	
	var permission = Permission.EVERYONE
	
	fun hasPermission(permission: Int): Boolean = this.permission - permission >= 0
	
	fun update(auriel: Auriel) = auriel.getGuildManager(guildId).userDataManager.updateData(this)
	
}
