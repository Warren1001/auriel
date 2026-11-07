package io.github.warren1001.auriel.d2.tz

data class TerrorZoneTrackerData(
	var _id: String = "default",
	val guilds: MutableSet<String> = mutableSetOf(),
	var nextTerrorTime: Long = 0L
)