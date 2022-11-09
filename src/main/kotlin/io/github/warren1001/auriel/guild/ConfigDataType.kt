package io.github.warren1001.auriel.guild

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

enum class ConfigDataType(val configSubCommands: List<String>, private val isType: (Any) -> Boolean) {
	
	STRING(listOf("string", "longstring"), { it is String }),
	CHANNEL(listOf("channel"), { it is GuildMessageChannel }),
	NUMBER(listOf("number"), { it is Number }),
	BOOLEAN(listOf("boolean"), { it is Boolean });
	
	fun isType(value: Any) = isType.invoke(value)
	
}