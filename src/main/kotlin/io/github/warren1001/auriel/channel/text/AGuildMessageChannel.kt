package io.github.warren1001.auriel.channel.text

import com.mongodb.client.model.UpdateOptions
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.guild.AGuild
import io.github.warren1001.auriel.util.filter.WordFilter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageHistory
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.utils.TimeUtil
import org.litote.kmongo.updateOne
import java.util.*
import kotlin.concurrent.timer

class AGuildMessageChannel {
	
	private val auriel: Auriel
	private val guild: AGuild
	private val id: String
	
	val data: AGuildMessageChannelData
	
	private val lastMemberMessage = mutableMapOf<User, Message>()
	private var messageAgeTimer: Timer? = null
	
	constructor(auriel: Auriel, guild: AGuild, id: String) {
		this.auriel = auriel
		this.guild = guild
		this.id = id
		data = AGuildMessageChannelData(id, auriel.guilds.guildMessageChannelDataDefaults.toMutableMap())
		setup()
	}
	
	constructor(auriel: Auriel, guild: AGuild, id: String, data: AGuildMessageChannelData) {
		this.auriel = auriel
		this.guild = guild
		this.id = id
		this.data = data
		setup()
	}
	
	private fun setup() {
		startMessageAgeTimer()
		deleteAllButOneMessage()
	}
	
	fun saveData() = guild.textChannelDataCollection.updateOne(data, options = UpdateOptions().upsert(true))
	
	fun handleMessageReceived(event: MessageReceivedEvent): Boolean {
		// cant be a bot and will always be a guild channel
		
		val author = event.member!!
		val channel = event.channel.asGuildMessageChannel()
		
		if (!author.hasPermission(Permission.BAN_MEMBERS)) {
			
			var message = event.message.contentRaw
			
			// word filters
			val filters = data.wordFilters.filter { it.containsMatchIn(message) }
			if (filters.isNotEmpty()) {
				val blockedPhrases = filters.joinToString(" ") { it.name }
				event.message.delete().queue_()
				var repostId: Long? = null
				if (filters.all { it.shouldReplace() } && allowsReposting()) {
					filters.forEach { message = it.replace(message) }
					repostId = event.channel.message("${event.author.asMention} said (filtered): $message}").complete().idLong
				} else {
					event.author.dm("Your message was deleted from ${channel.asMention} in **${event.guild.name}** because it contained the following blocked phrase(s): **$blockedPhrases**\n" +
							"Here's your message incase you didn't save it:")
					event.author.dm(message.quote())
				}
				guild.logMessageDelete(author, event.channel.asGuildMessageChannel(), "Blocked phrase(s): $blockedPhrases", event.message.contentRaw, repostId)
				return true
			}
			
			val lineLimit = data.getAsNumber("channel:line-limit").toInt()
			if (message.split('\n').size > lineLimit) {
				event.message.delete().queue_()
				event.author.dm("Your message was deleted from ${channel.asMention} in **${event.guild.name}** because it contained too many lines (limit of **$lineLimit**).\n" +
						"Here's your message incase you didn't save it:")
				event.author.dm(message.quote())
				guild.logMessageDelete(author, event.channel.asGuildMessageChannel(), "Exceeded line limit of $lineLimit", message)
				return true
			}
			
			// only one message
			if (data.getAsBoolean("channel:only-one-message")) {
				val lastMessage = lastMemberMessage[author.user]
				lastMessage?.delete()?.queueDelete()
				lastMemberMessage[author.user] = event.message
			}
			
		}
		return false
	}
	
	fun startMessageAgeTimer(): Boolean {
		messageAgeTimer?.cancel()
		val maxMessageAge = data.getAsNumber("channel:max-message-age").toLong()
		val messageAgeInterval = data.getAsNumber("channel:message-age-interval").toLong()
		if (maxMessageAge > 0L && messageAgeInterval > 0L) {
			val channel = auriel.jda.getTextChannelById(id)
			if (channel == null) {
				auriel.warren("Channel $id not found when trying to start message age timer")
			} else {
				messageAgeTimer = timer("$id-messageAge", true, 60 * 1000, messageAgeInterval) {
					try {
						MessageHistory.getHistoryBefore(channel, TimeUtil.getDiscordTimestamp(System.currentTimeMillis() - maxMessageAge).toString())
							.queue_ { it.channel.asGuildMessageChannel().purgeMessages(it.retrievedHistory.filter { it.member != null && !it.member!!.hasPermission(Permission.BAN_MEMBERS) }) }
					} catch (e: Exception) {
						auriel.warren(e.stackTraceToString())
					}
				}
				return true
			}
		}
		return false
	}
	
	fun stopMessageAgeTimer() {
		messageAgeTimer?.cancel()
		messageAgeTimer = null
	}
	
	fun deleteAllButOneMessage() {
		if (data.getAsBoolean("channel:only-one-message")) {
			var channel = auriel.jda.getTextChannelById(id)
			if (channel == null) {
				auriel.warren("Channel $id not found when trying to delete all but one message")
			} else {
				MessageHistory.getHistoryFromBeginning(channel).queue_ { history ->
					history.channel.asTextChannel().purgeMessages(history.retrievedHistory.filter {
						if (it.author.isBot || (it.member != null && it.member!!.hasPermission(Permission.BAN_MEMBERS))) {
							false
						} else if (lastMemberMessage.contains(it.author)) {
							true
						} else {
							lastMemberMessage[it.author] = it
							false
						}
					})
				}
			}
		}
	}
	
	fun allowsReposting() = data.getAsBoolean("channel:allow-reposts")
	
	fun addWordFilter(name: String, pattern: String, replace: String? = null, literal: Boolean = false, caseSensitive: Boolean = false) {
		val options = mutableSetOf<RegexOption>()
		if (literal) options += RegexOption.LITERAL
		if (!caseSensitive) options += RegexOption.IGNORE_CASE
		val wordFilter = WordFilter(name, Regex(pattern, options), replace.orEmpty())
		data.wordFilters.add(wordFilter)
		saveData()
	}
	
	fun removeWordFilter(name: String): Boolean {
		val removed = data.wordFilters.removeIf { it.name == name }
		if (removed) saveData()
		return removed
	}
	
}