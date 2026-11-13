package io.github.warren1001.auriel.serialize

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import io.github.warren1001.auriel.toMessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageCreateData

class MessageCreateDataDeserializer : JsonDeserializer<MessageCreateData>() {
	
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MessageCreateData = p.text.toMessageCreateData()
	
}