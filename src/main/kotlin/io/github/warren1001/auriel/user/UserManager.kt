package io.github.warren1001.auriel.user

import discord4j.common.util.Snowflake
import io.github.warren1001.auriel.Auriel

class UserManager(private val auriel: Auriel) {
	
	private val users = mutableMapOf<Snowflake, String>()
	
	fun getUserPrivateData(id: Snowflake): String? = users[id]
	
	fun setUserPrivateData(id: Snowflake, data: String) {
		users[id] = data
	}
	
	fun clearUserPrivateData(id: Snowflake) {
		users.remove(id)
	}
	
}