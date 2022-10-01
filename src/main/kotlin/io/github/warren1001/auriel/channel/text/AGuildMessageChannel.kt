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
	
	private val data: AGuildMessageData
	
	private val lastMemberMessage = mutableMapOf<User, Message>()
	private var messageAgeTimer: Timer? = null
	
	constructor(auriel: Auriel, guild: AGuild, id: String) {
		this.auriel = auriel
		this.guild = guild
		this.id = id
		data = AGuildMessageData(id)
		setup()
	}
	
	constructor(auriel: Auriel, guild: AGuild, id: String, data: AGuildMessageData) {
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
	
	fun handleMessageReceived(event: MessageReceivedEvent) {
		// cant be a bot and will always be a guild channel
		
		val author = event.member!!
		val channel = event.channel.asGuildMessageChannel()
		
		if (!author.hasPermission(Permission.BAN_MEMBERS)) {
			
			var message = event.message.contentRaw
			
			// word filters
			val filters = data.wordFilters.filter { it.containsMatchIn(message) }
			if (filters.isNotEmpty()) {
				val blockedPhrases = filters.joinToString(" ") { it.source }
				event.message.delete().queue()
				var repostId: Long? = null
				if (filters.all { it.shouldReplace() } && allowsReposting()) {
					filters.forEach { message = it.replace(message) }
					repostId = event.channel.message("${event.author.asMention} said (filtered): $message}").complete().idLong
				} else {
					event.author.dm("Your message was deleted from ${channel.asMention} in **${event.guild.name}** because it contained the following blocked phrase(s): **$blockedPhrases**\n" +
							"Here's your message incase you didn't save it:").queue()
					event.author.dm(message.quote()).queue()
				}
				guild.logMessageDelete(author, event.channel.asGuildMessageChannel(), "Blocked phrase(s): $blockedPhrases", event.message.contentRaw, repostId)
				return
			}
			
			if (message.split('\n').size > data.lineLimit) {
				event.message.delete().queue()
				event.author.dm("Your message was deleted from ${channel.asMention} in **${event.guild.name}** because it contained too many lines (limit of **${data.lineLimit}**).\n" +
						"Here's your message incase you didn't save it:").queue()
				event.author.dm(message.quote()).queue()
				guild.logMessageDelete(author, event.channel.asGuildMessageChannel(), "Exceeded line limit of ${data.lineLimit}", message)
				return
			}
			
			// only one message
			if (data.onlyOneMessage) {
				val lastMessage = lastMemberMessage[author.user]
				lastMessage?.delete()?.queueDelete()
				lastMemberMessage[author.user] = event.message
			}
			
		}
		
	}
	
	private fun startMessageAgeTimer() {
		if (messageAgeTimer != null) return
		if (data.maxMessageAge > 0L && data.messageAgeInterval > 0L) {
			messageAgeTimer = timer("$id-messageAge", true, 60 * 1000, data.messageAgeInterval) {
				MessageHistory.getHistoryBefore(auriel.jda.getTextChannelById(id)!!, TimeUtil.getDiscordTimestamp(System.currentTimeMillis() - data.maxMessageAge).toString())
						.queue { it.channel.asGuildMessageChannel().purgeMessages(it.retrievedHistory.filter { it.member != null && !it.member!!.hasPermission(Permission.BAN_MEMBERS) }) }
			}
		}
	}
	
	private fun deleteAllButOneMessage() {
		if (data.onlyOneMessage) {
			MessageHistory.getHistoryFromBeginning(auriel.jda.getTextChannelById(id)!!).queue { history ->
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
	
	fun setMaxMessageAge(maxMessageAge: Long, messageAgeInterval: Long) {
		data.maxMessageAge = maxMessageAge
		data.messageAgeInterval = messageAgeInterval
		messageAgeTimer?.cancel()
		messageAgeTimer = null
		startMessageAgeTimer()
		saveData()
	}
	
	fun setAllowReposts(value: Boolean) {
		data.allowBotReposts = value
		saveData()
	}
	
	fun allowsReposting() = data.allowBotReposts
	
	fun addWordFilter(name: String, pattern: String, replace: String? = null, literal: Boolean = false, caseSensitive: Boolean = false) {
		val options = mutableSetOf<RegexOption>()
		if (literal) options += RegexOption.LITERAL
		if (!caseSensitive) options += RegexOption.IGNORE_CASE
		val wordFilter = WordFilter(name, Regex(pattern, options), replace.orEmpty())
		data.wordFilters.add(wordFilter)
		saveData()
	}
	
	fun removeWordFilter(name: String): Boolean {
		val removed = data.wordFilters.removeIf { it.source == name }
		if (removed) saveData()
		return removed
	}
	
	fun setOnlyOneMessage(value: Boolean){
		data.onlyOneMessage = value
		deleteAllButOneMessage()
		saveData()
	}
	
	fun setLineLimit(value: Int) {
		data.lineLimit = value
		saveData()
	}
	
}