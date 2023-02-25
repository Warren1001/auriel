package io.github.warren1001.auriel.command

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.commands.*
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.a
import io.github.warren1001.auriel.d2.D2
import io.github.warren1001.auriel.d2.tz.TerrorZone
import io.github.warren1001.auriel.guild.ConfigError
import io.github.warren1001.auriel.queue_
import io.github.warren1001.auriel.replyFull
import io.github.warren1001.auriel.user.Users
import io.github.warren1001.d2data.enums.json.D2DesecratedZones
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit

class Commands(private val auriel: Auriel) {
	
	private val commandActions: MutableMap<String, (SlashCommandInteractionEvent) -> Unit> = mutableMapOf()
	
	init {
		auriel.jda.listener<SlashCommandInteractionEvent> {
			try {
				commandActions[it.name]!!.invoke(it)
			} catch (e: Exception) {
				auriel.warren(e.stackTraceToString())
			}
		}
		commandActions["ping"] = { it.reply_("Pong!").queue_() }
		commandActions["stop"] = { it.reply_("Shutting down.").queue_ { auriel.shutdown() } }
		commandActions["rolegivemsg"] = {
			val message = it.getOption("message")!!.asString
			val role = it.getOption("role")!!.asRole
			val roleGiveMsg = it.getOption("give-button-text")!!.asString
			val roleRemoveMsg = it.getOption("remove-button-text")!!.asString
			it.guild!!.a().sendRoleGiveMsg(it.channel.asGuildMessageChannel(), role, message, roleGiveMsg, roleRemoveMsg)
			it.reply_("Role give message sent.").queue_()
		}
		commandActions["addfilter"] = {
			if (it.subcommandName == "server") {
				val guild = it.guild!!.a()
				val name = it.getOption("name")!!.asString
				val expression = it.getOption("expression")!!.asString
				val replacement = it.getOption("replacement")!!.asString
				val literal = it.getOption("literal")?.asBoolean ?: false
				val caseSensitive = it.getOption("case-sensitive")?.asBoolean ?: false
				if (guild.addWordFilter(name, expression, replacement, literal, caseSensitive)) {
					it.reply_("The server filter **$name** added.").queue_()
				} else {
					it.reply_("The server filter name **$name** is already taken.").queue_()
				}
			} else {
				val channel = it.channel.asGuildMessageChannel().a()
				val name = it.getOption("name")!!.asString
				val expression = it.getOption("expression")!!.asString
				val replacement = it.getOption("replacement")!!.asString
				val literal = it.getOption("literal")?.asBoolean ?: false
				val caseSensitive = it.getOption("case-sensitive")?.asBoolean ?: false
				channel.addWordFilter(name, expression, replacement, literal, caseSensitive)
				it.reply_("Added word filter `$name`.").queue_()
			}
		}
		commandActions["removefilter"] = {
			val name = it.getOption("name")!!.asString
			if (it.subcommandName == "server") {
				if (it.guild!!.a().removeWordFilter(name)) {
					it.reply_("Removed word filter `$name`.").queue_()
				} else {
					it.reply_("No word filter with the name `$name` exists.").queue_()
				}
			} else {
				if (it.channel.asGuildMessageChannel().a().removeWordFilter(name)) {
					it.reply_("Removed word filter `$name` from this channel.").queue_()
				} else {
					it.reply_("No word filter with the name `$name` exists in this channel.").queue_()
				}
			}
		}
		commandActions["listfilter"] = {
			if (it.subcommandName == "server") {
				val guild = it.guild!!.a()
				val filters = guild.data.wordFilters
				if (filters.isNotEmpty()) {
					it.reply_("Filters for this server:\n${filters.joinToString("\n") { "${it.name}: `${it.regex.pattern}`" }}").queue_()
				} else {
					it.reply_("No filters exist for this server.").queue_()
				}
			} else {
				val channel = it.channel.asGuildMessageChannel().a()
				val filters = channel.data.wordFilters
				if (filters.isNotEmpty()) {
					it.reply_("Filters for this server:\n${filters.joinToString("\n") { "${it.name}: `${it.regex.pattern}`" }}").queue_()
				} else {
					it.reply_("No filters exist for this channel.").queue_()
				}
			}
		}
		/*commandActions["activity"] = {
			val message = it.getOption("message")!!.asString
			val typeString = it.getOption("type")!!.asString.uppercase()
			if (typeString == "CLEAR") {
				auriel.jda.presence.activity = null
				it.reply_("Cleared activity.").queue_()
			} else {
				if (typeString != "PLAYING" && typeString != "COMPETING" && typeString != "LISTENING" && typeString != "WATCHING" && typeString != "STREAMING") {
					it.reply_("Invalid activity type. Read the damn tooltip.").queue_()
				} else {
					val type = Activity.ActivityType.valueOf(typeString)
					val url = it.getOption("url")?.asString
					if (type == Activity.ActivityType.STREAMING && url == null) {
						it.reply_("You need to provide a URL for streaming.").queue_()
					} else if (type == Activity.ActivityType.STREAMING && !Activity.isValidStreamingUrl(url)) {
						it.reply_("Invalid URL for streaming.").queue_()
					} else {
						auriel.jda.presence.activity = Activity.of(type, message, url)
						it.reply_("Updated the bot's activity message.").queue_()
					}
				}
			}
		}
		auriel.autoCompletionHandler.setAutocompleteStrings("activity", option = "type", "playing", "competing", "listening", "watching", "streaming", "clear")*/
		commandActions["tz"] = {
			val msg = it.getOption("message", "%ROLE% **%ZONE%** is/are Terrorized!") { it.asString }
			// intellij will tell you its okay to remove the type arguments, its not okay!! program wont compile if its missing (module error)
			auriel.specialMessageHandler.replyChainMessageCallback<String, Role?> {
				userId = it.user.id
				values = D2.files.loadJson(D2DesecratedZones.FILE_PATH)["desecrated_zones"][0]["zones"].asIterable().map {
					it["levels"].asIterable().map { it["level_id"].asInt() }.toList().joinToString(",")
				}
				format = "What role do you want to use for the Terror Zone **%s**?"
				finishMsg = "Done setting up roles!"
				validationMessage = "**You must provide a valid role in the form of a role mention as if you were tagging the role. Try again.**"
				parse = { _, message -> message.mentions.roles.firstOrNull() }
				display = { it.split(',') }
				createMessage = { messageCreateData, callback -> it.reply_(messageCreateData.content).queue_ { callback.invoke(it) } }
				finished = { data -> auriel.guilds.getGuild(it.guild!!.id).setupTZ(it.channel.id, msg, data) }
			}
		}
		commandActions["tznotify"] = {
			val guild = it.guild!!.a()
			if (guild.tzGuildData.roleIds == null) {
				it.reply_("This feature has not been setup yet.").queue_()
			} else {
				val roleIds = guild.tzGuildData.roleIds!!
				val roleIdsList: List<Map.Entry<String, String>> = roleIds.entries.toList()
				val roleIdsByAct: List<MutableList<Map.Entry<String, String>>> = listOf(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
				roleIdsList.forEach { roleIdsByAct[it.key.act - 1].add(it) }
				var randId = 0
				
				val messageCreateData = auriel.specialMessageHandler.replyMultiSelectMenuMessage<MutableList<Map.Entry<String, String>>> {
					userId = it.user.id
					values = roleIdsByAct
					format = "What role do you want to use for the Terror Zones in **%s**?"
					finishMsg = "You will now receive notifications for the selected TZs."
					onlyOne = false
					mustChoose = false
					filter = { list, i -> (list[0].key.act - 1) == i }
					optionConverter = { tz -> tz.map { SelectOption(it.key.zoneName, "${it.value}-${randId++}") } }
					display = { list -> "Act ${list[0].key.act}" }
					finished = { data ->
						val addRoles = data.map { it.value }.flatten().map { if (it.contains("-")) it.substringBefore("-") else it }.map { auriel.jda.getRoleById(it)!! }.toSet()
						val removeRoles = roleIds.values.map { auriel.jda.getRoleById(it)!! }.toSet() - addRoles
						it.guild!!.modifyMemberRoles(it.member!!, addRoles, removeRoles).queue_()
					}
				}
				it.reply_(messageCreateData.content, components = messageCreateData.components).queue_()
			}
		}
		commandActions["tzrolebutton"] = {
			val message = it.getOption("message")!!.asString
			val buttonText = it.getOption("button")!!.asString
			val createMessageData = MessageCreate(message, components = listOf(ActionRow.of(Button.primary("tzrolebutton", buttonText))))
			it.channel.sendMessage(createMessageData).queue_()
			it.reply_("Done!").queue_()
		}
		commandActions["timer"] = fun(it: SlashCommandInteractionEvent) {
			var success = false
			if (it.subcommandName == "start") {
				val name = it.getOption("name")!!.asString
				if (name == "message-age") {
					success = true
					val channel = it.channel.asGuildMessageChannel().a()
					if (!channel.startMessageAgeTimer()) {
						it.reply_("You must configure the message age timer first. Check `/config`.").queue_()
						return
					}
				} else if (name == "youtube") {
					success = true
					val guild = it.guild!!.a()
					if (!guild.youtubeAnnouncer.start()) {
						it.reply_("You must configure the YouTube timer first. Check `/config`.").queue_()
						return
					}
				}
			} else {
				val name = it.getOption("name")!!.asString
				if (name == "message-age") {
					success = true
					it.channel.asGuildMessageChannel().a().stopMessageAgeTimer()
				} else if (name == "youtube") {
					success = true
					it.guild!!.a().youtubeAnnouncer.stop()
				}
			}
			if (success) {
				it.reply_("Successfully started the timer!").queue_()
			} else {
				it.reply_("Invalid timer name.").queue_()
			}
		}
		auriel.autoCompletionHandler.addAutocompleteStrings("timer", "start", option = "name", "message-age", "youtube")
		auriel.autoCompletionHandler.addAutocompleteStrings("timer", "stop", option = "name", "message-age", "youtube")
		commandActions["clone"] = {
			val guild = it.guild!!.a()
			if (it.subcommandName == "start") {
				if (guild.cloneHandler.isRunning()) {
					it.reply_("The Diablo Clone system is already running.").queue_()
				} else {
					val helpee = it.getOption("helpee-channel")!!.asChannel.asGuildMessageChannel()
					val helper = it.getOption("helper-channel")!!.asChannel.asGuildMessageChannel()
					guild.cloneHandler.start(helpee, helper)
					it.reply_("Started the Diablo Clone system with the helpee message in ${helpee.asMention} and the helper message in ${helper.asMention}.").queue_()
				}
			} else {
				guild.cloneHandler.stop()
				it.reply_("Stopped the Diablo Clone system if it was running.").queue_()
			}
		}
		auriel.jda.updateCommands {
			slash("ping", "Pong!") { restrict(true) }
			slash("stop", "Stops the bot. Only use if the bot is doing extremely bad things to the server.") { restrict(true, Permission.ADMINISTRATOR) }
			// config command
			slash("rolegivemsg", "Send a message with the ability to give a user a role.") {
				restrict(true, Permission.BAN_MEMBERS)
				option<String>("message", "The message to send.", required = true)
				option<Role>("role", "The role to give.", required = true)
				option<String>("give-button-text", "The text on the give role button.", required = true)
				option<String>("remove-button-text", "The text on the remove role button.", required = true)
			}
			slash("addfilter", "Add a word filter to the server or this specific channel.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("server", "Add a word filter to the server.") {
					option<String>("name", "An identifying name for the filter, this should be the word/phrase that is being filtered.", required = true)
					option<String>("expression", "The expression to filter, regular expression by default.", required = true)
					option<String>("replacement", "The replacement (literal) for the filtered word/phrase. The message will be deleted if left empty.")
					option<Boolean>("literal", "Whether to use literal matching or not.")
					option<Boolean>("case-sensitive", "Whether to use case-sensitive matching or not.")
				}
				subcommand("channel", "Add a word filter to this channel only.") {
					option<String>("name", "An identifying name for the filter, this should be the word/phrase that is being filtered.", required = true)
					option<String>("expression", "The expression to filter, regular expression by default.", required = true)
					option<String>("replacement", "The replacement (literal) for the filtered word/phrase. The message will be deleted if left empty.")
					option<Boolean>("literal", "Whether to use literal matching or not, defaults to False.")
					option<Boolean>("case-sensitive", "Whether to use case-sensitive matching or not, defaults to False.")
				}
			}
			slash("removefilter", "Remove a word filter from the server or this specific channel.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("server", "Remove a word filter from the server.") {
					option<String>("name", "The name of the filter to remove.", required = true)
				}
				subcommand("channel", "Remove a word filter from this channel only.") {
					option<String>("name", "The name of the filter to remove.", required = true)
				}
			}
			slash("listfilter", "List all word filters for the server or this specific channel.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("server", "List all word filters for the server.")
				subcommand("channel", "List all word filters for this channel.")
			}
			/*slash("activity", "Set the bot's activity message. The streaming option needs the optional URL.") {
				restrict(true, Permission.ADMINISTRATOR)
				option<String>("type", "playing|competing|listening|watching|streaming|clear", required = true, autocomplete = true)
				option<String>("message", "The message to set the activity to. Useless if type is 'clear'.", required = true)
				option<String>("url", "The URL of the stream.")
			}*/
			slash("tz", "Sets up Terror Zone notifications in this channel + roles.") {
				restrict(true, Permission.MANAGE_SERVER)
				option<String>("message", "TZ announce msg. %ROLE% and %ZONE% are the fill names", required = false)
			}
			slash("tzrolebutton", "Choose the areas you want to be notified for.") {
				restrict(true, Permission.BAN_MEMBERS)
				option<String>("message", "The message to use with the button.", required = true)
				option<String>("button", "The text on the button itself.", required = true)
			}
			slash("config", "Configuration options for the server.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("string", "Set a string configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
					option<String>("value", "The value of the configuration option.", required = true)
				}
				subcommand("longstring", "Set a long string (multi-line) configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
				}
				subcommand("channel", "Set a channel configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
				}
				subcommand("number", "Set a numerical configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
					option<Long>("value", "The value of the configuration option.", required = true)
				}
				subcommand("boolean", "Set a boolean configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
					option<Boolean>("value", "The value of the configuration option.", required = true)
				}
				subcommand("show", "Show the value and relevant information for a configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
				}
			}
			slash("timer", "Start and stop specific timers.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("start", "Start a timer.") {
					option<String>("name", "The name of the timer to start.", required = true, autocomplete = true)
				}
				subcommand("stop", "Stop a timer.") {
					option<String>("name", "The name of the timer to stop.", required = true, autocomplete = true)
				}
			}
			slash("clone", "Start or stop the DClone system.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("start", "Start the DClone system.") {
					option<GuildMessageChannel>("helpee-channel", "The channel to post the Request Help message.", required = true)
					option<GuildMessageChannel>("helper-channel", "The channel to post the Give Help message.", required = true)
				}
				subcommand("stop", "Stop the DClone system.")
			}
			slash("vouch", "Give another user a vouch.") {
				restrict(true)
				option<User>("user", "The user you are wanting to vouch.", required = true)
				option<String>("reason", "The reason you're vouching them.", required = true)
			}
			slash("vouches", "Look at a user's recent vouches.") {
				restrict(true)
				option<User>("user", "The user whose vouches you want to see.", required = true)
				option<Boolean>("hide", "Hide the response of this command (so people won't see you're checking their vouches).", required = false)
			}
			slash("removevouch", "Remove a user's vouch.") {
				restrict(true, Permission.BAN_MEMBERS)
				option<User>("user", "The user whose vouch you want to remove.", required = true)
				option<Long>("id", "The id of the vouch you're removing.", required = true)
			}
			slash("blacklistvouch", "Prevent a user from using the /vouch command.") {
				restrict(true, Permission.MANAGE_SERVER)
				option<User>("user", "The user you want to blacklist from using the /vouch command.", required = true)
			}
			slash("spam", "Add or remove a spam bot filter.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("add", "Add a spam bot filter.") {
					option<String>("name", "The name of the filter.", required = true)
					option<String>("regex", "The regex to use for the filter. Leaving empty will use chat box for multi-line (AND) support.", required = false)
					option<Int>("repeat", "Amount of times to type the filter before being muted. Defaults to 1.", required = false)
					option<Int>("window", "The length of the window in seconds for when the filter cannot be repeated. Defaults to 5 seconds.", required = false)
				}
				subcommand("remove", "Remove a spam bot filter.") {
					option<String>("name", "The name of the filter to remove.", required = true)
				}
				subcommand("list", "List all spam bot filters.")
			}
			slash("user", "Configuration options for the users of this server.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("string", "Set a string user configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
					option<String>("value", "The value of the configuration option.", required = true)
				}
				subcommand("longstring", "Set a long string (multi-line) user configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
				}
				subcommand("channel", "Set a channel user configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
				}
				subcommand("number", "Set a numerical user configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
					option<Long>("value", "The value of the configuration option.", required = true)
				}
				subcommand("boolean", "Set a boolean user configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
					option<Boolean>("value", "The value of the configuration option.", required = true)
				}
				subcommand("show", "Show the value and relevant information for a user configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true, autocomplete = true)
				}
			}
			slash("language", "Change the language of the Diablo II item commands for you.") {
				restrict(true)
				option<String>("language", "The language to change to.", required = true, autocomplete = true)
			}
			slash("item", "Get information about a Diablo II item.") {
				restrict(true)
				option<String>("item", "The name of the item to get information about.", required = true, autocomplete = true)
			}
			slash("unique", "Get information about a Diablo II unique item.") {
				restrict(true)
				option<String>("item", "The name of the unique item to get information about.", required = true, autocomplete = true)
			}
		}.queue_()
		auriel.autoCompletionHandler.addAutocompleteStrings("unique", "item") { member -> auriel.items.getAllUniqueItems(member.a().data.getAsString("user:language")) }
		commandActions["unique"] = {
			val itemName = it.getOption("item")!!.asString
			val item = auriel.items.getUniqueItem(itemName)
			if (item == null) {
				it.reply_("No item found with the name $itemName.").queue_()
			} else {
				it.reply(MessageCreate(embeds = listOf(item.createEmbed(it.member!!.a().data.getAsString("user:language"))))).queue_()
			}
		}
		auriel.autoCompletionHandler.addAutocompleteStrings("item", "item") { member -> auriel.items.getAllItems(member.a().data.getAsString("user:language")) }
		commandActions["item"] = {
			val itemName = it.getOption("item")!!.asString
			val item = auriel.items.getBaseItem(itemName)
			if (item == null) {
				it.reply_("No item found with the name $itemName.").queue_()
			} else {
				it.reply(MessageCreate(embeds = listOf(item.createEmbed(it.member!!.a().data.getAsString("user:language"))))).queue_()
			}
		}
		auriel.autoCompletionHandler.addAutocompleteStrings("language", option = "language", Users.LANGUAGES)
		commandActions["language"] = {
			val language = it.getOption("language")!!.asString
			if (!Users.LANGUAGES.contains(language)) {
				it.reply_("Unsupported language. Supported languages are: ${Users.LANGUAGES.joinToString(", ")}").queue_()
			} else {
				val user = it.member!!.a()
				user.data.set("user:language", language)
				user.saveData()
				it.reply_("Language changed to $language.").queue_()
			}
		}
		commandActions["user"] = fun(it: SlashCommandInteractionEvent) {
			val origKey = it.getOption("key")!!.asString
			var success = ConfigError.NONE
			if (origKey.contains(":")) {
				when (it.subcommandName) {
					"string" -> {
						val value = it.getOption("value")!!.asString
						success = auriel.config.modifyConfigValue(it.member!!, origKey) {
							this.guild = it.guild!!
							this.channel = it.channel.asGuildMessageChannel()
							this.author = it.user
							this.value = value
						}
					}
					"longstring" -> {
						success = if (auriel.config.hasKey(origKey)) ConfigError.NONE else ConfigError.NOT_FOUND
						if (success == ConfigError.NONE) {
							auriel.specialMessageHandler.replySingleMessage(it, "Respond with the value you'd like to set $origKey to.") { value ->
								val success2 = auriel.config.modifyConfigValue(it.member!!, origKey) {
									this.guild = it.guild!!
									this.channel = it.channel.asGuildMessageChannel()
									this.author = it.user
									this.value = value
								}
								if (success2 == ConfigError.NONE) {
									MessageEdit("Configuration option set.")
								} else {
									MessageEdit("You do not have permission to modify this configuration option.")
								}
							}
						} else {
							it.reply("That is not a valid key to configure.").queue_()
						}
						return
					}
					"channel" -> {
						val value = it.channel.asGuildMessageChannel()
						success = auriel.config.modifyConfigValue(it.member!!, origKey) {
							this.guild = it.guild!!
							this.channel = it.channel.asGuildMessageChannel()
							this.author = it.user
							this.value = value
						}
					}
					"number" -> {
						val value = it.getOption("value")!!.asLong
						success = auriel.config.modifyConfigValue(it.member!!, origKey) {
							this.guild = it.guild!!
							this.channel = it.channel.asGuildMessageChannel()
							this.author = it.user
							this.value = value
						}
					}
					"boolean" -> {
						val value = it.getOption("value")!!.asBoolean
						success = auriel.config.modifyConfigValue(it.member!!, origKey) {
							this.guild = it.guild!!
							this.channel = it.channel.asGuildMessageChannel()
							this.author = it.user
							this.value = value
						}
					}
					"show" -> {
						it.replyFull(auriel.config.prettyPrintConfigData(origKey, it.member!!)).queue_()
						return
					}
				}
			}
			when (success) {
				ConfigError.NONE -> {
					it.reply_("Configuration option set.").queue_()
				}
				ConfigError.NOT_FOUND -> {
					it.reply_("That is not a valid key to configure.").queue_()
				}
				ConfigError.INVALID_DATA_TYPE -> {
					it.reply_("That is not a valid value for this key. Use `/config show` to see the proper values for this key.").queue_()
				}
				ConfigError.NO_PERMISSION -> {
					it.reply_("You do not have permission to set this option.").queue_()
				}
			}
		}
		commandActions["blacklistvouch"] = {
			val id = it.getOption("user")!!.asUser.id
			val guild = it.guild!!.a()
			val blacklist = guild.data.vouchBlacklist
			if (blacklist.contains(id)) {
				it.reply_("That user is already blacklisted.").queue_()
			} else {
				blacklist.add(id)
				guild.saveData()
				it.reply_("User is now blacklisted.").queue_()
			}
		}
		commandActions["spam"] = {
			val subcommand = it.subcommandName
			if (subcommand == "add") {
				val name = it.getOption("name")!!.asString
				val regex = it.getOption("regex")?.asString
				val repeat = it.getOption("repeat")?.asInt ?: 1
				val window = it.getOption("window")?.asInt ?: 5
				if (regex == null) {
					auriel.specialMessageHandler.replySingleMessage(it, "Respond with the regexes you'd like to use for this filter. Separate them with new lines." +
							" All must match for this filter to be triggered.") { value ->
						val editContent = if (it.guild!!.a().addSpamFilter(name, value.split("\n"), repeat, window)) {
							"The spam filter **$name** has been added."
						} else {
							"The spam filter name **$name** is already taken."
						}
						MessageEdit(editContent)
					}
				} else {
					it.guild!!.a().addSpamFilter(name, regex, repeat, window)
					it.reply_("The spam filter **$name** has been added.").queue_()
				}
			} else if (subcommand == "remove") {
				val name = it.getOption("name")!!.asString
				if (it.guild!!.a().removeSpamFilter(name)) {
					it.reply_("The spam filter **$name** has been removed.").queue_()
				} else {
					it.reply_("The spam filter **$name** does not exist.").queue_()
				}
			} else if (subcommand == "list") {
				val filters = it.guild!!.a().data.spamFilters
				if (filters.isEmpty()) {
					it.reply_("There are no spam filters.").queue_()
				} else {
					it.replyFull(filters.joinToString("\n\n") { it.prettyPrint() }).queue_()
				}
			}
		}
		commandActions["removevouch"] = {
			val user = it.getOption("user")!!.asMember!!
			val aUser = user.a()
			val id = it.getOption("id")!!.asLong
			if (user.a().data.vouches.removeIf { it.id == id }) {
				aUser.saveData()
				it.reply_("Removed the vouch.").queue_()
			} else {
				it.reply_("${user.user.asTag} does not have a vouch with that ID.").queue_()
			}
		}
		commandActions["vouch"] = {
			if (it.guild!!.a().data.vouchBlacklist.contains(it.user.id)) {
				it.deferReply().delay(14, TimeUnit.MINUTES).flatMap { it.editOriginal("?") }.queue_()
			} else {
				val user = it.getOption("user")!!.asMember!!
				if (user.id == it.user.id) {
					it.reply_("You cannot vouch yourself, silly!").queue_()
				} else {
					val reason = it.getOption("reason")!!.asString
					if (user.a().giveVouch(it.member!!, reason)) {
						it.reply("${it.user.asMention} has given ${user.asMention} a vouch for: **$reason**.").queue_()
					} else {
						it.reply_("You must wait ${it.guild!!.a().data.getAsNumber("guild:vouch-cooldown").toLong()} seconds between vouches.").queue_()
					}
				}
			}
		}
		commandActions["vouches"] = {
			val user = it.getOption("user")!!.asMember!!
			val hide = it.getOption("hide")?.asBoolean ?: false
			val totalVouches = user.a().data.vouches
			val vouches = totalVouches.takeLast(5).reversed()
			if (vouches.isEmpty()) {
				it.reply_("That user has no vouches.").queue_()
			} else {
				val embed = EmbedBuilder()
					.setTitle("${totalVouches.size} vouches for ${user.user.asTag}")
					.setDescription("Showing the last 5 vouches:")
					.setColor(Color.GREEN)
					.setTimestamp(Instant.now())
				for (vouch in vouches) {
					val voucher = vouch.vouchedBy.let { auriel.jda.getUserById(it) }?.asTag ?: "Unknown User (${vouch.vouchedBy})"
					embed.addField(voucher, "(${vouch.id}) - ${vouch.reason}", false)
				}
				if (hide) {
					it.reply_(embeds = listOf(embed.build())).queue_()
				} else {
					it.reply(MessageCreate(embeds = listOf(embed.build()))).queue_()
				}
			}
		}
		commandActions["config"] = fun(it: SlashCommandInteractionEvent) {
			val origKey = it.getOption("key")!!.asString
			var success = ConfigError.NONE
			if (origKey.contains(":")) {
				when (it.subcommandName) {
					"string" -> {
						val value = it.getOption("value")!!.asString
						success = auriel.config.modifyConfigValue(it.member!!, origKey) {
							this.guild = it.guild!!
							this.channel = it.channel.asGuildMessageChannel()
							this.author = it.user
							this.value = value
						}
					}
					"longstring" -> {
						success = if (auriel.config.hasKey(origKey)) ConfigError.NONE else ConfigError.NOT_FOUND
						if (success == ConfigError.NONE) {
							auriel.specialMessageHandler.replySingleMessage(it, "Respond with the value you'd like to set $origKey to.") { value ->
								val success2 = auriel.config.modifyConfigValue(it.member!!, origKey) {
									this.guild = it.guild!!
									this.channel = it.channel.asGuildMessageChannel()
									this.author = it.user
									this.value = value
								}
								if (success2 == ConfigError.NONE) {
									MessageEdit("Configuration option set.")
								} else {
									MessageEdit("You do not have permission to modify this configuration option.")
								}
							}
						} else {
							it.reply("That is not a valid key to configure.").queue_()
						}
						return
					}
					"channel" -> {
						val value = it.channel.asGuildMessageChannel()
						success = auriel.config.modifyConfigValue(it.member!!, origKey) {
							this.guild = it.guild!!
							this.channel = it.channel.asGuildMessageChannel()
							this.author = it.user
							this.value = value
						}
					}
					"number" -> {
						val value = it.getOption("value")!!.asLong
						success = auriel.config.modifyConfigValue(it.member!!, origKey) {
							this.guild = it.guild!!
							this.channel = it.channel.asGuildMessageChannel()
							this.author = it.user
							this.value = value
						}
					}
					"boolean" -> {
						val value = it.getOption("value")!!.asBoolean
						success = auriel.config.modifyConfigValue(it.member!!, origKey) {
							this.guild = it.guild!!
							this.channel = it.channel.asGuildMessageChannel()
							this.author = it.user
							this.value = value
						}
					}
					"show" -> {
						it.replyFull(auriel.config.prettyPrintConfigData(origKey, it.member!!)).queue_()
						return
					}
				}
			}
			when (success) {
				ConfigError.NONE -> {
					it.reply_("Configuration option set.").queue_()
				}
				ConfigError.NOT_FOUND -> {
					it.reply_("That is not a valid key to configure.").queue_()
				}
				ConfigError.INVALID_DATA_TYPE -> {
					it.reply_("That is not a valid value for this key. Use `/config show` to see the proper values for this key.").queue_()
				}
				ConfigError.NO_PERMISSION -> {
					it.reply_("You do not have permission to set this option.").queue_()
				}
			}
		}
	}
	
}