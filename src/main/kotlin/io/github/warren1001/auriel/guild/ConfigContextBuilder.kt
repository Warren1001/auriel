package io.github.warren1001.auriel.guild

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

class ConfigContextBuilder {
	
	var guild: Guild? = null
	var channel: GuildMessageChannel? = null
	var author: User? = null
	var value: Any? = null
	
}