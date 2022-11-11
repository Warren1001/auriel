package io.github.warren1001.auriel.user

import com.mongodb.client.MongoCollection
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.guild.AGuild

class Users(private val auriel: Auriel, val guild: AGuild) {
	
	private val users = mutableMapOf<String, AUser>()
	val userDataCollection: MongoCollection<AUserData> = auriel.database.getCollection("${guild.id}-users", AUserData::class.java)
	
	fun getUser(id: String) = users.computeIfAbsent(id) { AUser(auriel, it, this) }
	
}