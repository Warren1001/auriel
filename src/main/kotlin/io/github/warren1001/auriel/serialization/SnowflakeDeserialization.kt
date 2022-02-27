package io.github.warren1001.auriel.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import discord4j.common.util.Snowflake

class SnowflakeDeserialization: StdDeserializer<Snowflake>(Snowflake::class.java) {
	
	override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Snowflake {
		var id = Snowflake.of(0)
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			val fieldName = parser.currentName
			parser.nextToken()
			if (fieldName == "id") {
				id = Snowflake.of(parser.valueAsLong)
			}
		}
		return id
	}
	
}