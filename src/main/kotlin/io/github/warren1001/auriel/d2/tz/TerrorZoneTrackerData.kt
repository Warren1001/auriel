package io.github.warren1001.auriel.d2.tz

import java.time.Instant

data class TerrorZoneTrackerData(var _id: String = "default") {
	
	var senderChannelId: String? = null
	val guilds = mutableSetOf<String>()
	var lastUpdate: Instant = Instant.now()
	var lastTrust: Int = 0
	
}