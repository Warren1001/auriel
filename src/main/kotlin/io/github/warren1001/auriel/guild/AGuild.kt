package io.github.warren1001.auriel.guild

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.UpdateOptions
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreateBuilder
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.channel.text.AGuildMessageChannel
import io.github.warren1001.auriel.channel.text.AGuildMessageChannelData
import io.github.warren1001.auriel.d2.clone.CloneHandler
import io.github.warren1001.auriel.d2.tz.TerrorZone
import io.github.warren1001.auriel.d2.tz.TerrorZoneTrackerGuildData
import io.github.warren1001.auriel.d2.tz.TerrorZoneTrackerStatus
import io.github.warren1001.auriel.user.Users
import io.github.warren1001.auriel.util.filter.SpamFilter
import io.github.warren1001.auriel.util.filter.WordFilter
import io.github.warren1001.auriel.util.youtube.YoutubeAnnouncer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.litote.kmongo.findOneById
import org.litote.kmongo.updateOne
import java.awt.Color
import java.time.Duration
import java.time.Instant

class AGuild {
	
	private val auriel: Auriel
	val id: String
	
	val data: AGuildData
	val textChannelDataCollection: MongoCollection<AGuildMessageChannelData>
	val tzGuildData: TerrorZoneTrackerGuildData
	val cloneHandler: CloneHandler
	val youtubeAnnouncer: YoutubeAnnouncer
	val users: Users
	
	private val guildMessageChannels = mutableMapOf<String, AGuildMessageChannel>()
	private var lastTZAnnouncement: Message? = null
	
	constructor(auriel: Auriel, id: String, guilds: Guilds) {
		this.auriel = auriel
		this.id = id
		data = guilds.guildDataCollection.findOneById(id) ?: AGuildData(id, guilds.guildDataDefaults.toMutableMap())
		textChannelDataCollection = auriel.database.getCollection("$id-textChannels", AGuildMessageChannelData::class.java)
		tzGuildData = guilds.tzTrackerRoleCollection.findOneById(id) ?: TerrorZoneTrackerGuildData(id)
		cloneHandler = CloneHandler(auriel, this)
		youtubeAnnouncer = YoutubeAnnouncer(auriel, this, data.youtubeData, auriel.youtube)
		users = Users(auriel, this)
		setup()
	}
	
	constructor(auriel: Auriel, id: String, guilds: Guilds, data: AGuildData) {
		this.auriel = auriel
		this.id = id
		this.data = data
		textChannelDataCollection = auriel.database.getCollection("$id-textChannels", AGuildMessageChannelData::class.java)
		tzGuildData = guilds.tzTrackerRoleCollection.findOneById(id) ?: TerrorZoneTrackerGuildData(id)
		cloneHandler = CloneHandler(auriel, this)
		youtubeAnnouncer = YoutubeAnnouncer(auriel, this, data.youtubeData, auriel.youtube)
		users = Users(auriel, this)
		setup()
	}
	
	private fun setup() {
		textChannelDataCollection.find().forEach { guildMessageChannels[it._id] = AGuildMessageChannel(auriel, this, it._id, it) }
		youtubeAnnouncer.start()
	}
	
	fun getGuildMessageChannel(channel: GuildMessageChannel): AGuildMessageChannel = guildMessageChannels.computeIfAbsent(channel.id) { AGuildMessageChannel(auriel, this, it) }
	
	fun forEachGuildMessageChannel(action: (AGuildMessageChannel) -> Unit) = guildMessageChannels.values.forEach(action)
	
	private fun saveTZGuildData() = auriel.guilds.tzTrackerRoleCollection.updateOne(tzGuildData, UpdateOptions().upsert(true))
	
	fun saveData() = auriel.guilds.guildDataCollection.updateOne(data, options = UpdateOptions().upsert(true))
	
	fun setupTZ(channelId: String, format: String, roles: Map<String, Role?>) {
		tzGuildData.channelId = channelId
		tzGuildData.messageTemplate = format
		tzGuildData.roleIds = roles.filterValues { it != null }.mapValues { it.value!!.id }
		tzGuildData.roleMentions = roles.mapValues { it.value?.asMention ?: "?" }
		auriel.guilds.tzTracker.addGuild(id)
		saveTZGuildData()
	}
	
	fun handleMessageReceived(event: MessageReceivedEvent): Boolean {
		// cant be a bot and will always be a guild channel
		
		val author = event.member!!
		
		val aChannel = getGuildMessageChannel(event.channel.asGuildMessageChannel())
		
		if (!author.hasPermission(Permission.BAN_MEMBERS)) {
			
			var message = event.message.contentRaw
			
			// swear filters
			val swearFilters = data.wordFilters.filter { it.containsMatchIn(message) }
			if (swearFilters.isNotEmpty()) {
				val swearWords = swearFilters.joinToString(", ") { it.name }
				event.message.delete().queue_()
				var repostId: Long? = null
				if (swearFilters.all { it.shouldReplace() } && aChannel.allowsReposting()) {
					swearFilters.forEach { message = it.replace(message) }
					repostId = event.channel.message("${event.author.asMention} said (censored): $message").complete().idLong
				} else {
					event.author.dm("Your message was deleted from ${event.channel.asMention} in **${event.guild.name}** because it contained the following blocked phrase(s): **$swearWords**\n" +
							"Here's your message incase you didn't save it:")
					event.author.dm(message.quote())
				}
				logMessageDelete(author, event.channel.asGuildMessageChannel(), "Swearing: $swearWords", event.message.contentRaw, repostId)
				return true
			}
			
			// spam filters
			val triggeredSpamFilters = data.spamFilters.filter { it.containsMatchIn(message) }
			data.spamFilters.filter { !it.containsMatchIn(message) }.forEach { it.checkDecrease(author) }
			if (triggeredSpamFilters.isNotEmpty()) {
				val filter = triggeredSpamFilters.firstOrNull { it.logSpam(author, event.message) }
				if (filter != null) {
					muteSpammer(author, event.message, filter.name)
					return true
				}
			}
			
		}
		
		if (event.channelType == ChannelType.NEWS && data.getAsBoolean("guild:crosspost")) event.message.crosspost().queue_()
		
		return aChannel.handleMessageReceived(event)
		
	}
	
