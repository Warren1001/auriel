package io.github.warren1001.auriel.reaction

import discord4j.common.util.Snowflake

data class ReactionMessageContext(val messageId: Snowflake) {
	
	var channelId: Snowflake? = null
	var userId: Snowflake? = null
	var guildId: Snowflake? = null
	var addCtx: String? = null
	
	override fun equals(other: Any?): Boolean {
		return (other is ReactionMessageContext) && (other.messageId == messageId)
	}
	
	override fun hashCode(): Int {
		return messageId.hashCode()
	}
	
	override fun toString(): String {
		return "ReactionMessageContext[messageId=${messageId},channelId=${channelId},userId=${userId},guildId=${guildId},addCtx=${addCtx}]"
	}
	
}