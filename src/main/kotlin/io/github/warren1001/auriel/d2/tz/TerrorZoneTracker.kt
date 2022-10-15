package io.github.warren1001.auriel.d2.tz

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.model.UpdateOptions
import io.github.warren1001.auriel.guild.Guilds
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.litote.kmongo.updateOne
import java.time.Instant

class TerrorZoneTracker(private val guilds: Guilds, val data: TerrorZoneTrackerData) {
	
	fun saveData() = guilds.tzTrackerCollection.updateOne(data, options = UpdateOptions().upsert(true))
	
	fun setChannel(id: String) {
		data.senderChannelId = id
		saveData()
	}
	
	fun addGuild(id: String) {
		data.guilds.add(id)
		saveData()
	}
	
	fun removeGuild(id: String) {
		data.guilds.remove(id)
		saveData()
	}
	
	fun handle(event: MessageReceivedEvent): Boolean {
		if (event.channel.id != data.senderChannelId) return false
		val content = event.message.contentRaw
		if (!content.startsWith("{")) return false
		
		val json = ObjectMapper().readTree(content)
		
		//val ladder = json["ladder"].asText()
		val zone = TerrorZone.valueOf(json["zone"].asText())
		val utcTimestamp = Instant.parse(json["utcTimestamp"].asText())
		val trust = json["trust"].asInt()
		
		if (utcTimestamp == data.lastUpdate && trust <= data.lastTrust) return true
		
		data.guilds.forEach { guilds.getGuild(it).onTerrorZoneChange(zone) }
		
		data.lastUpdate = utcTimestamp
		data.lastTrust = trust
		saveData()
		
		return true
	}
	
}