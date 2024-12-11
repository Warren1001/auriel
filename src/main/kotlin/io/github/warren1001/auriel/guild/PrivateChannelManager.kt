package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

class PrivateChannelManager(private val guild: AGuild) {
	
	private val privateChannels = mutableMapOf<String, PrivateChannelData>()
	
	init {
		guild.data.privateChannelData.values.forEach {
			privateChannels[it.creatorId] = it
		}
	}

	fun createPrivateChannel(creator: Member, otherUsers: List<Member>, reason: String, tagAll: Boolean, callback: (TextChannel) -> Unit = {}): Boolean {
		if (!guild.data.has("guild:private-channel-category")) return false
		guild.jda().getCategoryById(guild.data.getAsString("guild:private-channel-category"))!!.createTextChannel(creator.id).queue_ { channel ->
			channel.upsertPermissionOverride(creator).grant(Permission.VIEW_CHANNEL).queue_()
			otherUsers.forEach { channel.upsertPermissionOverride(it).grant(Permission.VIEW_CHANNEL).queue_() }
			val mentionsString = if (tagAll) "@here" else otherUsers.joinToString(" ") { it.asMention }
			channel.sendMessage("$mentionsString, ${creator.asMention} created this channel because $reason").queue_()
			callback(channel)
			val data = PrivateChannelData(channel.id, -1, creator.id)
			guild.data.privateChannelData[channel.id] = data
			privateChannels[creator.id] = data
			guild.saveData()
		}
		return true
	}
	
	fun close(channelId: String): Boolean {
		if (!guild.data.privateChannelData.containsKey(channelId)) return false
		guild.jda().getTextChannelById(channelId)?.delete()?.queue_()
		val data = guild.data.privateChannelData.remove(channelId)
		privateChannels.remove(data!!.creatorId)
		guild.saveData()
		return true
	}

}