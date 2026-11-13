package io.github.warren1001.auriel.guild

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.github.warren1001.auriel.Auriel
import io.github.warren1001.auriel.toDataObject
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateData

enum class ConfigDataType(val configSubCommands: List<String>, private val isType: (Any) -> Boolean) {
	
	STRING(listOf("string", "longstring"), { it is String }),
	CHANNEL(listOf("channel"), { it is GuildMessageChannel }),
	NUMBER(listOf("number"), { it is Number }),
	BOOLEAN(listOf("boolean"), { it is Boolean }),
	MESSAGE_CREATE_DATA(listOf("longstring"), { it is MessageCreateData });
	
	fun isType(value: Any) = isType.invoke(value)
	
}