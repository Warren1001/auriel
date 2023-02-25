package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.a
import io.github.warren1001.auriel.isWarren
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

class Config(private val auriel: Auriel) {

	private val configData = mutableMapOf<String, ConfigData>()
	
	fun prettyPrintConfigData(key: String, requester: Member): String {
		val configData = configData[key] ?: return "That is not a valid key to configure."
		if (configData.permission == Permission.ADMINISTRATOR && !requester.isWarren()) return "That is not a valid key to configure."
		return configData.prettyPrint()
	}

	fun createGuildConfigData(builder: ConfigDataBuilder.() -> Unit): ConfigData {
		val configDataBuilder = ConfigDataBuilder()
		builder.invoke(configDataBuilder)
		
		require(configDataBuilder.key.isNotBlank()) { "Key cannot be blank" }
		require(configDataBuilder.allowedTypes().isNotEmpty()) { "allowedTypes cannot be empty" }
		requireNotNull(configDataBuilder.valueChanged) { "valueChanged cannot be null" }
		
		if (configDataBuilder.defaultValue != null) configDataBuilder.setDefault.invoke(auriel, configDataBuilder.key, configDataBuilder.defaultValue!!)
		
		val configData = ConfigData(configDataBuilder.key, configDataBuilder.description, configDataBuilder.permission,
			configDataBuilder.allowedTypes(), configDataBuilder.defaultValue ?: "None", configDataBuilder.setDefault, configDataBuilder.saveChanges,
			configDataBuilder.modifyValue, configDataBuilder.valueChanged)
		this.configData[configData.key] = configData
		if (configData.description.isNotBlank()) {
			configData.allowedTypes.forEach {
				it.configSubCommands.forEach {
					auriel.autoCompletionHandler.addAutocompleteStrings("config", it, option = "key", configData.key)
				}
			}
		}
		return configData
	}
	
	fun createChannelConfigData(builder: ConfigDataBuilder.() -> Unit): ConfigData {
		val configDataBuilder = ConfigDataBuilder()
		configDataBuilder.setDefault = ConfigDataBuilder.CHANNEL_SET_DEFAULT
		configDataBuilder.modifyValue = ConfigDataBuilder.CHANNEL_MODIFY_VALUE
		configDataBuilder.saveChanges = ConfigDataBuilder.CHANNEL_SAVE_CHANGES
		builder.invoke(configDataBuilder)
		
		require(configDataBuilder.key.isNotBlank()) { "Key cannot be blank" }
		require(configDataBuilder.description.isNotBlank()) { "Description cannot be blank" }
		require(configDataBuilder.allowedTypes().isNotEmpty()) { "allowedTypes cannot be empty" }
		requireNotNull(configDataBuilder.valueChanged) { "valueChanged cannot be null" }
		
		if (configDataBuilder.defaultValue != null) configDataBuilder.setDefault.invoke(auriel, configDataBuilder.key, configDataBuilder.defaultValue!!)
		
		val configData = ConfigData(configDataBuilder.key, configDataBuilder.description, configDataBuilder.permission,
			configDataBuilder.allowedTypes(), configDataBuilder.defaultValue ?: "None", configDataBuilder.setDefault, configDataBuilder.saveChanges,
			configDataBuilder.modifyValue, configDataBuilder.valueChanged)
		this.configData[configData.key] = configData
		if (configData.description.isNotBlank()) {
			configData.allowedTypes.forEach {
				it.configSubCommands.forEach {
					auriel.autoCompletionHandler.addAutocompleteStrings("config", it, option = "key", configData.key)
				}
			}
		}
		return configData
	}
	
