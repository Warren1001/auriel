package io.github.warren1001.auriel.d2.item

import net.dv8tion.jda.api.entities.MessageEmbed

abstract class Item(val items: Items, val names: Map<String, String>, val qlvl: Int, val rlvl: Int) {
	
	abstract fun createEmbed(lang: String): MessageEmbed
	
	abstract fun getString(key: String, lang: String): String?
	
}