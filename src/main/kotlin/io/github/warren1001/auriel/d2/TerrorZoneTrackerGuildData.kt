package io.github.warren1001.auriel.d2

data class TerrorZoneTrackerGuildData(var _id: String) {
	
	var roleIds: Map<TerrorZone, String>? = null
	var roleMentions: Map<TerrorZone, String>? = null
	var messageTemplate: String? = null
	var channelId: String? = null

}