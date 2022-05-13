package io.github.warren1001.auriel

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.entity.channel.PrivateChannel
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec
import discord4j.core.spec.MessageCreateSpec
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class CloneQueue(private val auriel: Auriel, val guildId: Snowflake) {
	
	private val buttonList = mutableListOf(
		Button.primary("${guildId.asString()}-a", "I need help with Diablo Clone"), Button.danger(
			"${guildId.asString()}-r", "I no longer need help with Diablo Clone"
		)
	)
	private val nextButton = Button.primary("${guildId.asString()}-n", "Next")
	private val helperButton = Button.primary("${guildId.asString()}-h", "Click me to help")
	
	private val startTime: Long = System.currentTimeMillis()
	private var completed: Int = 0
	private val queue: Queue<CloneData> = ConcurrentLinkedQueue()
	
	init {
		auriel.gateway.on(ButtonInteractionEvent::class.java).handleErrors(auriel).filter { it.customId.startsWith(guildId.asString()) }.flatMap {
			
			val member = it.interaction.member.orElseThrow()
			
			return@flatMap if (it.customId.endsWith("-a")) {
				
				if (isInQueue(member.id) || auriel.userManager.getUserPrivateData(member.id) != null) {
					if (auriel.userManager.getUserPrivateData(member.id) != null) {
						it.reply("Check your private messages!").withEphemeral(true)
					} else {
						it.reply("Estimated remaining time: ${getTimeRemainingMessage(member.id)}").withEphemeral(true)
					}
				} else {
					auriel.userManager.setUserPrivateData(member.id, "clone-${member.guildId.asString()}")
					val a = member.dm(
						"Hello, you have requested to be added to the queue for Diablo Clone help in MrLlamaSC's Discord.\n\n" +
								"Please respond with your game name, password, and any additional information you'd like.\n" +
								"If you do not provide your game name, your helper will not be able to help you.\n" +
								"If you responded with a random message before reading this, remove yourself from the queue and then add yourself to the queue again.\n\n" +
								"All you have to do is simply type a message to me with info to do so.\n" +
								"Here is a suggested format: myCloneGameName///myPassword I did not spawn him yet. I can only access Act 1."
					)
					val b = it.reply("Check your private messages to be added to the queue!").withEphemeral(true)
					
					Mono.`when`(a, b)
				}
			} else if (it.customId.endsWith("-r")) {
				
				//val member = it.interaction.member.orElseThrow()
				removeUserFromQueue(member.id)
				it.reply("You have been removed from the queue for Diablo Clone.").withEphemeral(true)
			} else {
				val data = getNextUser()
				if (data == null) {
					it.reply(InteractionApplicationCommandCallbackSpec.builder().content(
						"The queue is currently empty, press the button to check again. You can see the active queue amount in the clone-help channel."
					).addComponent(ActionRow.of(nextButton)).build().withEphemeral(true))
				} else {
					//val user = data.userData.getUser(auriel)
					val a = it.message.orElseThrow().del().then(it.reply(InteractionApplicationCommandCallbackSpec.builder().content(
						"From <@${data.userId.asString()}>: ${data.message}"
					).addComponent(ActionRow.of(nextButton)).build())).async()
					val b = it.message.orElseThrow().channel.ofType(PrivateChannel::class.java).flatMap {
						it.createMessage("${it.recipients.first().mention} is your helper and is on the way! Donations are not required but some helpers might ask for rejuvs")
					}.async()
					//val b = user.dm("${member.mention} is your helper and is on the way!").async()
					Mono.`when`(a, b)
				}
				
			}
			
		}.subscribe()
	}
	
	fun addUserToQueue(userId: Snowflake, message: String): Boolean {
		if (isInQueue(userId)) return false
		queue.add(CloneData(userId, message))
		auriel.userManager.clearUserPrivateData(userId)
		return true
	}
	
	fun removeUserFromQueue(id: Snowflake) {
		queue.removeIf { it.userId == id }
		auriel.userManager.clearUserPrivateData(id)
	}
	
	fun isInQueue(id: Snowflake): Boolean {
		return queue.stream().anyMatch { it.userId == id }
	}
	
	fun getNextUser(): CloneData? {
		completed++
		return queue.poll()
	}
	
	fun getRemainingTime(id: Snowflake): Long {
		val position = queue.indexOfFirst { it.userId == id } + 1
		return if (position == 0 || completed < 10) -1 else ((System.currentTimeMillis() - startTime) / completed) * position
	}
	
	fun sendJoinMessage(channel: GuildMessageChannel): Mono<Message> {
		return channel.createMessage(
			MessageCreateSpec.builder().content(
				"If you need help with Diablo Clone, click the button labeled `I need help with Diablo Clone`.\n" +
						"I will private message you a prompt on how to join the queue, after you respond, you will then wait until a helper joins your game.\n" +
						"You can click the button again to see the estimated time remaining until someone can help you (very estimate).\n\n" +
						"If you found your own help or killed Clone yourself, click the button labeled `I no longer need help with Diablo Clone`."
			).addComponent(ActionRow.of(buttonList)).build()
		)
	}
	
	fun sendHelperMessage(channel: GuildMessageChannel): Mono<Message> {
		return channel.createMessage(
			MessageCreateSpec.builder().content(
				"If you are a helper, click the button to begin helping people. If you are not going to help, do not click the button. You will mess with the queue."
			).addComponent(ActionRow.of(helperButton)).build()
		)
	}
	
	fun getTimeRemainingMessage(id: Snowflake): String {
		val time = getRemainingTime(id)
		return if (time == -1L)
			"Not enough data. Try again in a few minutes." else "${time / 1000} seconds remaining."
	}
	
	fun destroy() {
		queue.clear()
		completed = 0
	}
	
}