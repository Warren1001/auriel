package io.github.warren1001.auriel.guild

import com.mongodb.client.MongoCollection
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.d2.TerrorZoneTracker
import io.github.warren1001.auriel.d2.TerrorZoneTrackerData
import io.github.warren1001.auriel.d2.TerrorZoneTrackerGuildData
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
	
	fun handleMessageReceived(event: MessageReceivedEvent) {
		if (!event.channelType.isGuild) return
		if (tzTracker.handle(event)) return
		if (event.author.isBot) {
			if (event.author.id == auriel.jda.selfUser.id && event.channelType == ChannelType.NEWS) {
				event.message.crosspost().queue()
			}
			return
		}
		val guild = guilds[event.guild.id] ?: return
		guild.handleMessageReceived(event)
	}
	
	fun getGuild(id: String): AGuild = guilds.computeIfAbsent(id) { AGuild(auriel, it, this) }
	
	fun handleSelectMenuInteraction(event: SelectMenuInteractionEvent) {
		if (!event.isFromGuild) return
		val guild = guilds[event.guild!!.id] ?: return
		guild.handleSelectMenuInteraction(event)
	}

}