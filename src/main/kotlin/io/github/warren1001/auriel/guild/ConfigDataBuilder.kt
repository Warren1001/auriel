package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.a
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

class ConfigDataBuilder {
	
	var key: String = ""
	var defaultValue: Any? = null
	var permission: Permission = Permission.BAN_MEMBERS
	var description: String = ""
	var setDefault: (Auriel, String, Any) -> Unit = GUILD_SET_DEFAULT
	var valueChanged: (ConfigContext) -> Unit = {}
	var modifyValue: (ConfigContext, String) -> Boolean = GUILD_MODIFY_VALUE
	var saveChanges: (ConfigContext) -> Unit = GUILD_SAVE_CHANGES
	private val allowedTypes = mutableSetOf<ConfigDataType>()
	
	fun allowedTypes(type: ConfigDataType, vararg types: ConfigDataType = arrayOf()) {
		allowedTypes.add(type)
		allowedTypes.addAll(types)
	}
	
	fun allowedTypes(types: Collection<ConfigDataType>) = allowedTypes.addAll(types)
	
	fun allowedTypes() = allowedTypes
	
	companion object {
		
		val GUILD_SET_DEFAULT: (Auriel, String, Any) -> Unit = { auriel, key, value ->
			auriel.guilds.guildDataDefaults[key] = value
			auriel.guilds.forEachGuild { it.data.setDefault(key, value) }
		}
		val GUILD_MODIFY_VALUE: (ConfigContext, String) -> Boolean = { context, key ->
			var value = context.get()
			if (value is GuildMessageChannel) value = value.id
			context.guild.a().data.set(key, value)
		}
		val GUILD_SAVE_CHANGES: (ConfigContext) -> Unit = { it.guild.a().saveData() }
		
		val CHANNEL_SET_DEFAULT: (Auriel, String, Any) -> Unit = { auriel, key, value ->
			auriel.guilds.guildMessageChannelDataDefaults[key] = value
			auriel.guilds.forEachGuild { guild ->
				guild.forEachGuildMessageChannel {
					it.data.setDefault(key, value)
				}
			}
		}
		val CHANNEL_MODIFY_VALUE: (ConfigContext, String) -> Boolean = { context, key ->
			var value = context.get()
			if (value is GuildMessageChannel) value = value.id
			context.channel.a().data.set(key, value)
		}
		val CHANNEL_SAVE_CHANGES: (ConfigContext) -> Unit = { it.channel.a().saveData() }
		
		val USER_SET_DEFAULT: (Auriel, String, Any) -> Unit = { auriel, key, value ->
			auriel.guilds.userDataDefaults[key] = value
			auriel.guilds.forEachGuild { it.data.setUserDefault(key, value) }
		}
		val USER_MODIFY_VALUE: (ConfigContext, String) -> Boolean = { context, key ->
			var value = context.get()
			if (value is GuildMessageChannel) value = value.id
			context.author.a(context.guild.id).data.set(key, value)
		}
		val USER_SAVE_CHANGES: (ConfigContext) -> Unit = { it.author.a(it.guild.id).saveData() }
		
	}
	
}