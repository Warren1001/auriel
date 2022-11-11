package io.github.warren1001.auriel.guild

import com.mongodb.client.MongoCollection
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.d2.tz.TerrorZoneTracker
import io.github.warren1001.auriel.d2.tz.TerrorZoneTrackerData
import io.github.warren1001.auriel.d2.tz.TerrorZoneTrackerGuildData
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.litote.kmongo.findOneById

class Guilds(private val auriel: Auriel) {
	
	private val guilds = mutableMapOf<String, AGuild>()
	
	val guildDataCollection: MongoCollection<AGuildData> = auriel.database.getCollection("guilds", AGuildData::class.java)
	val tzTrackerCollection: MongoCollection<TerrorZoneTrackerData> = auriel.database.getCollection("tztrackerdata", TerrorZoneTrackerData::class.java)
	val tzTrackerRoleCollection: MongoCollection<TerrorZoneTrackerGuildData> = auriel.database.getCollection("tztrackerroledata", TerrorZoneTrackerGuildData::class.java)
	val tzTracker: TerrorZoneTracker
	val guildDataDefaults = mutableMapOf<String, Any>()
	val guildMessageChannelDataDefaults = mutableMapOf<String, Any>()

	init {
		guildDataCollection.find().forEach { guilds[it._id] = AGuild(auriel, it._id, this, it) }
		auriel.jda.guilds.filter { !guilds.contains(it.id) }.map { AGuild(auriel, it.id, this) }.forEach { guilds[it.id] = it }
		
		var data = tzTrackerCollection.findOneById("default")
		if (data == null) {
			data = TerrorZoneTrackerData("default")
			tzTrackerCollection.insertOne(data)
		}
		tzTracker = TerrorZoneTracker(this, data)
	}
	
	fun handleMessageReceived(event: MessageReceivedEvent): Boolean {
		if (!event.channelType.isGuild) return false
		if (tzTracker.handle(event)) return false
		if (event.author.isBot) {
			if (event.author.id == auriel.jda.selfUser.id && event.channelType == ChannelType.NEWS) {
				event.message.crosspost().queue_()
			}
			return false
		}
		val guild = guilds[event.guild.id] ?: return false
		return guild.handleMessageReceived(event)
	}
	
	fun getGuild(id: String): AGuild = guilds.computeIfAbsent(id) { AGuild(auriel, it, this) }
	
	fun forEachGuild(block: (AGuild) -> Unit) = guilds.values.forEach(block)
	
	fun handleSelectMenuInteraction(event: SelectMenuInteractionEvent) {
		if (!event.isFromGuild) return
		val guild = guilds[event.guild!!.id] ?: return
		guild.handleSelectMenuInteraction(event)
	}

}