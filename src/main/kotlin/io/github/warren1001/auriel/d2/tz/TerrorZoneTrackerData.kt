package io.github.warren1001.auriel.d2.tz

data class TerrorZoneTrackerData(var _id: String = "default") {
	
	var senderChannelId: String? = null
	val guilds = mutableSetOf<String>()
	var lastZone: Int = 0
	var lastTrust: Int = 0
	var lastMinuteIndex: Int = -1
	
}