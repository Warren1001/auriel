package io.github.warren1001.auriel.user

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.CloneHelpData

data class UserData(val _id: Snowflake, val guildId: Snowflake) {
	
	var permission = Permission.EVERYONE
	val cloneData = CloneHelpData(_id)
	
	fun hasPermission(permission: Int): Boolean = this.permission - permission >= 0
	
	fun update(auriel: Auriel) = auriel.getGuildManager(guildId).userDataManager.updateData(this)
	
	fun getUser(auriel: Auriel): User = auriel.gateway.getMemberById(guildId, _id).block()!!
	
}
