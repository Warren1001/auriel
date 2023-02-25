package io.github.warren1001.auriel.d2.tz

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.model.UpdateOptions
import io.github.warren1001.auriel.d2.D2
import io.github.warren1001.auriel.guild.Guilds
import io.github.warren1001.d2data.enums.D2Levels
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.litote.kmongo.updateOne
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TerrorZoneTracker(private val guilds: Guilds, val data: TerrorZoneTrackerData) {
	
	private val client: HttpClient = HttpClient.newHttpClient()
	private val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create("https://d2rapi.fly.dev/tz")).build()
	private val executors: ExecutorService = Executors.newSingleThreadExecutor()
	private val levelsStrings = loadJsonToMap("levels.json")
	private val levels = D2.files.loadSheet(D2Levels.SHEET_NAME)
	
	private var running = true
	private var automatedOffline = false
	
	init {
		executors.execute {
			while (running) {
				try {
					val doc = client.send(request, HttpResponse.BodyHandlers.ofString())
					val text = doc.body()
					val node = ObjectMapper().readTree(text)
					
					if (node.isArray && node.size() > 0) {
						if (automatedOffline) {
							automatedOffline = false
							data.guilds.forEach { guilds.getGuild(it).terrorZoneTrackerUpdate("Automated Terror Zone Tracker is online again.") }
						}
						val zoneIds = node.elements().asSequence()
							.map { it.asInt() }.toList()
						/*val zonesString = zoneIds
							.asSequence()
							.map { levels[it.toString(), D2Levels.ID, D2Levels.LEVEL_NAME] }
							.map { levelsStrings[it]!!["enUS"] }
							.distinct().toList().joinToString(", ")*/
						newInfo(zoneIds, 1000)
						/*val zoneId = node["zones"].fieldNames().next().toInt()
						val zone = TerrorZone.getTerrorZone(zoneId)
						if (zone == null) {
							guilds.auriel.warren("Invalid TZ info:\n$text")
						} else {
							println("Received TZ info from automatic: $zone")
							synchronized(guilds) {
								println("updating in sync..")
								newInfo(zone, 1000)
								println("finishing in sync..")
							}
						}*/
					} else if (!automatedOffline) {
						automatedOffline = true
						data.guilds.forEach {
							//guilds.getGuild(it).terrorZoneTrackerUpdate("Automated Terror Zone Tracker is offline, falling back to manual usage, please report TZs here until " +
							//	"the automated tracker is back online: https://terror-tracker.aodhagan.link")
							guilds.getGuild(it).terrorZoneTrackerUpdate("Automated Terror Zone Tracker is offline.")
						}
					}
				} catch (e: Exception) {
					guilds.auriel.warren(e.stackTraceToString())
				}
				Thread.sleep(1000L * (59 * 15 + 3 - Instant.now().atZone(ZoneId.systemDefault()).minute % 15))
			}
		}
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
		
		val zone = TerrorZone.valueOf(json["zone"].asText())
		val trust = json["trust"].asInt()
		
		println("Received TZ info from manual: $zone, $trust")
		
		synchronized(guilds) {
			println("updating in sync..")
			newInfo(zone, trust)
			println("finishing in sync..")
		}
		
		return true*/
	}
	
	fun stopTracker() {
		running = false
		executors.shutdownNow()
	}
	
	private fun newInfo(zoneIds: List<Int>, trust: Int) {
		val zoneIdsString = zoneIds.joinToString(",")
		if (zoneIdsString != data.lastZone) {
			//println("zone not the same as the last one, updating..")
			val minute = getCurrentMinuteIndex()
			if (minute == data.lastMinuteIndex && trust > data.lastTrust) {
				//println("minute is the same as the last one, and trust is higher, updating and deleting wrong one..")
				update(zoneIds, zoneIdsString, trust, minute, true)
			}
			else {
				//println("minute is not the same as the last one, or trust is lower, updating..")
				update(zoneIds, zoneIdsString, trust, minute, false)
			}
		}
	}
	
	private fun update(zoneIds: List<Int>, zoneIdsString: String, trust: Int, lastMinuteIndex: Int, deleteOld: Boolean) {
		data.lastZone = zoneIdsString
		data.lastTrust = trust
		data.lastMinuteIndex = lastMinuteIndex
		saveData()
		data.guilds.forEach { guilds.getGuild(it).onTerrorZoneChange(zoneIds, deleteOld) }
	}
	
	private fun getCurrentMinuteIndex() = LocalDateTime.now().minute % 15
	
}

fun loadJsonToMap(name: String) = TerrorZoneTracker::class.java.classLoader.getResource(name)!!
	.readText(Charsets.UTF_8)
	.replace("\uFEFF", "")
	.let { ObjectMapper().readTree(it) }
	.associate { it["Key"].asText() to it.fields().asSequence()
		.filter { it.key != "id" && it.key != "Key" }.map {
			it.key to it.value.asText().replace(Regex("\\(?%\\+[id]\\)?"), "+%s").replace(Regex("%\\+?[id]"), "%s")
				.replace("%3", "%4\$s").replace("%2", "%3\$s").replace("%1", "%2\$s").replace("%0", "%1\$s")
				.replace("%+3", "+%4\$s").replace("%+2", "+%3\$s").replace("%+1", "+%2\$s").replace("%+0", "+%1\$s")
		}.toMap()
	}