package io.github.warren1001.auriel.d2.tz

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.mongodb.client.model.UpdateOptions
import io.github.warren1001.auriel.d2.D2
import io.github.warren1001.auriel.guild.Guilds
import io.github.warren1001.d2data.enums.json.D2DesecratedZones
import io.github.warren1001.d2data.enums.sheet.D2LevelGroups
import io.github.warren1001.d2data.enums.sheet.D2Levels
import org.litote.kmongo.updateOne
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TerrorZoneTracker(private val guilds: Guilds, val data: TerrorZoneTrackerData, emuToken: String) {
	
	private val client: HttpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
	private val request: HttpRequest = HttpRequest.newBuilder().header("x-emu-username", "warren1001").header("x-emu-token", emuToken).uri(URI.create("https://d2emu.com/api/v1/tz")).build()
	private val executors: ExecutorService = Executors.newSingleThreadExecutor()
	val tzInfos: Map<String, TerrorZoneInfo>
	
	private var running = true
	//private var automatedOffline = false
	//private var previousTerrorZoneInfo: TerrorZoneInfo?
	
	private var attempt = 0
	private var sameNotified = false
	
	init {
		val tz = D2.files.loadJson(D2DesecratedZones.FILE_PATH)
		val levels = D2.files.loadSheet(D2Levels.FILE_PATH)
		val levelGroups = D2.files.loadSheet(D2LevelGroups.FILE_PATH)
		val levelsLang = D2.files.loadLang(io.github.warren1001.d2data.enums.lang.D2Levels.FILE_PATH)
		tzInfos = tz.root["desecrated_zones"][0]["zones"].asIterable().map {
			val id = it["id"].asText()
			val zoneIds = it["levels"].asIterable().map { it["level_id"].asInt() }.toList()
			val strings = zoneIds.filter { it != 0 }.map {
				val levelGroup = levels[it.toString(), D2Levels.ID, D2Levels.LEVEL_GROUP]
				val groupName = levelGroups[levelGroup, D2LevelGroups.NAME_STRING]
				if (groupName == "") {
					println("GroupName was empty.. it=$it, groupName=$groupName, levelGroup=$levelGroup")
				}
				levelsLang[groupName]
			}.distinct()
			val string = strings[0].clone()
			for (i in 1 until strings.size) {
				string.append(strings[i], ", ")
			}
			//println("it=$it")
			//println("id=$id, zoneIds=$zoneIds, strings=$strings")
			TerrorZoneInfo(id, zoneIds, string)
		}.associateBy { it.id }
		//println("tzInfos: $tzInfos")
		//println("data.lastZone: ${data.lastZone}")
		//previousTerrorZoneInfo = getInfoFromZoneId(data.lastZone)
		//println("previousTerrorZoneInfo: $previousTerrorZoneInfo")
	}
	
	fun getInfoFromZoneIds(zoneIds: List<Int>): TerrorZoneInfo? {
		tzInfos.forEach {
			//println("Checking if all of $zoneIds can be found in ${it.value.zoneIds}")
			if (it.value.zoneIds.containsAll(zoneIds)) {
				return it.value
			}
		}
		return null
	}
	
	fun saveData() = guilds.tzTrackerCollection.updateOne(data, options = UpdateOptions().upsert(true))
	
	/*fun setChannel(id: String, save: Boolean = true): Boolean {
		if (data.senderChannelId == id) return false
		data.senderChannelId = id
		if (save) saveData()
		return true
	}*/
	
	fun addGuild(id: String) {
		data.guilds.add(id)
		saveData()
	}
	
	fun removeGuild(id: String) {
		data.guilds.remove(id)
		saveData()
	}
	
	fun startTracker() {
		executors.execute {
			val mapper: ObjectMapper = JsonMapper.builder().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION).build()
			running = true
			while (running) {
				try {
					val doc = client.send(request, HttpResponse.BodyHandlers.ofString())
					val text = doc.body()
					//println(text)
					val node = mapper.readTree(text)
					if (node.has("current")) {
						val currentZones = node["current"].elements().asSequence().map { it.asInt() }.toList()
						val nextZones = node["next"].elements().asSequence().map { it.asInt() }.toList()
						val nextTerrorTime = node["next_terror_time_utc"].asLong() * 1000
						val delay = (node["delay"].asInt() + 3 + 5) * 1000
						val nextUpdate = nextTerrorTime + delay//(node["next_available_time_utc"].asLong() + 3) * 1000
						val currentTerrorZoneInfo = getInfoFromZoneIds(currentZones)
						val nextTerrorZoneInfo = getInfoFromZoneIds(nextZones)
						if (currentTerrorZoneInfo == null) {
							attempt = 0
							guilds.auriel.warren("Invalid current TZ info:\n$text")
							val sleep = nextUpdate - System.currentTimeMillis()
							if (sleep > 0) {
								Thread.sleep(sleep)
							} else {
								attempt++
							}
							continue
						} else if (nextTerrorZoneInfo == null) {
							attempt = 0
							guilds.auriel.warren("Invalid next TZ info:\n$text")
							val sleep = nextUpdate - System.currentTimeMillis()
							if (sleep > 0) {
								Thread.sleep(sleep)
							} else {
								attempt++
							}
							continue
						} else {
							if (nextTerrorTime <= data.nextTerrorTime) {
								if (System.currentTimeMillis() < nextUpdate) {
									Thread.sleep(nextUpdate - System.currentTimeMillis())
									attempt = 0
									continue
								} else if (!sameNotified) {
									//guilds.auriel.warren("Automation reporting the same or old? data after expected update")
									sameNotified = true
								}
							} else {
								update(nextTerrorTime, currentTerrorZoneInfo, nextTerrorZoneInfo)
								attempt = 0
								sameNotified = false
								val sleep = nextUpdate - System.currentTimeMillis()
								if (sleep > 0) {
									Thread.sleep(sleep)
								} else {
									attempt++
								}
								continue
							}
						}
					}
					attempt++
					if (attempt <= 5) {
						Thread.sleep(1000 * 5)
					} else if (attempt <= 10) {
						Thread.sleep(1000 * 60)
					} else if (attempt <= 15) {
						Thread.sleep(1000 * 60 * 5)
					} else {
						/*if (!automatedOffline) {
							automatedOffline = true
							data.guilds.forEach {
								guilds.getGuild(it).terrorZoneTrackerUpdate(TerrorZoneTrackerStatus.OFFLINE)
							}
						}*/
						Thread.sleep(1000 * 60 * 10)
					}
				} catch (_: InterruptedException) {
				} catch (e: Exception) {
					guilds.auriel.warren(e.stackTraceToString())
					try {
						Thread.sleep(1000 * 60 * 10)
					} catch (_: InterruptedException) {}
				}
			}
		}
	}
	
	fun stopTracker() {
		running = false
		executors.shutdownNow()
	}
	
	private fun update(nextTerrorTime: Long, currentTerrorZoneInfo: TerrorZoneInfo, nextTerrorZoneInfo: TerrorZoneInfo) {
		/*if (automatedOffline) {
			automatedOffline = false
			data.guilds.forEach { guilds.getGuild(it).terrorZoneTrackerUpdate(TerrorZoneTrackerStatus.ONLINE) }
		}*/
		data.nextTerrorTime = nextTerrorTime
		//previousTerrorZoneInfo = currentTerrorZoneInfo
		saveData()
		//if (attempt > 0) guilds.auriel.warren("found TZ info after $attempt attempts")
		data.guilds.forEach { guilds.getGuild(it).onTerrorZoneChange(currentTerrorZoneInfo, nextTerrorZoneInfo) }
	}
	
}
