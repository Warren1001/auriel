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
	
	private val playlistItemsRequest = youtube.PlaylistItems().list(listOf("snippet"))
	private val videosRequest = youtube.Videos().list(listOf("snippet"))
	
	private var timer: Timer? = null
	
	private val alreadyPosted: MutableSet<String> = mutableSetOf()
	
	init {
		playlistItemsRequest.playlistId = data.playListId
		playlistItemsRequest.maxResults = 2
		videosRequest.maxResults = 1
		data.lastVideoId?.let { alreadyPosted.add(it) }
	}
	
	private fun checkForUpload() {
		try {
			playlistItemsRequest.execute().items.filter {
				//println("publishedAt: ${it.snippet.publishedAt.value} lastUpdate: ${data.lastUpdate}")
				it.snippet.resourceId.kind == "youtube#video" && it.snippet.publishedAt.value > data.lastUpdate
			}
				.map {
					val realTime = it.snippet.publishedAt.value
					videosRequest.setId(listOf(it.snippet.resourceId.videoId))
					Pair(videosRequest.execute().items[0], realTime)
				}.filter {
					return@filter if (it.first.snippet.liveBroadcastContent != "none") {
						if (!alreadyPosted.contains(it.first.id)) alreadyPosted.add(it.first.id)
						false
					}
					else !alreadyPosted.contains(it.first.id)
				}
				.sortedWith(Comparator.comparingLong { it.second }).forEach {
					val video = it.first
					val videoId = video.id
					val time = it.second
					println("videoId: $videoId time: $time")
					val title = video.snippet.title
					updateLastPost(time, videoId)
					auriel.jda.getChannel<GuildMessageChannel>(data.channelId!!)!!.sendMessage(
						data.message.replace("%TITLE%", title).replace("%LINK%", "https://www.youtube.com/watch?v=$videoId")
							.replace("%URL%", "https://www.youtube.com/watch?v=$videoId")
					).queue_()
				}
		} catch (e: Exception) {
			auriel.warren("Error checking for new uploads: ${e.stackTraceToString()}")
		}
	}
	
	private fun updateLastPost(time: Long, videoId: String) {
		data.lastUpdate = time
		data.lastVideoId = videoId
		alreadyPosted.add(videoId)
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
		timer = timer("ytAnnouncer-${guild.id}", true, 0, 1000 * 60 * 1) { checkForUpload() }
		return true
	}
	
	fun stop() {
		timer?.cancel()
	}
	
}