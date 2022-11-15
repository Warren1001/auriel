package io.github.warren1001.auriel.user

import com.mongodb.client.model.UpdateOptions
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.a
import io.github.warren1001.auriel.guild.AGuild
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import org.litote.kmongo.findOneById
import org.litote.kmongo.updateOne

class AUser {
	
	private val auriel: Auriel
	private val users: Users
	val id: String
	val guild: AGuild
	
	val data: AUserData
	
	constructor(auriel: Auriel, id: String, users: Users) {
		this.auriel = auriel
		this.id = id
		this.users = users
		this.guild = users.guild
		this.data = users.userDataCollection.findOneById(id) ?: AUserData(id, guild.data.userDefaults)
		this.data.verifyDefaults(guild.data.userDefaults)
	}
	
	constructor(auriel: Auriel, id: String, users: Users, data: AUserData) {
		this.auriel = auriel
		this.id = id
		this.users = users
		this.guild = users.guild
		this.data = data
	}
	
	fun saveData() = users.userDataCollection.updateOne(data, options = UpdateOptions().upsert(true))
	
	fun giveVouch(member: Member, reason: String): Boolean {
		val giver = member.a()
		val vouchCooldown = guild.data.getAsNumber("guild:vouch-cooldown").toLong()
		if (!member.hasPermission(Permission.BAN_MEMBERS) && System.currentTimeMillis() - giver.data.lastVouch < vouchCooldown * 1000) return false
		data.vouches.add(Vouch(guild.data.nextVouchId++, giver.id, reason, System.currentTimeMillis()))
		giver.data.lastVouch = System.currentTimeMillis()
		guild.saveData()
		saveData()
		giver.saveData()
		return true
	}
	
}