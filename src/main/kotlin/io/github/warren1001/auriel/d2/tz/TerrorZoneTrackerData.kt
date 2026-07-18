package io.github.warren1001.auriel.d2.tz

data class TerrorZoneTrackerData(var _id: String = "default") {
	
	val guilds = mutableSetOf<String>()
	var nextTerrorTime = 0L
	
}