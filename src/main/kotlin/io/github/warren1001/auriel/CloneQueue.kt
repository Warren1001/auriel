package io.github.warren1001.auriel

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.spec.MessageEditSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import kotlin.concurrent.timer

class CloneQueue(private val auriel: Auriel, val guildId: Snowflake) {
	
	private val startTime: Long = System.currentTimeMillis()
	private var completed: Int = 0
	private val queue: Queue<CloneData> = ConcurrentLinkedQueue()
	private val helperHelpee: ConcurrentMap<Snowflake, CloneData> = ConcurrentHashMap()
	private val helperPrivateMessage: ConcurrentMap<Snowflake, Message> = ConcurrentHashMap()
	
	private var helperMessage: Message? = null
	private var requestMessage: Message? = null
	
	private val currentlyInQueueTimer: Timer = timer("clone-${guildId.asString()}-timer", true, 1000 * 3, 1000 * 3) {
		val a = requestMessage?.edit(
			MessageEditSpec.builder().contentOrNull(
				"If you need help with Diablo Clone, (un)react with ✅.\n" +
						"I will private message you a prompt on how to join the queue. After you respond, you will wait until a helper joins your game.\n" +
						"You can (un)react again to see the estimated time remaining until someone can help you (rough estimate).\n\n" +
						"If you found your own help or killed Clone yourself, simply (un)react.\n\n" +
						"Players currently in queue: ${queue.size}"
			).build()
		)?.async()
		val b = helperMessage?.edit(
			MessageEditSpec.builder().contentOrNull(
				"If you are a helper, (un)react with ✅ to help people with their Diablo Clone.\n\n" +
						"Players currently in queue: ${queue.size}"
			).build()
		)?.async()
		Flux.merge(a, b).handleErrors(auriel, "clone-${guildId.asString()}-timer").subscribe()
	}
	
	fun addUserToQueue(userId: Snowflake, message: String): Boolean {
		if (isInQueue(userId)) return false
		queue.add(CloneData(userId, message.substring(0, message.length.coerceAtMost(500))))
		return true
	}
	
	fun removeUserFromQueue(id: Snowflake): Boolean {
		val a = queue.removeIf { it.userId == id }
		auriel.userManager.clearUserPrivateData(id)
		return a
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
		return channel.message(
			"If you need help with Diablo Clone, (un)react with ✅.\n" +
					"I will private message you a prompt on how to join the queue. After you respond, you will wait until a helper joins your game.\n" +
					"You can (un)react again to see the estimated time remaining until someone can help you (rough estimate).\n\n" +
					"If you found your own help or killed Clone yourself, simply (un)react.\n\n" +
					"Players currently in queue: ${queue.size}"
		).doOnSuccess { msg ->
			requestMessage = msg
			msg.addReaction(Emojis.CHECKMARK).subscribe()
			val ctx = ReactionMessageContext(msg.id)
			ctx.guildId = guildId
			ctx.channelId = msg.channelId
			ctx.addCtx = "clone-${guildId.asString()}-request"
			auriel.reactionMessageHandler.registerReactionMessage(
				ctx, ReactionHandler(Emojis.CHECKMARK,
					{ event -> sendHelpeeMessage(event.userId, event.user) },
					{ event -> sendHelpeeMessage(event.userId, event.user) })
			)
		}
	}
	
	private fun sendHelpeeMessage(userId: Snowflake, userMono: Mono<User>): Mono<Message> {
		return if (isInQueue(userId)) {
			removeUserFromQueue(userId)
			userMono.flatMap { it.dm("You have been removed from the queue for Diablo Clone help in MrLlamaSC's Discord.") }
		} else {
			auriel.userManager.setUserPrivateData(userId, "clone-${guildId.asString()}")
			userMono.flatMap {
				it.dm(
					"Hello, you have requested to be added to the queue for Diablo Clone help in MrLlamaSC's Discord.\n\n" +
							"**Please respond with your game name, password, and any additional information you'd like**.\n" +
							"If you responded with a random message before reading this, remove yourself from the queue and then add yourself to the queue again.\n\n" +
							"Here is a suggested format: `myCloneGameName///myPassword I did not spawn him yet. I can only access Act 1.`\n" +
							"or: `MyBattlenetAccount#0000 I made a non-lobby game, I did not spawn him yet.`"
				)
			}
		}
	}
	
	fun sendHelperMessage(channel: GuildMessageChannel): Mono<Message> {
		return channel.message(
			"If you are a helper, (un)react with ✅ to help people with their Diablo Clone.\n\n" +
					"Players currently in queue: ${queue.size}"
		).doOnSuccess { msg1 ->
			helperMessage = msg1
			msg1.addReaction(Emojis.CHECKMARK).subscribe()
			val ctx0 = ReactionMessageContext(msg1.id)
			ctx0.guildId = guildId
			ctx0.channelId = msg1.channelId
			ctx0.addCtx = "clone-${guildId.asString()}-helper"
			auriel.reactionMessageHandler.registerReactionMessage(
				ctx0, ReactionHandler(Emojis.CHECKMARK,
					{ event ->
						val member = event.member.orElseThrow()
						member.dm(
							"React with ✅ to get started with helping people with their Diablo Clone.\n" +
									"If you are not going to help, do not (un)react. You will mess with the queue."
						).doOnSuccess { msg2 ->
							msg2.addReaction(Emojis.CHECKMARK).subscribe()
							msg2.addReaction(Emojis.CANCEL).subscribe()
							val ctx = ReactionMessageContext(msg2.id)
							val userId = member.id
							ctx.userId = userId
							ctx.guildId = guildId
							ctx.channelId = msg2.channelId
							ctx.addCtx = "clone-${guildId.asString()}-helper"
							helperPrivateMessage[ctx.userId] = msg2
							auriel.reactionMessageHandler.registerReactionMessage(
								ctx,
								ReactionHandler(Emojis.CHECKMARK, { event1 -> editHelperMessage(event1.userId, event1.user, event1.message) },
									{ event1 -> editHelperMessage(event1.userId, event1.user, event1.message) }),
								ReactionHandler(Emojis.CANCEL, { event1 -> finishHelping(event1.userId, event1.message) },
									{ event1 -> finishHelping(event1.userId, event1.message) })
							)
						}
					})
			)
		}
	}
	