	fun handleSelectMenuInteraction(event: SelectMenuInteractionEvent) {
		if (event.componentId == "tznotify") {
			event.guild!!.modifyMemberRoles(event.member!!, event.selectedOptions.map { auriel.jda.getRoleById(it.value)!! }, emptyList()).queue_()
			event.reply_("You will now receive notifications for those Terror Zones.").queue_()
		}
	}
	
	private fun muteSpammer(member: Member, message: Message, filterName: String) {
		//message.delete().queue_()
		member.timeoutFor(Duration.ofDays(28).minusSeconds(1)).queue_()
		logMessageDelete(member, message.channel.asGuildMessageChannel(), "Suspected bot ($filterName), muted", message.contentRaw)
		data.spamFilters.forEach { it.clearMember(member) }
	}
	
	fun logMessageDelete(author: Member, channel: GuildMessageChannel, reason: String, originalMessage: String, repostId: Long? = null) {
		log(Embed(title = "Message Deleted", color = Color.RED.rgb, timestamp = Instant.now()) {
			field { name = "User"; value = "${author.asMention} (${author.effectiveName}#${author.user.discriminator})"; inline = true }
			field { name = "Channel"; value = channel.asMention; inline = true }
			field { name = "Reason"; value = reason; inline = false }
			field { name = "Original Message"; value = originalMessage.quote(1024); inline = false }
			if (repostId != null) description = "[Jump to repost](https://discord.com/channels/${channel.guild.id}/${channel.id}/$repostId)"
		})
	}
	
	private fun log(embed: MessageEmbed) = data.get("guild:logging-channel")?.let { auriel.jda.getTextChannelById(it as String)?.sendMessageEmbeds(embed)?.queue_() }
	
	fun logDMFailure(member: Member, message: String) {
		log(Embed(title = "DM Failure", color = Color.BLACK.rgb, timestamp = Instant.now()) {
			field { name = "User"; value = "${member.asMention} (${member.effectiveName}#${member.user.discriminator})"; inline = true }
			field { name = "Message"; value = message.quote(1024); inline = false }
			description = "Please setup a fallback channel using `/setfallbackchannel`."
		})
	}
	
	fun sendRoleGiveMsg(channel: GuildMessageChannel, role: Role, message: String, given: String, removed: String) {
		channel.sendMessage(MessageCreateBuilder {
			content = message
			components += ActionRow.of(
				Button.primary("r-${role.id}-g", given.truncate(80)),
				Button.danger("r-${role.id}-r", removed.truncate(80))
			)
		}.build()).queue_()
	}
	
	fun addSpamFilter(name: String, pattern: String, repeat: Int = 1, window: Int = 5) = addSpamFilter(name, listOf(pattern), repeat, window)
	
	fun addSpamFilter(name: String, patterns: Collection<String>, repeat: Int = 1, window: Int = 5): Boolean {
		if (data.spamFilters.any { it.name == name }) return false
		val regexes = patterns.map { Regex(it.replaceOtherAlphabets(), RegexOption.IGNORE_CASE) }.toMutableSet()
		data.spamFilters += if (repeat == 1) SpamFilter(name, regexes) else SpamFilter(name, regexes, repeat, window * 1000L)
		saveData()
		return true
	}
	
	fun removeSpamFilter(name: String): Boolean {
		val removed = data.spamFilters.removeIf { it.name == name }
		if (removed) saveData()
		return removed
	}
	
	fun addWordFilter(name: String, pattern: String, replace: String? = null, literal: Boolean = false, caseSensitive: Boolean = false): Boolean {
		if (data.wordFilters.any { it.name == name }) return false
		val options = mutableSetOf<RegexOption>()
		if (literal) options += RegexOption.LITERAL
		if (!caseSensitive) options += RegexOption.IGNORE_CASE
		val wordFilter = WordFilter(name, Regex(pattern.replaceOtherAlphabets(), options), replace.orEmpty())
		data.wordFilters += wordFilter
		saveData()
		return true
	}
	
	fun removeWordFilter(name: String): Boolean {
		val removed = data.wordFilters.removeIf { it.name == name }
		if (removed) saveData()
		return removed
	}
	
	fun onTerrorZoneChange(zoneIds: List<Int>, deleteLast: Boolean = false) {
		val zoneIdsString = zoneIds.joinToString(",")
		if (deleteLast) lastTZAnnouncement?.delete()?.queueDelete()
		auriel.jda.getChannelById(GuildMessageChannel::class.java, tzGuildData.channelId!!)!!
			.message(tzGuildData.messageTemplate!!.replace("%ROLE%", tzGuildData.roleMentions!![zoneIdsString]!!).replace("%ZONE%", tz.zoneName)).queue_ { lastTZAnnouncement = it }
	}
	
	fun terrorZoneTrackerUpdate(status: TerrorZoneTrackerStatus) = auriel.jda.getChannelById(GuildMessageChannel::class.java, tzGuildData.channelId!!)!!.message(msg).queue_()
	
}