package io.github.warren1001.auriel.youtube

import com.google.api.services.youtube.YouTube
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.warren1001.auriel.Auriel
import kotlin.concurrent.timer

class YoutubeAnnouncer(private val auriel: Auriel, guildId: Snowflake, val data: YoutubeData, youtube: YouTube) {
	
	private val playlistItemsRequest: YouTube.PlaylistItems.List;
	private val playlistItemsRequestLimit: YouTube.PlaylistItems.List;
	
	private var channel: MessageChannel? = null
	private var early = false
	
	init {
		
		playlistItemsRequest = youtube.PlaylistItems().list(mutableListOf("snippet"))
		playlistItemsRequest.playlistId = data.playListId
		playlistItemsRequest.maxResults = 5
		
		playlistItemsRequestLimit = youtube.PlaylistItems().list(mutableListOf("snippet"))
		playlistItemsRequestLimit.playlistId = data.playListId
		playlistItemsRequestLimit.maxResults = 1
		
		auriel.gateway.getChannelById(data.channelId).cast(MessageChannel::class.java).subscribe {
			channel = it
			if (early) {
				checkForUpload()
				early = false
			}
		}
		
		timer("${guildId.asString()}-ytcheck", true, 0L, (1000 * 60 * 1).toLong()) { checkForUpload() }
		
	}
	
	private fun checkForUpload() {
		if (channel == null) {
			early = true
			return
		}
		val playlistItems = if (data.lastUpdate == 0L) playlistItemsRequestLimit.execute() else playlistItemsRequest.execute()
		playlistItems.items.filter { it.snippet.resourceId.kind == "youtube#video" && it.snippet.publishedAt.value > data.lastUpdate }
			.sortedWith(Comparator.comparingLong { it.snippet.publishedAt.value }).forEach {
				val videoId = it.snippet.resourceId.videoId
				val time = it.snippet.publishedAt.value
				val title = it.snippet.title
				updateLastUpdate(time)
				channel!!.createMessage(
					data.message.replace("%TITLE%", title).replace("%LINK%", "https://www.youtube.com/watch?v=$videoId")
						.replace("%URL%", "https://www.youtube.com/watch?v=$videoId")
				).subscribe()
			}
		
	}
	
	private fun updateLastUpdate(time: Long) {
		data.lastUpdate = time
		auriel.updateYoutubeData(data)
	}
	
}