	private fun finishHelping(helperId: Snowflake, messageMono: Mono<Message>): Mono<Message> {
		var a: Mono<Message>? = null
		if (helperHelpee.containsKey(helperId)) {
			val cloneData = helperHelpee[helperId]!!
			removeUserFromQueue(cloneData.userId)
			a = auriel.gateway.getUserById(cloneData.userId).flatMap {
				it.updateData(auriel, guildId) { it.cloneData.completed++ }.then(it.dm(
					"<@${helperId.asString()}> has finished helping you. If this was a mistake, please rejoin the queue by (un)reacting twice (there is a 3 second " +
							"cooldown between reactions) to the message in the Discord channel."
				).doOnSuccess { helperHelpee.remove(helperId) })
			}
		}
		helperPrivateMessage.remove(helperId)
		auriel.reactionMessageHandler.removeIf {
			//println("Checking ${it.key}")
			val aa = "clone-${guildId.asString()}-helper"
			val a = it.key.addCtx == aa
			val b = it.key.userId == helperId
			//println("a=$a b=$b aa=$aa helperId=$helperId")
			return@removeIf a && b
		}
		val b = messageMono.flatMap {
			it.edit(
				MessageEditSpec.builder().contentOrNull(
					"Thanks for the help :) You have helped kill ${
						auriel.gateway.getMemberById(guildId, helperId).flatMap { it.getData(auriel) }.block()!!.cloneData.completed
					} Diablo Clones in total.\n" +
							"(Un)react to the original message if you wish to help again."
				).build()
			)
		}.doOnSuccess {
			it.removeSelfReaction(Emojis.CHECKMARK).subscribe()
			it.removeSelfReaction(Emojis.CANCEL).subscribe()
		}
		return if (a == null) b else a.then(b)
	}
	
	private fun editHelperMessage(helperId: Snowflake, userMono: Mono<User>, messageMono: Mono<Message>): Mono<Message> {
		var a: Mono<Message>? = null
		if (helperHelpee.containsKey(helperId)) {
			val cloneData = helperHelpee[helperId]!!
			removeUserFromQueue(cloneData.userId)
			a = auriel.gateway.getUserById(cloneData.userId).flatMap {
				it.updateData(auriel, guildId) { it.cloneData.completed++ }.then(it.dm(
					"<@${helperId.asString()}> has finished helping you. If this was a mistake, please rejoin the queue by (un)reacting twice (there is a 3 second " +
							"cooldown between reactions) to the message in the Discord channel."
				).doOnSuccess { helperHelpee.remove(helperId) })
			}
		}
		val data = getNextUser()
		if (data != null) {
			helperHelpee[helperId] = data
			auriel.reactionMessageHandler.removeIf { it.key.addCtx == "clone-${guildId.asString()}-request" && it.key.userId == data.userId }
		}
		val b = userMono.flatMap { it.asMember(guildId) }.flatMap { member ->
			messageMono.flatMap {
				it.edit(
					MessageEditSpec.builder().contentOrNull(
						(if (data != null) "From <@${data.userId.asString()}>: ${data.message}\n" else "The queue is currently empty, (un)react to check again.\n") +
								"You have helped kill ${member.getData(auriel).block()!!.cloneData.completed} Diablo Clones.\n" +
								"(Un)react to help the next person. There is a cooldown of 3 seconds to prevent double clicks."
					).build()
				)
			}
		}
		a = if (a == null) b else a.then(b)
		if (data != null) a = a.then(auriel.gateway.getMemberById(guildId, helperHelpee[helperId]!!.userId).flatMap { it.dm("<@${helperId.asString()}> is your helper and is on the way!") })
		return a
	}
	
	fun getPosition(userId: Snowflake): Int {
		return queue.indexOfFirst { it.userId == userId } + 1
	}
	
	fun getTimeRemainingMessage(id: Snowflake): String {
		val time = getRemainingTime(id)
		return if (time == -1L)
			"Not enough data. Try again in a minute." else "${time / 1000} seconds remaining."
	}
	
	fun destroy(): Mono<out Any> {
		currentlyInQueueTimer.cancel()
		auriel.reactionMessageHandler.removeIf { it.key.addCtx != null && it.key.addCtx!!.startsWith("clone-${guildId.asString()}") }
		queue.clear()
		helperHelpee.clear()
		Flux.merge(helperPrivateMessage.values.map { it.del() }).subscribe()
		helperPrivateMessage.clear()
		return Mono.`when`(helperMessage?.del(), requestMessage?.del())
	}
	
}