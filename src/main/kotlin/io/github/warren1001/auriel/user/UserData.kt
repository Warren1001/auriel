package io.github.warren1001.auriel.user

import discord4j.common.util.Snowflake
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.async
import io.github.warren1001.auriel.d2.clone.CloneHelpData

data class UserData(val _id: Snowflake, val guildId: Snowflake) {
	
	var permission = Permission.EVERYONE
	val cloneData = CloneHelpData(_id)
	
	fun hasPermission(permission: Int): Boolean = this.permission >= permission
	
	@Deprecated("Use updateAsync(Auriel) instead", ReplaceWith("updateAsync(Auriel)"))
	fun update(auriel: Auriel) = auriel.getGuildManager(guildId).userDataManager.updateData(this)
	
	fun updateAsync(auriel: Auriel) {
		auriel.getGuildManager(guildId).userDataManager.updateData(this).async().subscribe()
	}
	
}
