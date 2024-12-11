package io.github.warren1001.auriel.d2.tz

data class TerrorZoneTrackerGuildData(var _id: String) {
	
	var roleIds: Map<Int, String>? = null
	var roleMentions: Map<Int, String>? = null

}