	fun createUserConfigData(builder: ConfigDataBuilder.() -> Unit): ConfigData {
		val configDataBuilder = ConfigDataBuilder()
		configDataBuilder.setDefault = ConfigDataBuilder.USER_SET_DEFAULT
		configDataBuilder.modifyValue = ConfigDataBuilder.USER_MODIFY_VALUE
		configDataBuilder.saveChanges = ConfigDataBuilder.USER_SAVE_CHANGES
		builder.invoke(configDataBuilder)
		
		require(configDataBuilder.key.isNotBlank()) { "Key cannot be blank" }
		require(configDataBuilder.description.isNotBlank()) { "Description cannot be blank" }
		require(configDataBuilder.allowedTypes().isNotEmpty()) { "allowedTypes cannot be empty" }
		requireNotNull(configDataBuilder.valueChanged) { "valueChanged cannot be null" }
		
		if (configDataBuilder.defaultValue != null) configDataBuilder.setDefault.invoke(auriel, configDataBuilder.key, configDataBuilder.defaultValue!!)
		
		val configData = ConfigData(configDataBuilder.key, configDataBuilder.description, configDataBuilder.permission,
			configDataBuilder.allowedTypes(), configDataBuilder.defaultValue ?: "None", configDataBuilder.setDefault, configDataBuilder.saveChanges,
			configDataBuilder.modifyValue, configDataBuilder.valueChanged)
		this.configData[configData.key] = configData
		if (configData.description.isNotBlank()) {
			configData.allowedTypes.forEach {
				it.configSubCommands.forEach {
					auriel.autoCompletionHandler.addAutocompleteStrings("user", it, option = "key", configData.key)
				}
			}
		}
		return configData
	}
	
	fun modifyConfigValue(member: Member, key: String, builder: ConfigContextBuilder.() -> Unit): ConfigError {
		val configContextBuilder = ConfigContextBuilder()
		builder.invoke(configContextBuilder)
		
		requireNotNull(configContextBuilder.guild) { "Guild cannot be null" }
		requireNotNull(configContextBuilder.channel) { "Channel cannot be null" }
		requireNotNull(configContextBuilder.author) { "Author cannot be null" }
		requireNotNull(configContextBuilder.value) { "Value cannot be null" }
		
		val configData = configData[key] ?: return ConfigError.NOT_FOUND
		if (!configData.isAllowedType(configContextBuilder.value!!)) return ConfigError.INVALID_DATA_TYPE
		if (configData.permission == Permission.ADMINISTRATOR && !member.isWarren()) return ConfigError.NOT_FOUND
		if (!member.hasPermission(configData.permission)) return ConfigError.NO_PERMISSION
		
		val configContext = ConfigContext(auriel, configContextBuilder.guild!!, configContextBuilder.channel!!,
			configContextBuilder.author!!, configContextBuilder.value!!)
		if (configData.modifyValue.invoke(configContext, key)) {
			configData.valueChanged.invoke(configContext)
			configData.saveChanges.invoke(configContext)
		}
		return ConfigError.NONE
	}
	
	fun hasKey(key: String) = configData.containsKey(key)
	
