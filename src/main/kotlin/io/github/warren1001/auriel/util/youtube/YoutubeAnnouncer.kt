package io.github.warren1001.auriel.util.youtube

import com.google.api.services.youtube.YouTube
import dev.minn.jda.ktx.generics.getChannel
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.guild.AGuild
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.*
import kotlin.concurrent.timer

class YoutubeAnnouncer(private val auriel: Auriel, private val guild: AGuild, private val data: YoutubeData, youtube: YouTube) {
	
	private val playlistItemsRequest = youtube.PlaylistItems().list(mutableListOf("snippet"))
	
	private var timer: Timer? = null
	
	init {
		playlistItemsRequest.playlistId = data.playListId
		playlistItemsRequest.maxResults = 1
	}
	
	private fun checkForUpload() {
		try {
			playlistItemsRequest.execute().items.filter { it.snippet.resourceId.kind == "youtube#video" && it.snippet.publishedAt.value > data.lastUpdate }
				.sortedWith(Comparator.comparingLong { it.snippet.publishedAt.value }).forEach {
					val videoId = it.snippet.resourceId.videoId
					val time = it.snippet.publishedAt.value
					val title = it.snippet.title
					updateLastUpdate(time)
					auriel.jda.getChannel<GuildMessageChannel>(data.channelId!!)!!.sendMessage(
						data.message.replace("%TITLE%", title).replace("%LINK%", "https://www.youtube.com/watch?v=$videoId")
							.replace("%URL%", "https://www.youtube.com/watch?v=$videoId")
					).queue_()
				}
		} catch (e: Exception) {
			auriel.warren("Error checking for new uploads: ${e.stackTraceToString()}")
		}
	}
	
	private fun updateLastUpdate(time: Long) {
		data.lastUpdate = time
		guild.saveData()
	}
	
	fun setChannelId(channelId: String): Boolean {
		if (data.channelId == channelId) return false
		data.channelId = channelId
		return true
	}
	
	fun setPlaylistId(playlistId: String): Boolean {
		if (data.playListId == playlistId) return false
		playlistItemsRequest.playlistId = playlistId
		data.playListId = playlistId
		return true
	}
	
	fun setMessage(message: String): Boolean {
		if (data.message == message) return false
		data.message = message
		return true
	}
	
	fun start(): Boolean {
		if (timer != null) return true
		if (data.playListId == null || data.channelId == null) return false
		timer = timer("ytAnnouncer-${guild.id}", true, 1000 * 60 * 1, 1000 * 60 * 1) { checkForUpload() }
		return true
	}
	
	fun stop() {
		timer?.cancel()
	}
	
}