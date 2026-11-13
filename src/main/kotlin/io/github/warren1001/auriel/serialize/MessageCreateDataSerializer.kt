package io.github.warren1001.auriel.serialize

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.github.warren1001.auriel.toJSONString
import net.dv8tion.jda.api.utils.messages.MessageCreateData

class MessageCreateDataSerializer : JsonSerializer<MessageCreateData>() {
	
	override fun serialize(value: MessageCreateData, gen: JsonGenerator, serializers: SerializerProvider) = gen.writeString(value.toJSONString())
	
}