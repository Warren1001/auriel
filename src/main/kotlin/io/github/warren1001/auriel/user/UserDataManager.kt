package io.github.warren1001.auriel.user

import com.mongodb.client.model.UpdateOptions
import discord4j.common.util.Snowflake
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.WARREN_ID
import org.litote.kmongo.reactor.findOneById
import org.litote.kmongo.reactor.updateOne
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

class UserDataManager(auriel: Auriel, private val guildId: Snowflake) {
	
	private val collection = auriel.database.getCollection("${guildId.asString()}-users", UserData::class.java)
	
	fun getData(id: Snowflake) = collection.findOneById(id).switchIfEmpty { Mono.just(getDefaultData(id)) }
	
	fun updateData(userData: UserData) = collection.updateOne(userData, UpdateOptions().upsert(true))
	
	private fun getDefaultData(id: Snowflake): UserData {
		val data = UserData(id, guildId)
		if (id == WARREN_ID) {
			data.permission = Permission.OWNER
		}
		return data
	}
	
}