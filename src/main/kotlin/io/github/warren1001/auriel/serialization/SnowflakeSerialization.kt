package io.github.warren1001.auriel.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import discord4j.common.util.Snowflake

class SnowflakeSerialization: StdSerializer<Snowflake>(Snowflake::class.java) {
	
	override fun serialize(value: Snowflake, gen: JsonGenerator, provider: SerializerProvider) {
		//gen.writeNumber(value.asLong()) -- this just gives errors, dont know why
		gen.writeStartObject()
		gen.writeNumberField("id", value.asLong())
		gen.writeEndObject()
	}
	
}