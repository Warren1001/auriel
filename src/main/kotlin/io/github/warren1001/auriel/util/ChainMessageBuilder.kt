package io.github.warren1001.auriel.util

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateData

class ChainMessageBuilder<T, U> {
	
	var userId: String? = null
	var data: MutableMap<T, U> = mutableMapOf()
	var values: List<T>? = null
	var format: String? = null
	var finishMsg: String? = null
	var createMessage: ((MessageCreateData, (InteractionHook) -> Unit) -> Unit)? = null
	var parse: ((T, Message) -> U)? = null
	var validationMessage: String = ""
	var display: ((T) -> String)? = null
	var finished: ((MutableMap<T, U>) -> Unit)? = null
	
	fun build() = ChainMessage(data, values!!, format!!, finishMsg!!, parse!!, validationMessage, display!!, finished!!)
	
}