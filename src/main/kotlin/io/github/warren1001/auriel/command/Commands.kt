package io.github.warren1001.auriel.command

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.restrict
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.commands.upsertCommand
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.replyModal
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.d2.CloneHandler
import io.github.warren1001.auriel.d2.TerrorZone
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

class Commands(private val auriel: Auriel) {
	
	private val commandActions: MutableMap<String, (SlashCommandInteractionEvent) -> Unit> = mutableMapOf()
	
	init {
		auriel.jda.listener<SlashCommandInteractionEvent> { commandActions[it.name]?.invoke(it) }
		registerCommands()
	}
	
	private fun registerCommands() {
		registerCommand("ping", "Pong!") {
			it.reply_("Pong!").queue()
		}
		registerCommand("stop", "Stops the bot. Only use if the bot is doing extremely bad things to the server.", permission = Permission.BAN_MEMBERS) {
			it.reply_("Shutting down.").queue { auriel.shutdown() }
		}
		registerCommand("modal", "test the modal", permission = Permission.ADMINISTRATOR) {
			it.replyModal("modal1", "title", emptyList()) {
				short("short0", "Discord ID", false, value = "Warren#2433")
				short("short1", "Game Name", false, value = "myGameName1")
				short("short2", "Game Password", false, value = "1")
				paragraph("paragraph1", "Other Info", false, placeholder = "No additional information provided.")
				//components.plusAssign(ActionRow.of(Button.primary("test-button", "Finished")))
				
			}.queue()
		}
		commandActions["youtube"] = {
			val playlistId = it.getOption("playlist-id")?.asString!!
			val message = it.getOption("message")?.asString!!
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			guild.startYoutubeAnnouncer(playlistId, message, it.channel.asGuildMessageChannel())
			it.reply_("Youtube Announcer started.").queue()
		}
		auriel.jda.upsertCommand("youtube", "Set Youtube Announcer settings.") {
			restrict(guild = true, perm = Permission.ADMINISTRATOR)
			option<String>("playlist-id", "The ID of the playlist to check for new videos.", required = true)
			option<String>("message", "The message to send when a new video is found. You can use %TITLE% and %URL% in your message.", required = true)
		}.queue()
		commandActions["messageage"] = {
			val age = it.getOption("age")?.asLong!!
			val interval = it.getOption("interval")?.asLong!!
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			guild.getGuildMessageChannel(it.channel.asGuildMessageChannel()).setMaxMessageAge(age, interval)
			it.reply_("Message age and interval set.").queue()
		}
		auriel.jda.upsertCommand("messageage", "Set the maximum age of a message in the current channel.") {
			restrict(guild = true, perm = Permission.BAN_MEMBERS)
			option<Long>("age", "The maximum age of a message in the current channel in milliseconds.", required = true)
			option<Long>("interval", "How often to check for messages to be deleted in milliseconds.", required = true)
		}.queue()
		commandActions["onlyonemessage"] = {
			val enabled = it.getOption("enabled")?.asBoolean!!
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			guild.getGuildMessageChannel(it.channel.asGuildMessageChannel()).setOnlyOneMessage(enabled)
			it.reply_("Only one message is ${if (enabled) "enabled" else "disabled"}.").queue()
		}
		auriel.jda.upsertCommand("onlyonemessage", "Only allow one message in the current channel.") {
			restrict(guild = true, perm = Permission.BAN_MEMBERS)
			option<Boolean>("enabled", "Whether to enable or disable this feature (true or false).", required = true)
		}.queue()
		commandActions["allowbotrepost"] = {
			val enabled = it.getOption("enabled")?.asBoolean!!
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			guild.getGuildMessageChannel(it.channel.asGuildMessageChannel()).setAllowReposts(enabled)
			it.reply_("Bot reposts are ${if (enabled) "enabled" else "disabled"}.").queue()
		}
		auriel.jda.upsertCommand("allowbotrepost", "Allow the bot to repost messages in the current channel.") {
			restrict(guild = true, perm = Permission.BAN_MEMBERS)
			option<Boolean>("enabled", "Whether to enable or disable this feature (true or false).", required = true)
		}.queue()
		commandActions["linelimit"] = {
			val limit = it.getOption("limit")?.asInt!!
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			guild.getGuildMessageChannel(it.channel.asGuildMessageChannel()).setLineLimit(limit)
			it.reply_("Line limit set to $limit.").queue()
		}
		auriel.jda.upsertCommand("linelimit", "Set the maximum number of lines in a message in the current channel.") {
			restrict(guild = true, perm = Permission.BAN_MEMBERS)
			option<Int>("limit", "The maximum number of lines in a message in the current channel.", required = true)
		}.queue()
		commandActions["rolegivemsg"] = {
			val message = it.getOption("message")?.asString!!
			val role = it.getOption("role")?.asRole!!
			val roleGiveMsg = it.getOption("give-button-text")?.asString!!
			val roleRemoveMsg = it.getOption("remove-button-text")?.asString!!
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			guild.sendRoleGiveMsg(it.channel.asGuildMessageChannel(), role, message, roleGiveMsg, roleRemoveMsg)
			it.reply_("Role give message sent.").queue()
		}
		auriel.jda.upsertCommand("rolegivemsg", "Send a message with the ability to give a user a role.") {
			restrict(guild = true, perm = Permission.BAN_MEMBERS)
			option<String>("message", "The message to send.", required = true)
			option<Role>("role", "The role to give.", required = true)
			option<String>("give-button-text", "The text on the give role button.", required = true)
			option<String>("remove-button-text", "The text on the remove role button.", required = true)
		}.queue()
		registerCommand("setlogchannel", "Sets the server's bot logging channel.", permission = Permission.ADMINISTRATOR) {
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			guild.setLoggingChannel(it.channel.asGuildMessageChannel())
			it.reply_("Log channel set to this channel.").queue()
		}
		auriel.jda.upsertCommand("filter", "Add a word filter to the server or this specific channel.") {
			restrict(guild = true, perm = Permission.BAN_MEMBERS)
			subcommand("server-add", "Add a word filter to the server.") {
				option<String>("name", "An identifying name for the filter, this should be the word/phrase that is being filtered.", required = true)
				option<String>("expression", "The expression to filter, regular expression by default.", required = true)
				option<String>("replacement", "The replacement (literal) for the filtered word/phrase. The message will be deleted if left empty.")
				option<Boolean>("literal", "Whether to use literal matching or not.")
				option<Boolean>("case-sensitive", "Whether to use case-sensitive matching or not.")
			}
			subcommand("server-remove", "Remove a word filter for the server.") {
				option<String>("name", "The name of the filter to remove.", required = true)
			}
			// TODO list?
			subcommand("channel-add", "Add a word filter to this channel only.") {
				option<String>("name", "An identifying name for the filter, this should be the word/phrase that is being filtered.", required = true)
				option<String>("expression", "The expression to filter, regular expression by default.", required = true)
				option<String>("replacement", "The replacement (literal) for the filtered word/phrase. The message will be deleted if left empty.")
				option<Boolean>("literal", "Whether to use literal matching or not, defaults to False.")
				option<Boolean>("case-sensitive", "Whether to use case-sensitive matching or not, defaults to False.")
			}
			subcommand("channel-remove", "Remove a word filter that is specific to this channel only.") {
				option<String>("name", "The name of the filter to remove.", required = true)
			}
			// TODO list?
		}.queue()
		commandActions["filter"] = {
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			val channel = guild.getGuildMessageChannel(it.channel.asGuildMessageChannel())
			when (it.subcommandName) {
				"server-add" -> {
					val name = it.getOption("name")?.asString!!
					val expression = it.getOption("expression")?.asString!!
					val replacement = it.getOption("replacement")?.asString
					val literal = it.getOption("literal")?.asBoolean ?: false
					val caseSensitive = it.getOption("case-sensitive")?.asBoolean ?: false
					guild.addWordFilter(name, expression, replacement, literal, caseSensitive)
					it.reply_("Added word filter `$name`.").queue()
				}
				"server-remove" -> {
					val name = it.getOption("name")?.asString!!
					if (guild.removeWordFilter(name)) {
						it.reply_("Removed word filter `$name`.").queue()
					} else {
						it.reply_("No word filter with the name `$name` exists.").queue()
					}
				}
				"channel-add" -> {
					val name = it.getOption("name")?.asString!!
					val expression = it.getOption("expression")?.asString!!
					val replacement = it.getOption("replacement")?.asString
					val literal = it.getOption("literal")?.asBoolean ?: false
					val caseSensitive = it.getOption("case-sensitive")?.asBoolean ?: false
					channel.addWordFilter(name, expression, replacement, literal, caseSensitive)
					it.reply_("Added word filter `$name`.").queue()
				}
				"channel-remove" -> {
					val name = it.getOption("name")?.asString!!
					if (channel.removeWordFilter(name)) {
						it.reply_("Removed word filter `$name` from this channel.").queue()
					} else {
						it.reply_("No word filter with the name `$name` exists in this channel.").queue()
					}
				}
			}
		}
		auriel.jda.upsertCommand("activity", "Set the bot's activity message. The streaming option needs the optional URL.") {
			restrict(guild = true, perm = Permission.BAN_MEMBERS)
			option<String>("type", "playing|competing|listening|watching|streaming|clear", required = true)
			option<String>("message", "The message to set the activity to. Useless if type is 'clear'.", required = true)
			option<String>("url", "The URL of the stream.")
		}.queue()
		commandActions["activity"] = {
			val message = it.getOption("message")?.asString!!
			val typeString = it.getOption("type")?.asString!!.uppercase()
			if (typeString == "CLEAR") {
				auriel.jda.presence.activity = null
				it.reply_("Cleared activity.").queue()
			} else {
				if (typeString != "PLAYING" && typeString != "COMPETING" && typeString != "LISTENING" && typeString != "WATCHING" && typeString != "STREAMING") {
					it.reply_("Invalid activity type. Read the damn tooltip.").queue()
				} else {
					val type = Activity.ActivityType.valueOf(typeString)
					val url = it.getOption("url")?.asString
					if (type == Activity.ActivityType.STREAMING && url == null) {
						it.reply_("You need to provide a URL for streaming.").queue()
					} else if (type == Activity.ActivityType.STREAMING && !Activity.isValidStreamingUrl(url)) {
						it.reply_("Invalid URL for streaming.").queue()
					} else {
						auriel.jda.presence.activity = Activity.of(type, message, url)
						it.reply_("Updated the bot's activity message.").queue()
					}
				}
			}
		}
		registerCommand("clone", "Request help with Diablo Clone (PC Only).", true, permission = Permission.MESSAGE_SEND) {
			CloneHandler(auriel.guilds.getGuild(it.guild!!.id)).openRequestHelpModal(it)
		}
		commandActions["tz"] = {
			val msg = it.getOption("message")?.asString ?: "%ROLE% **%ZONE%** is/are Terrorized!"
			val tzs = TerrorZone.values().toList()
			auriel.specialMessageHandler.replyChainMessageCallback(it.user, tzs, "What role do you want to use for the Terror Zone **%s**?", "Done setting up roles!",
				{ _, message -> message.mentions.roles.firstOrNull() }, TerrorZone::zoneName, { messageCreateData, callback -> it.reply_(messageCreateData.content).queue { callback.invoke(it) } }
			) { data ->
				auriel.guilds.getGuild(it.guild!!.id).setupTZ(it.channel.id, msg, data)
			}
		}
		auriel.jda.upsertCommand("tz", "Sets up Terror Zone notifications in this channel + roles.") {
			restrict(guild = true, perm = Permission.ADMINISTRATOR)
			option<String>("message", "TZ announce msg. %ROLE% and %ZONE% are the fill names", required = false)
		}.queue()
		registerCommand("setuptz", "The receiving channel for TZ data.", true, permission = Permission.ADMINISTRATOR) {
			auriel.guilds.tzTracker.setChannel(it.channel.id)
			it.reply_("Set the TZ data channel.").queue()
		}
		commandActions["crosspost"] = {
			val crosspost = it.getOption("crosspost")!!.asBoolean
			auriel.guilds.getGuild(it.guild!!.id).setCrosspost(crosspost)
			it.reply_("Auto-crossposting is now ${if (crosspost) "enabled" else "disabled"}.").queue()
		}
		auriel.jda.upsertCommand("crosspost", "Enable or disable auto-crossposting.") {
			restrict(guild = true, perm = Permission.ADMINISTRATOR)
			option<Boolean>("crosspost", "Enable or disable auto-crossposting.", required = true)
		}.queue()
		commandActions["tznotify"] = { // this looks so ugly
			val guild = auriel.guilds.getGuild(it.guild!!.id)
			if (guild.tzGuildData == null) {
				it.reply_("This feature has not been setup yet.").queue()
			} else {
				val roleIds = guild.tzGuildData!!.roleIds!!
				val a: List<Map.Entry<TerrorZone, String>> = roleIds.entries.toList()
				val b: List<MutableList<Map.Entry<TerrorZone, String>>> = listOf(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
				a.forEach { b[it.key.act - 1].add(it) }
				var z = 0
				val messageCreateData = auriel.specialMessageHandler.replyMultiSelectMenuMessage(it.user, b, "What role do you want to use for the Terror Zones in **%s**?",
					"You will now receive notifications for the selected TZs.", false, filter = { list, i -> (list[0].key.act - 1) == i }, mustChoose = false,
					optionConverter = { tz -> tz.map { SelectOption(it.key.zoneName, "${it.value}-${z++}") } },
					display = { list -> "Act ${list[0].key.act}" }
				) { data ->
					val addRoles = data.map { it.value }.flatten().map { if (it.contains("-")) it.substringBefore("-") else it }.map { auriel.jda.getRoleById(it)!! }.toSet()
					val removeRoles = roleIds.values.map { auriel.jda.getRoleById(it)!! }.toSet() - addRoles
					it.guild!!.modifyMemberRoles(it.member!!, addRoles, removeRoles).queue()
				}
				it.reply_(messageCreateData.content, components = messageCreateData.components).queue()
			}
		}
		auriel.jda.upsertCommand("tznotify", "Choose the areas you want to be notified for.") {
			restrict(guild = true, perm = Permission.MESSAGE_SEND)
		}.queue()
		commandActions["tzrolebutton"] = {
			val message = it.getOption("message")!!.asString
			val buttonText = it.getOption("button")!!.asString
			val createMessageData = MessageCreate(message, components = listOf(ActionRow.of(Button.primary("tzrolebutton", buttonText))))
			it.channel.sendMessage(createMessageData).queue()
			it.reply_("Done!").queue()
		}
		auriel.jda.upsertCommand("tzrolebutton", "Choose the areas you want to be notified for.") {
			restrict(guild = true, perm = Permission.BAN_MEMBERS)
			option<String>("message", "The message to use for the button.", required = true)
			option<String>("button", "The text on the button itself", required = true)
		}.queue()
	}
	
	fun registerCommand(name: String, description: String, guildOnly: Boolean = true, permission: Permission = Permission.MESSAGE_SEND, action: (SlashCommandInteractionEvent) -> Unit) {
		commandActions[name] = action
		auriel.jda.upsertCommand(name, description) {
			restrict(guild = guildOnly, perm = permission)
		}.queue()
	}
	
}