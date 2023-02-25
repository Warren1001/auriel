package io.github.warren1001.auriel.d2.tz

data class TerrorZoneTrackerGuildData(var _id: String) {
	
	var roleIds: Map<String, String>? = null
	var roleMentions: Map<String, String>? = null
	var messageTemplate: String? = null
	var channelId: String? = null

}