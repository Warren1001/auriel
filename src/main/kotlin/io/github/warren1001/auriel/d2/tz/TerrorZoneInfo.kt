package io.github.warren1001.auriel.d2.tz

import io.github.warren1001.d2data.lang.MultiLangString

data class TerrorZoneInfo(val id: Int, val act: Int, val zoneIds: List<Int>, val string: MultiLangString) {
	
	override fun toString() = "TerrorZoneInfo[id=$id, act=$act, zoneIds=$zoneIds, string='$string']"
	
}