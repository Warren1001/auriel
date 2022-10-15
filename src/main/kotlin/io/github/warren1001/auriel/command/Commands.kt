package io.github.warren1001.auriel.command

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.commands.*
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.a
import io.github.warren1001.auriel.d2.tz.TerrorZone
import io.github.warren1001.auriel.queue_
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

class Commands(private val auriel: Auriel) {
	
	private val commandActions: MutableMap<String, (SlashCommandInteractionEvent) -> Unit> = mutableMapOf()
	
	init {
		auriel.jda.listener<SlashCommandInteractionEvent> { commandActions[it.name]?.invoke(it) }
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
				guild.addWordFilter(name, expression, replacement, literal, caseSensitive)
				it.reply_("Added word filter `$name`.").queue_()
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
					it.reply_("Filters for this server:\n${filters.joinToString("\n") { "${it.source}: `${it.regex.pattern}`" }}").queue_()
				} else {
					it.reply_("No filters exist for this server.").queue_()
				}
			} else {
				val channel = it.channel.asGuildMessageChannel().a()
				val filters = channel.data.wordFilters
				if (filters.isNotEmpty()) {
					it.reply_("Filters for this server:\n${filters.joinToString("\n") { "${it.source}: `${it.regex.pattern}`" }}").queue_()
				} else {
					it.reply_("No filters exist for this channel.").queue_()
				}
			}
		}
		commandActions["activity"] = {
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
		commandActions["tz"] = {
			val msg = it.getOption("message", "%ROLE% **%ZONE%** is/are Terrorized!") { it.asString }
			// intellij will tell you its okay to remove the type arguments, its not okay!! program wont compile if its missing (module error)
			auriel.specialMessageHandler.replyChainMessageCallback<TerrorZone, Role?> {
				userId = it.user.id
				values = TerrorZone.values().toList()
				format = "What role do you want to use for the Terror Zone **%s**?"
				finishMsg = "Done setting up roles!"
				parse = { _, message -> message.mentions.roles.firstOrNull() }
				display = TerrorZone::zoneName
				createMessage = { messageCreateData, callback -> it.reply_(messageCreateData.content).queue_ { callback.invoke(it) } }
				finished = { data -> auriel.guilds.getGuild(it.guild!!.id).setupTZ(it.channel.id, msg, data) }
			}
			/*auriel.specialMessageHandler.replyChainMessageCallback(it.user, TerrorZone.values().toList(), "What role do you want to use for the Terror Zone **%s**?", "Done setting up roles!",
				{ _, message -> message.mentions.roles.firstOrNull() }, TerrorZone::zoneName, { messageCreateData, callback -> it.reply_(messageCreateData.content).queue_ { callback.invoke(it) } }
			) { data ->
				auriel.guilds.getGuild(it.guild!!.id).setupTZ(it.channel.id, msg, data)
			}*/
		}
		commandActions["tznotify"] = {
			val guild = it.guild!!.a()
			if (guild.tzGuildData.roleIds == null) {
				it.reply_("This feature has not been setup yet.").queue_()
			} else {
				val roleIds = guild.tzGuildData.roleIds!!
				val roleIdsList: List<Map.Entry<TerrorZone, String>> = roleIds.entries.toList()
				val roleIdsByAct: List<MutableList<Map.Entry<TerrorZone, String>>> = listOf(mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf(), mutableListOf())
				roleIdsList.forEach { roleIdsByAct[it.key.act - 1].add(it) }
				var randId = 0
				
				val messageCreateData = auriel.specialMessageHandler.replyMultiSelectMenuMessage<MutableList<Map.Entry<TerrorZone, String>>> {
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
		commandActions["timer"] = {
			var success = false
			if (it.subcommandName == "start") {
				val name = it.getOption("name")!!.asString
				if (name == "message-age") {
					success = true
					val channel = it.channel.asGuildMessageChannel().a()
					if (channel.startMessageAgeTimer()) {
						it.reply_("Started the message age timer.").queue_()
					} else {
						it.reply_("You must configure the message age timer first. Check `/config`.").queue_()
					}
				} else if (name == "youtube") {
					success = true
					val guild = it.guild!!.a()
					if (guild.youtubeAnnouncer.start()) {
						it.reply_("Started the YouTube timer.").queue_()
					} else {
						it.reply_("You must configure the YouTube timer first. Check `/config`.").queue_()
					}
				}
			} else {
				val name = it.getOption("name")!!.asString
				if (name == "message-age") {
					success = true
					it.channel.asGuildMessageChannel().a().stopMessageAgeTimer()
					it.reply_("Stopped the message age timer if it was running.").queue_()
				} else if (name == "youtube") {
					success = true
					it.guild!!.a().youtubeAnnouncer.stop()
					it.reply_("Stopped the YouTube timer if it was running.").queue_()
				}
			}
			if (success) {
				it.reply_("Successfully started the timer!").queue_()
			} else {
				it.reply_("Invalid timer name.").queue_()
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
			slash("activity", "Set the bot's activity message. The streaming option needs the optional URL.") {
				restrict(true, Permission.BAN_MEMBERS)
				option<String>("type", "playing|competing|listening|watching|streaming|clear", required = true)
				option<String>("message", "The message to set the activity to. Useless if type is 'clear'.", required = true)
				option<String>("url", "The URL of the stream.")
			}
			slash("tz", "Sets up Terror Zone notifications in this channel + roles.") {
				restrict(true, Permission.ADMINISTRATOR)
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
					option<String>("key", "The key of the configuration option.", required = true)
					option<String>("value", "The value of the configuration option.", required = true)
				}
				subcommand("channel", "Set a channel configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true)
				}
				subcommand("long", "Set a long configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true)
					option<Long>("value", "The value of the configuration option.", required = true)
				}
				subcommand("boolean", "Set a boolean configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true)
					option<Boolean>("value", "The value of the configuration option.", required = true)
				}
				subcommand("int", "Set an integer configuration option.") {
					option<String>("key", "The key of the configuration option.", required = true)
					option<Int>("value", "The value of the configuration option.", required = true)
				}
			}
			slash("timer", "Start and stop specific timers.") {
				restrict(true, Permission.BAN_MEMBERS)
				subcommand("start", "Start a timer.") {
					option<String>("name", "The name of the timer to start.", required = true)
				}
				subcommand("stop", "Stop a timer.") {
					option<String>("name", "The name of the timer to stop.", required = true)
				}
			}
			addCommands(
				Commands.user("Make DClone Helper").setGuildOnly(true).setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS)),
				Commands.user("Remove DClone Helper").setGuildOnly(true).setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
			)
		}.queue_()
		commandActions["config"] = fun(it: SlashCommandInteractionEvent) {
			val origKey = it.getOption("key")!!.asString
			var success = false
			if (origKey.contains(":")) {
				val guild = it.guild!!.a()
				val args = origKey.split(":")
				val id = args[0].lowercase()
				val key = args[1].lowercase()
				when (it.subcommandName) {
					"string" -> {
						val value = it.getOption("value")!!.asString
						if (id == "youtube") {
							if (key == "playlist-id") {
								success = true
								guild.youtubeAnnouncer.setPlaylistId(value)
							} else if (key == "message") {
								success = true
								guild.youtubeAnnouncer.setMessage(value)
							}
						}
					}
					"channel" -> {
						val value = it.channel.asGuildMessageChannel()
						if (id == "youtube") {
							if (key == "channel") {
								success = true
								guild.youtubeAnnouncer.setChannelId(value.id)
							}
						} else if (id == "guild") {
							if (key == "logging") {
								success = true
								guild.setLoggingChannel(value)
							} else if (key == "fallback") {
								success = true
								guild.setFallbackChannel(value)
							}
						} else if (id == "tz") {
							if (key == "source") {
								if (!it.member!!.hasPermission(Permission.ADMINISTRATOR)) {
									it.reply_("You do not have the permission to set this option.").queue_()
									return
								}
								success = true
								auriel.guilds.tzTracker.setChannel(it.channel.id)
							}
						}
					}
					"long" -> {
						val value = it.getOption("value")!!.asLong
						if (id == "channel") {
							if (key == "max-message-age") {
								success = true
								it.channel.asGuildMessageChannel().a().setMaxMessageAge(value)
							} else if (key == "message-age-interval") {
								success = true
								it.channel.asGuildMessageChannel().a().setMessageAgeInterval(value)
							}
						}
					}
					"boolean" -> {
						val value = it.getOption("value")!!.asBoolean
						if (id == "channel") {
							if (key == "only-one-message") {
								success = true
								it.channel.asGuildMessageChannel().a().setOnlyOneMessage(value)
							} else if (key == "allow-reposts") {
								success = true
								it.channel.asGuildMessageChannel().a().setAllowReposts(value)
							}
						} else if (id == "guild") {
							if (key == "crosspost") {
								success = true
								guild.setCrosspost(value)
							}
						}
					}
					"int" -> {
						val value = it.getOption("value")!!.asInt
						if (id == "channel") {
							if (key == "line-limit") {
								success = true
								it.channel.asGuildMessageChannel().a().setLineLimit(value)
							}
						}
					}
				}
			}
			if (success) {
				it.reply("Configuration option set.").queue_()
			} else {
				it.reply("That is not a valid key to configure.").queue_()
			}
		}
	}
	
}