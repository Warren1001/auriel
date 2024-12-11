package io.github.warren1001.auriel.d2.tz

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.model.UpdateOptions
import io.github.warren1001.auriel.d2.D2
import io.github.warren1001.auriel.guild.Guilds
import io.github.warren1001.d2data.enums.json.D2DesecratedZones
import io.github.warren1001.d2data.enums.sheet.D2LevelGroups
import io.github.warren1001.d2data.enums.sheet.D2Levels
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.litote.kmongo.updateOne
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TerrorZoneTracker(private val guilds: Guilds, val data: TerrorZoneTrackerData, emuToken: String) {
	
	private val client: HttpClient = HttpClient.newHttpClient()
	private val request: HttpRequest = HttpRequest.newBuilder().header("x-emu-username", "warren1001").header("x-emu-token", emuToken).uri(URI.create("https://www.d2emu.com/api/v1/tz")).build()
	private val executors: ExecutorService = Executors.newSingleThreadExecutor()
	val tzInfos: List<TerrorZoneInfo>
	
	private var running = true
	private var automatedOffline = false
	private var previousTerrorZoneInfo: TerrorZoneInfo?
	
	private var attempt = 0
	
	init {
		val tz = D2.files.loadJson(D2DesecratedZones.FILE_PATH)
		val levels = D2.files.loadSheet(D2Levels.FILE_PATH)
		val levelGroups = D2.files.loadSheet(D2LevelGroups.FILE_PATH)
		val levelsLang = D2.files.loadLang(io.github.warren1001.d2data.enums.lang.D2Levels.FILE_PATH)
		tzInfos = tz.root["desecrated_zones"][0]["zones"].asIterable().map {
			val id = it["id"].asInt()
			val zoneIds = it["levels"].asIterable().map { it["level_id"].asInt() }.toList()
			var act = -1
			val strings = zoneIds.map {
				if (act == -1) act = levels[it.toString(), D2Levels.ID, D2Levels.ACT].toInt() + 1
				val levelGroup = levels[it.toString(), D2Levels.ID, D2Levels.LEVEL_GROUP]
				val groupName = levelGroups[levelGroup, D2LevelGroups.GROUP_NAME]
				levelsLang[groupName]
			}.distinct()//.distinct().joinToString(", ")
			val string = strings[0].clone()
			for (i in 1 until strings.size) {
				string.append(strings[i], ", ")
			}
			TerrorZoneInfo(id, act, zoneIds, string)
		}
		//println("data.lastZone: ${data.lastZone}")
		previousTerrorZoneInfo = getInfoFromZoneId(data.lastZone)
		//println("previousTerrorZoneInfo: $previousTerrorZoneInfo")
	}
	
	fun getInfoFromZoneIds(zoneIds: List<Int>): TerrorZoneInfo? {
		return tzInfos.firstOrNull { it.zoneIds.containsAll(zoneIds) }
	}
	
	fun getInfoFromZoneId(id: Int): TerrorZoneInfo? {
		return tzInfos.firstOrNull { it.id == id }
	}
	
	fun saveData() = guilds.tzTrackerCollection.updateOne(data, options = UpdateOptions().upsert(true))
	
	fun setChannel(id: String, save: Boolean = true): Boolean {
		if (data.senderChannelId == id) return false
		data.senderChannelId = id
		if (save) saveData()
		return true
	}
	
	fun addGuild(id: String) {
		data.guilds.add(id)
		saveData()
	}
	
	fun removeGuild(id: String) {
		data.guilds.remove(id)
		saveData()
	}
	
	fun handle(event: MessageReceivedEvent): Boolean {
		return false
		/*if (event.channel.id != data.senderChannelId) return false
		val content = event.message.contentRaw
		if (!content.startsWith("{")) return false
		
		val json = ObjectMapper().readTree(content)
		
		val tzInfo = getInfoFromZoneIds(json["zoneIds"].elements().asSequence().map { it.asInt() }.toList())
		//val zone = TerrorZone.valueOf(json["zone"].asText())
		val trust = json["trust"].asInt()
		
		if (tzInfo == null) {
			event.channel.sendMessage("Invalid TZ info:\n$content").queue()
			return true
		}
		
		//println("Received TZ info from manual: $tzInfo, $trust")
		
		synchronized(guilds) {
			//println("updating in sync..")
			newInfo(trust, tzInfo)
			//println("finishing in sync..")
		}
		
		return true*/
	}
	
	/*fun getTimeToSleep(): Long {
		val now = LocalDateTime.now()
		val nextNormalAttempt = 1000L * ((60 - now.minute) * 60 - now.second + 10)
		if (attempt < 3) {
			return min(1000L * 5, nextNormalAttempt)
		}
		if (attempt < 8) {
			return min(1000L * 30, nextNormalAttempt)
		}
		if (attempt < 10) {
			return min(1000L * 60, nextNormalAttempt)
		}
		if (attempt < 15) {
			return min(1000L * 60 * 5, nextNormalAttempt)
		}
		attempt = 0
		return nextNormalAttempt
	}*/
	
	fun startTracker() {
		executors.execute {
			running = true
			while (running) {
				//println("checking for tz..")
				try {
					val doc = client.send(request, HttpResponse.BodyHandlers.ofString())
					val text = doc.body()
					val node = ObjectMapper().readTree(text)
					
					if (node.has("current")) {
						val currentZones = node["current"].elements().asSequence().map { it.asInt() }.toList()
						val nextZones = node["next"].elements().asSequence().map { it.asInt() }.toList()
						val nextUpdate : Long = node["next_terror_time_utc"].asLong() * 1000
						//println("nextUpdate: $nextUpdate, current: ${System.currentTimeMillis()}, diff=${nextUpdate - System.currentTimeMillis()}")
						val currentTerrorZoneInfo = getInfoFromZoneIds(currentZones)
						val nextTerrorZoneInfo = getInfoFromZoneIds(nextZones)
						if (currentTerrorZoneInfo == null) {
							attempt = 0
							guilds.auriel.warren("Invalid current TZ info:\n$text")
							Thread.sleep(nextUpdate - System.currentTimeMillis())
							continue
						} else if (nextTerrorZoneInfo == null) {
							attempt = 0
							guilds.auriel.warren("Invalid next TZ info:\n$text")
							Thread.sleep(nextUpdate - System.currentTimeMillis())
							continue
						} else {
							//println("previousTerrorZoneInfo: $previousTerrorZoneInfo, currentTerrorZoneInfo: $currentTerrorZoneInfo")
							if (currentTerrorZoneInfo == previousTerrorZoneInfo) {
								if (System.currentTimeMillis() < nextUpdate) {
									attempt = 0
									Thread.sleep(nextUpdate - System.currentTimeMillis())
									continue
								}
							}
							else {
								//println("Received TZ info from automatic: $tzInfo")
								attempt = 0
								//synchronized(guilds) {
								//println("updating in sync..")
								newInfo(1000, currentTerrorZoneInfo, nextTerrorZoneInfo)
								//println("finishing in sync..")
								//}
								Thread.sleep(nextUpdate - System.currentTimeMillis())
								continue
							}
						}
					}
					attempt++
					//println("Attempt $attempt: ")
					if (attempt <= 10) {
						Thread.sleep(1000 * 1)
					} else if (attempt <= 15) {
						Thread.sleep(1000 * 5)
					} else if (attempt <= 20) {
						Thread.sleep(1000 * 30)
					} else {
						if (!automatedOffline) {
							automatedOffline = true
							data.guilds.forEach {
								guilds.getGuild(it).terrorZoneTrackerUpdate(TerrorZoneTrackerStatus.OFFLINE)
							}
						}
						Thread.sleep(1000 * 60 * 5)
					}
				} catch (e: Exception) {
					guilds.auriel.warren(e.stackTraceToString())
					Thread.sleep(1000 * 60 * 5)
				}
			}
		}
	}
	
	fun stopTracker() {
		running = false
		executors.shutdownNow()
	}
	
	private fun newInfo(trust: Int, currentTerrorZoneInfo: TerrorZoneInfo, nextTerrorZoneInfo: TerrorZoneInfo? = null) {
		//if (data.lastZone != currentTerrorZoneInfo.id) {
			//println("zone not the same as the last one, updating..")
			val minute = getCurrentMinuteIndex()
			//if (minute == data.lastMinuteIndex && trust > data.lastTrust) {
				//println("minute is the same as the last one, and trust is higher, updating and deleting wrong one..")
				//update(trust, minute, true, currentTerrorZoneInfo, nextTerrorZoneInfo)
			//}
			//else {
				//println("minute is not the same as the last one, or trust is lower, updating..")
				update(trust, minute, false, currentTerrorZoneInfo, nextTerrorZoneInfo)
			//}
		//}
	}
	
	private fun update(trust: Int, lastMinuteIndex: Int, deleteOld: Boolean, currentTerrorZoneInfo: TerrorZoneInfo, nextTerrorZoneInfo: TerrorZoneInfo? = null) {
		if (automatedOffline) {
			automatedOffline = false
			data.guilds.forEach { guilds.getGuild(it).terrorZoneTrackerUpdate(TerrorZoneTrackerStatus.ONLINE) }
		}
		data.lastZone = currentTerrorZoneInfo.id
		previousTerrorZoneInfo = currentTerrorZoneInfo
		data.lastTrust = trust
		data.lastMinuteIndex = lastMinuteIndex
		saveData()
		data.guilds.forEach { guilds.getGuild(it).onTerrorZoneChange(deleteOld, currentTerrorZoneInfo, nextTerrorZoneInfo) }
	}
	
	private fun getCurrentMinuteIndex() = Math.floorDivExact(LocalDateTime.now().minute + 1, 15)
	
}
