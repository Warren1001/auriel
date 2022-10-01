package io.github.warren1001.auriel.util.filter

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message

data class RepeatedSpamFilter(val name: String, val regexes: MutableSet<Regex>, val maxAmount: Int, val timeWindow: Long): Filter {
	
	private val messages = mutableMapOf<Member, RepeatedSpamLog>()
	
	override fun containsMatchIn(input: CharSequence) = regexes.all { it.containsMatchIn(input) }
	
	fun logSpam(author: Member, message: Message): Boolean {
		val log = messages.getOrPut(author) { RepeatedSpamLog(maxAmount, timeWindow) }
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
	
	override fun toString() = "RepeatedSpamFilter[name=$name,regexes=$regexes,maxAmount=$maxAmount,timeWindow=$timeWindow]"
	
	private class RepeatedSpamLog(private val maxAmount: Int, private val timeWindow: Long,
	                              val matchedMessages: MutableList<Message> = mutableListOf(), var lastTime: Long = System.currentTimeMillis()) {
		
		fun increase(message: Message): Boolean {
			matchedMessages.add(message)
			lastTime = System.currentTimeMillis()
			return matchedMessages.size >= maxAmount
		}
		
		fun purge() {
			matchedMessages.forEach { it.delete().queue() }
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
