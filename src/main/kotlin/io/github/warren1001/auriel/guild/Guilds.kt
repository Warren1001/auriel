package io.github.warren1001.auriel.guild

import com.mongodb.client.MongoCollection
import io.github.warren1001.auriel.Auriel
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class Guilds(private val auriel: Auriel) {
	
	private val guilds = mutableMapOf<String, AGuild>()
	
	val guildDataCollection: MongoCollection<AGuildData> = auriel.database.getCollection("guilds", AGuildData::class.java)

	init {
		guildDataCollection.find().forEach { guilds[it._id] = AGuild(auriel, it._id, it) }
		auriel.jda.guilds.filter { !guilds.contains(it.id) }.map { AGuild(auriel, it.id, this) }.forEach { guilds[it.id] = it }
	}
	
	fun handleMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot || !event.channelType.isGuild) {
			if (event.channelType == ChannelType.NEWS) event.message.crosspost().queue()
			return
		}
		val guild = guilds[event.guild.id] ?: return
		guild.handleMessageReceived(event)
	}
	
	fun getGuild(id: String): AGuild? = guilds[id]

}