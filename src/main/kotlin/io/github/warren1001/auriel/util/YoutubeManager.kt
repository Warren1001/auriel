package io.github.warren1001.auriel.util

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import discord4j.common.util.Snowflake
import io.github.warren1001.auriel.Auriel
import org.litote.kmongo.reactor.findOneById

class YoutubeManager(private val auriel: Auriel, key: String) {
	
	private val youtube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()) {}.setApplicationName("new-video-video-checker")
		.setYouTubeRequestInitializer(YouTubeRequestInitializer(key)).build()
	
	fun getYoutubeAnnouncer(guildId: Snowflake): YoutubeAnnouncer? =
		auriel.youtubeDataCollection.findOneById(guildId).blockOptional().map { YoutubeAnnouncer(auriel, guildId, it, youtube) }.orElse(null)
	
	fun createYoutubeAnnouncer(data: YoutubeData): YoutubeAnnouncer {
		return auriel.youtubeDataCollection.findOneById(data._id).blockOptional().map { YoutubeAnnouncer(auriel, data._id, it, youtube) }.orElseGet {
			auriel.updateYoutubeData(data)
			val announcer = YoutubeAnnouncer(auriel, data._id, data, youtube)
			return@orElseGet announcer
		}
	}
	
}