	init {
		createGuildConfigData {
			key = "youtube:playlist-id"
			permission = Permission.MANAGE_SERVER
			description = "The ID of the playlist for general uploads for the YouTube channel to post notifications for."
			allowedTypes(ConfigDataType.STRING)
			modifyValue = { context, _ ->
				context.guild.a().youtubeAnnouncer.setPlaylistId(context.getAsString())
			}
		}
		createGuildConfigData {
			key = "youtube:message"
			permission = Permission.BAN_MEMBERS
			description = "The message used when a new YouTube video is posted by the bot.\n" +
					"Available placeholders:\n" +
					"  - %TITLE% - The title of the video\n" +
					"  - %LINK%, %URL% - The URL of the video"
			allowedTypes(ConfigDataType.STRING)
			modifyValue = { context, _ ->
				context.guild.a().youtubeAnnouncer.setMessage(context.getAsString())
			}
		}
		createGuildConfigData {
			key = "youtube:channel-id"
			permission = Permission.MANAGE_SERVER
			description = "The ID of the channel to post new YouTube video upload notifications in."
			allowedTypes(ConfigDataType.CHANNEL)
			modifyValue = { context, _ ->
				context.guild.a().youtubeAnnouncer.setChannelId(context.getAsChannel().id)
			}
		}
		createGuildConfigData {
			key = "clone:helpee-request-button"
			permission = Permission.BAN_MEMBERS
			description = "The text on the button for a user requesting help with Diablo Clone."
			defaultValue = "I need help w/ DClone"
			allowedTypes(ConfigDataType.STRING)
		}
		createGuildConfigData {
			key = "clone:helpee-cancel-button"
			permission = Permission.BAN_MEMBERS
			description = "The text on the button for a user cancelling their help request with Diablo Clone."
			defaultValue = "I no longer need help"
			allowedTypes(ConfigDataType.STRING)
		}
		createGuildConfigData {
			key = "clone:helper-begin-button"
			permission = Permission.BAN_MEMBERS
			description = "The text on the button for a user giving help with Diablo Clone."
			defaultValue = "Begin helping"
			allowedTypes(ConfigDataType.STRING)
		}
		createGuildConfigData {
			key = "clone:helper-mention-button"
			permission = Permission.BAN_MEMBERS
			description = "The text on the button for a user getting the user mention of the user they are helping with Diablo Clone."
			defaultValue = "Get mention"
			allowedTypes(ConfigDataType.STRING)
		}
		createGuildConfigData {
			key = "clone:helpee-message"
			permission = Permission.BAN_MEMBERS
			description = "The text on the bot post containing the buttons for where users request help with Diablo Clone.\n" +
					"Available placeholders:\n" +
					"  - %REMAINING% - The amount of people remaining in the queue.\n" +
					"  - %POSITION% - The position in the queue that the helper(s) are currently working on."
			defaultValue = "**ATTENTION, PLEASE READ IF YOU ARE NEW TO DCLONE OR REQUESTING HELP!**\n" +
					"Diablo Clone will spawn in the specified region in Hell difficulty only! You MUST be in a game before `Diablo Invades Sanctuary` happens.\n" +
					"If you plan to request help, please follow these steps:\n" +
					"- Create a lobby game, the quick play games are hard to join.\n" +
					"- Make sure the game is in Hell!\n" +
					"- Disable the level restriction! We may not be able to join your game if you forget this and then you're out of luck.\n" +
					"- Make sure the allowed player amount for the game is higher than 1, or else nobody can join you.\n" +
					"- Do not make the game name or password obvious. Thieves actively try to join common games to steal Annihilus's. Create an elaborate password.\n" +
					"- Click the button below once you are in the game and input your information to join the queue. Then wait patiently for a direct message indicating that your helper is on the way.\n" +
					"- Please do not spawn Diablo Clone yourself unless you are seriously intent on trying to kill him first. Helpers have their preferred spawn locations.\n" +
					"- No, Diablo Clone does **not** despawn under any circumstances. As long as you are in the game, he will always be there.\n\n" +
					"Currently assisting position #%POSITION% in queue."
			allowedTypes(ConfigDataType.STRING)
		}
		createGuildConfigData {
			key = "clone:helper-message"
			permission = Permission.BAN_MEMBERS
			description = "The text on the bot post containing the button for where users give help with Diablo Clone.\n" +
					"Available placeholders:\n" +
					"  - %REMAINING% - The amount of people remaining in the queue.\n" +
					"  - %POSITION% - The position in the queue that the helper(s) are currently working on."
			defaultValue = "**ATTENTION!**\n" +
					"Clicking the button will open up a form which will immediately start helping people. Do not click it if you are not ready to help people.\n" +
					"Upon clicking the button, you will be given the requester's information. Click `Submit` when you have finished helping them.\n" +
					"Do not click `Submit` before you have finished helping them. If you need out of the form temporarily, click `Cancel` or outside of the form.\n" +
					"Clicking the button again will return you to the same requester's information incase you need to view it again.\n\n" +
					"People remaining: %REMAINING%"
			allowedTypes(ConfigDataType.STRING)
		}
		createGuildConfigData {
			key = "guild:logging-channel"
			permission = Permission.MANAGE_SERVER
			description = "The ID of the channel to log bot events to."
			allowedTypes(ConfigDataType.CHANNEL)
		}
		createGuildConfigData {
			key = "guild:fallback-channel"
			permission = Permission.MANAGE_SERVER
			description = "The ID of the channel to send messages to if a user cannot be privately messaged."
			allowedTypes(ConfigDataType.CHANNEL)
		}
		createGuildConfigData {
			key = "tz:source"
			permission = Permission.ADMINISTRATOR
			description = "" // hide this option
			allowedTypes(ConfigDataType.CHANNEL)
			modifyValue = { context, _ ->
				auriel.guilds.tzTracker.setChannel(context.getAsChannel().id, false)
			}
			saveChanges = { auriel.guilds.tzTracker.saveData() }
		}
		createGuildConfigData {
			key = "tz:language"
			permission = Permission.MANAGE_SERVER
			description = "The language to use for the Terror Zone tracker."
			defaultValue = "enUS"
			allowedTypes(ConfigDataType.STRING)
		}
		createGuildConfigData {
			key = "tz:online"
			permission = Permission.MANAGE_SERVER
			description = "The message to send when the automated Terror Zone tracker comes back online."
			defaultValue = "The Terror Zone tracker is back online!"
			allowedTypes(ConfigDataType.STRING)
		}
		createGuildConfigData {
			key = "tz:offline"
			permission = Permission.MANAGE_SERVER
			description = "The message to send when the automated Terror Zone tracker goes offline."
			defaultValue = "The Terror Zone tracker is offline!"
			allowedTypes(ConfigDataType.STRING)
		}
		createChannelConfigData {
			key = "channel:max-message-age"
			permission = Permission.MANAGE_SERVER
			description = "The maximum age of a message in this channel before it is deleted."
			defaultValue = 0L
			allowedTypes(ConfigDataType.NUMBER)
			valueChanged = { context ->
				context.channel.a().startMessageAgeTimer()
			}
		}
		createChannelConfigData {
			key = "channel:message-age-interval"
			permission = Permission.MANAGE_SERVER
			description = "The interval to check messages' ages in this channel to be purged."
			defaultValue = 0L
			allowedTypes(ConfigDataType.NUMBER)
			valueChanged = { context ->
				context.channel.a().startMessageAgeTimer()
			}
		}
		createChannelConfigData {
			key = "channel:line-limit"
			permission = Permission.MANAGE_SERVER
			description = "The maximum amount of new lines allowed in a message in this channel."
			defaultValue = 2000
			allowedTypes(ConfigDataType.NUMBER)
		}
		createChannelConfigData {
			key = "channel:only-one-message"
			permission = Permission.BAN_MEMBERS
			description = "Only allow one message per user in this channel."
			defaultValue = false
			allowedTypes(ConfigDataType.BOOLEAN)
			valueChanged = { context ->
				context.channel.a().deleteAllButOneMessage()
			}
		}
		createChannelConfigData {
			key = "channel:allow-reposts"
			permission = Permission.BAN_MEMBERS
			description = "Allow the bot to repost censored messages in this channel."
			defaultValue = true
			allowedTypes(ConfigDataType.BOOLEAN)
		}
		createGuildConfigData {
			key = "guild:crosspost"
			permission = Permission.MANAGE_SERVER
			description = "Auto-publish messages posted in every announcement channel."
			defaultValue = false
			allowedTypes(ConfigDataType.BOOLEAN)
		}
		createGuildConfigData {
			key = "guild:vouch-cooldown"
			permission = Permission.MANAGE_SERVER
			description = "The cooldown in seconds that someone can give a vouch to another user."
			defaultValue = 3600L
			allowedTypes(ConfigDataType.NUMBER)
		}
		createUserConfigData {
			key = "user:language"
			permission = Permission.MANAGE_SERVER
			description = "The default language that users will use in this server."
			defaultValue = "enUS"
			allowedTypes(ConfigDataType.STRING)
		}
	}

}