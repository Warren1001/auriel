package io.github.warren1001.auriel.util.youtube

data class YoutubeData(val playListId: String, var channelId: String, var message: String = "%TITLE% %URL%") {
	
	var lastUpdate: Long = 0L
	
}