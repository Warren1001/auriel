package io.github.warren1001.auriel.util.filter

import io.github.warren1001.auriel.queue_
import io.github.warren1001.auriel.replaceOtherAlphabets
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message

data class SpamFilter(val name: String, val regexes: MutableSet<Regex>, val maxAmount: Int = 1, val timeWindow: Long = 5000L): Filter {
	
	private val messages = mutableMapOf<Member, SpamLog>()
	
	override fun containsMatchIn(input: String) = regexes.all { it.containsMatchIn(input.replaceOtherAlphabets()) }
	
	
	fun logSpam(author: Member, message: Message): Boolean {
		val log = messages.getOrPut(author) { SpamLog(maxAmount, timeWindow) }
		val isSpam = log.increase(message)
		if (isSpam) {
			log.purge()
			messages.remove(author)
		}
		return isSpam
	}
	
	fun clearMember(member: Member) {
		messages.remove(member)
	}
	
	fun checkDecrease(member: Member) {
		val log = messages[member] ?: return
		log.checkDecrease()
		if (log.matchedMessages.isEmpty()) messages.remove(member)
	}
	
	fun prettyPrint(): String {
		return "**$name**\n" +
				"Regexes:\n" +
				"${regexes.joinToString("\n") { "   `${it.pattern}`" }}\n" +
				"Max Amount: $maxAmount\n" +
				"Time Window: $timeWindow"
	}
	
	override fun toString() = "SpamFilter[name=$name,regexes=$regexes,maxAmount=$maxAmount,timeWindow=$timeWindow]"
	
	private class SpamLog(private val maxAmount: Int, private val timeWindow: Long,
	                              val matchedMessages: MutableList<Message> = mutableListOf(), var lastTime: Long = System.currentTimeMillis()) {
		
		fun increase(message: Message): Boolean {
			matchedMessages.add(message)
			lastTime = System.currentTimeMillis()
			return matchedMessages.size >= maxAmount
		}
		
		fun purge() {
			matchedMessages.forEach { it.delete().queue_() }
			matchedMessages.clear()
		}
		
		fun checkDecrease() {
			if (matchedMessages.size > 0 && System.currentTimeMillis() - lastTime > timeWindow) {
				val messagesToRemove = ((System.currentTimeMillis() - lastTime) / timeWindow).coerceAtMost(matchedMessages.size.toLong()).toInt()
				for (i in 0 until messagesToRemove) {
					matchedMessages.removeAt(0)
				}
			}
		}
		
	}
	
}
