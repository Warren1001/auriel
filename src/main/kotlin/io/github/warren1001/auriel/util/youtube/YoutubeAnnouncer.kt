package io.github.warren1001.auriel.util.youtube

import com.google.api.services.youtube.YouTube
import dev.minn.jda.ktx.generics.getChannel
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.dm
import io.github.warren1001.auriel.guild.AGuild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.*
import kotlin.concurrent.timer

class YoutubeAnnouncer(private val auriel: Auriel, private val guild: AGuild, youtube: YouTube) {
	
	private val data = guild.data.youtubeData!!
	private val playlistItemsRequest: YouTube.PlaylistItems.List
	
	private val timer: Timer
	
	init {
		
		playlistItemsRequest = youtube.PlaylistItems().list(mutableListOf("snippet"))
		playlistItemsRequest.playlistId = data.playListId
		playlistItemsRequest.maxResults = 1
		
		timer = timer("${guild.id}-ytcheck", true, 0L, (1000 * 60 * 1).toLong()) { checkForUpload() }
		
	}
	
	private fun checkForUpload() {
		try {
			playlistItemsRequest.execute().items.filter { it.snippet.resourceId.kind == "youtube#video" && it.snippet.publishedAt.value > data.lastUpdate }
				.sortedWith(Comparator.comparingLong { it.snippet.publishedAt.value }).forEach {
					val videoId = it.snippet.resourceId.videoId
					val time = it.snippet.publishedAt.value
					val title = it.snippet.title
					updateLastUpdate(time)
					auriel.jda.getChannel<GuildMessageChannel>(data.channelId)!!.sendMessage(
						data.message.replace("%TITLE%", title).replace("%LINK%", "https://www.youtube.com/watch?v=$videoId")
							.replace("%URL%", "https://www.youtube.com/watch?v=$videoId")
					).queue()
				}
		} catch (e: Exception) {
			auriel.warren().dm("Error checking for new uploads: ${e.message}\n${e.stackTrace.joinToString("\n")}").queue()
		}
	}
	
	private fun updateLastUpdate(time: Long) {
		data.lastUpdate = time
		guild.saveData()
	}
	
	fun stop() {
		timer.cancel()
	}
	
}