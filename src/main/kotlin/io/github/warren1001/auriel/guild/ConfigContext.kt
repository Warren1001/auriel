package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.Auriel
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

data class ConfigContext(val auriel: Auriel, val event: SlashCommandInteractionEvent, val guild: Guild, val channel: GuildMessageChannel, val author: User, private val value: Any) {
	
	fun get() = value
	fun getAsString() = value as String
	fun getAsNumber() = value as Number
	fun getAsBoolean() = value as Boolean
	fun getAsChannel() = value as GuildMessageChannel
	
}