package io.github.warren1001.auriel

import com.fasterxml.jackson.databind.module.SimpleModule
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.result.UpdateResult
import com.mongodb.reactivestreams.client.MongoCollection
import com.mongodb.reactivestreams.client.MongoDatabase
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.guild.BanEvent
import discord4j.core.event.domain.guild.MemberLeaveEvent
import discord4j.core.event.domain.guild.UnbanEvent
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.event.domain.lifecycle.DisconnectEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.*
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.PrivateChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.MessageCreateMono
import discord4j.core.spec.MessageCreateSpec
import discord4j.gateway.intent.IntentSet
import io.github.warren1001.auriel.channel.ChannelData
import io.github.warren1001.auriel.channel.ChannelManager
import io.github.warren1001.auriel.command.CommandManager
import io.github.warren1001.auriel.guild.GuildData
import io.github.warren1001.auriel.guild.GuildManager
import io.github.warren1001.auriel.listener.*
import io.github.warren1001.auriel.reaction.Emojis
import io.github.warren1001.auriel.serialization.SnowflakeDeserialization
import io.github.warren1001.auriel.serialization.SnowflakeSerialization
import io.github.warren1001.auriel.user.UserData
import io.github.warren1001.auriel.user.UserManager
import io.github.warren1001.auriel.youtube.YoutubeData
import io.github.warren1001.auriel.youtube.YoutubeManager
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.reactor.updateOne
import org.litote.kmongo.util.KMongoConfiguration
import org.litote.kmongo.util.UpdateConfiguration
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.lang.Thread.sleep
import java.time.Duration
import java.util.*
import kotlin.concurrent.thread

class Auriel(val gateway: GatewayDiscordClient, youtubeKey: String) {
	
	val commandManager = CommandManager(this)
	
	private val mongo = KMongo.createClient()
	private val guildMessageCreateHandler = GuildMessageCreateHandler(this)
	private val privateMessageCreateHandler = PrivateMessageCreateHandler(this)
	private val messageUpdateHandler = MessageUpdateHandler()
	private val buttonClickHandler = ButtonClickHandler(this)
	private val modalSubmitHandler = ModalSubmitInteractionHandler()
	private val guildLoggerListener = GuildLoggerListener(this)
	
	private val guildManagers = mutableMapOf<Snowflake, GuildManager>()
	
	val database: MongoDatabase = mongo.getDatabase("test")
	val guildDataCollection: MongoCollection<GuildData> = database.getCollection("guilds", GuildData::class.java)
	val channelDataCollection: MongoCollection<ChannelData> = database.getCollection("channels", ChannelData::class.java)
	val youtubeDataCollection: MongoCollection<YoutubeData> = database.getCollection("youtube", YoutubeData::class.java)
	val youtubeManager: YoutubeManager = YoutubeManager(this, youtubeKey)
	val userManager: UserManager = UserManager(this)
	val reactionMessageHandler = ReactionMessageHandler(this)
	val warren: User = gateway.getUserById(WARREN_ID).blockOptional().orElseThrow()
	
	init {
		UpdateConfiguration.updateOnlyNotNullProperties = true
		val module = SimpleModule("AurielSerializationObjects")
		module.addSerializer(Snowflake::class.java, SnowflakeSerialization())
		module.addDeserializer(Snowflake::class.java, SnowflakeDeserialization())
		KMongoConfiguration.registerBsonModule(module)
		gateway.guilds.map { guildManagers[it.id] = GuildManager(this, it.id) }.handleErrors(this).blockLast()
	}
	
	fun registerListeners(): Mono<out Any> {
		// todo optimize
		val guildCreateListener = gateway.on(MessageCreateEvent::class.java).filter { it.message.channel.block() is GuildMessageChannel }.flatMap { guildMessageCreateHandler.handle(it) }
			.handleErrors(this, "guildCreateListener")
		val privateCreateListener = gateway.on(MessageCreateEvent::class.java).filter { it.message.channel.block() is PrivateChannel }.flatMap { privateMessageCreateHandler.handle(it) }
			.handleErrors(this, "privateCreateListener")
		val editListener = gateway.on(MessageUpdateEvent::class.java).flatMap { messageUpdateHandler.handle(it) }.handleErrors(this, "editListener")
		val deleteListener = gateway.on(MessageDeleteEvent::class.java).flatMap { event ->
			event.channel.ofType(GuildMessageChannel::class.java)
				.map { getGuildManager(it.guildId).getChannelManager(it.id) }.filter { event.message.isPresent && event.message.get().author.isPresent }.map {
					it.clearUserLastMessage(event.message.get().author.get().id, event.message.get().id)
				}
		}.handleErrors(this, "deleteListener")
		val reactionAddListener =
			gateway.on(ReactionAddEvent::class.java).filter { it.userId != gateway.selfId }.flatMap { reactionMessageHandler.onReactionAdd(it) }.handleErrors(this, "reactionAddListener")
		val reactionRemoveListener = gateway.on(ReactionRemoveEvent::class.java).flatMap { reactionMessageHandler.onReactionRemove(it) }.handleErrors(this, "reactionRemoveListener")
		val buttonListener = gateway.on(ButtonInteractionEvent::class.java).flatMap { buttonClickHandler.handle(it) }.handleErrors(this, "buttonListener")
		val modalSubmitListener = gateway.on(ModalSubmitInteractionEvent::class.java).flatMap { modalSubmitHandler.handle(it) }.handleErrors(this, "modalSubmitListener")
		val banListener = gateway.on(BanEvent::class.java).flatMap { guildLoggerListener.handle(it) }.handleErrors(this, "banListener")
		val unbanListener = gateway.on(UnbanEvent::class.java).flatMap { guildLoggerListener.handle(it) }.handleErrors(this, "unbanListener")
		val kickListener = gateway.on(MemberLeaveEvent::class.java).flatMap { guildLoggerListener.handle(it) }.handleErrors(this, "kickListener")
		return Mono.`when`(guildCreateListener, privateCreateListener, editListener, deleteListener, reactionAddListener, reactionRemoveListener, buttonListener, modalSubmitListener, banListener,
			unbanListener, kickListener).handleErrors(this, "aurielCombinedMono")
	}
	
	fun getGuildManager(guildId: Snowflake): GuildManager {
		return guildManagers[guildId] ?: throw IllegalArgumentException("Could not find Guild with ID '${guildId.asString()}'")
	}
	
	fun getGuildManagers() = guildManagers.values.toSet()
	
	fun updateGuildData(data: GuildData) = guildDataCollection.updateOne(data, UpdateOptions().upsert(true))
	
	fun updateChannelData(data: ChannelData) = channelDataCollection.updateOne(data, UpdateOptions().upsert(true))
	
	fun updateYoutubeData(data: YoutubeData) {
		youtubeDataCollection.updateOne(data, UpdateOptions().upsert(true)).subscribe()
	}
	
}

fun main(args: Array<String>) {
	
	val token = args[0]
	val youtube = args[1]
	
	var auriel: Auriel? = null
	
	
	
	DiscordClient.create(token).gateway().setEnabledIntents(IntentSet.all()).withGateway { gateway ->
		
		thread(isDaemon = true, start = true) {
			val scanner = Scanner(System.`in`)
			while (true) {
				val nextLine = scanner.nextLine()
				if (nextLine.lowercase() == "stop") {
					gateway.logout().doOnSuccess { println("Bye!") }.subscribe()
				}
				sleep(100L)
			}
		}
		val a = gateway.on(ReadyEvent::class.java).flatMap {
			auriel = Auriel(gateway, youtube)
			val a = auriel!!.registerListeners()
			println("Ready!")
			a
		}
		val b = gateway.on(DisconnectEvent::class.java).doOnNext { println("Disconnected") }
		Flux.merge(a, b)
	}.onErrorContinue { t, a ->
		println("[ERROR] withGateway")
		t.printStackTrace()
		if (auriel != null) auriel!!.warren.dm("Associated object: $a\nwithGateway - ${t.javaClass.name}: ${t.message}\n${t.stackTrace.joinToString("\n")}").subscribe()
	}.block()
	
}

val WARREN_ID: Snowflake = Snowflake.of(164118147073310721L)
val NOTHING: Mono<Void> = Mono.empty()

//private val deletedMessages: LoadingCache<Snowflake, Any> = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build(CacheLoader.from { _: Snowflake? -> EMPTY })

fun Message.deletedReply(message: String): Mono<Message> = channel.flatMap { it.createMessage("${author.orElseThrow().mention}, $message") }

fun Message.reply(message: String, delete: Boolean = false, duration: Duration = Duration.ZERO): Mono<Message> {
	//if (isDeleted()) return deletedReply(message)
	var messageMono: Mono<Message> = if (delete) deletedReply(message).async() else channel.flatMap {
		it.message(
			MessageCreateSpec.builder().content(message).messageReference(id).build()
		)
	}
	if (delete) messageMono = del().then(messageMono)
	if (duration != Duration.ZERO) {
		messageMono = messageMono.doOnSuccess { it.del().delaySubscription(duration).subscribe() }
	}
	return messageMono
}

/*fun Message.reply(auriel: Auriel, message: String, deleteReason: String, duration: Duration = Duration.ZERO): Mono<out Any> {
	if (isDeleted()) return deletedReply(message)
	val messageMono = deletedReply(message)
	return if (duration != Duration.ZERO) {
		Mono.`when`(delAndLog(auriel, deleteReason).async(), messageMono.flatMap { msg ->
			Mono.just(msg).flatMap { it.del() }.delaySubscription(duration)
		})
	} else Mono.`when`(delAndLog(auriel, deleteReason).async(), messageMono.async())
}*/

//fun Message.isDeleted(): Boolean {
//	return deletedMessages.asMap().containsKey(id)
//}

fun Message.delAndLog(auriel: Auriel, reason: String = "", channel: GuildMessageChannel? = null, repostId: Snowflake? = null): Mono<Void> {
	//if (isDeleted()) return NOTHING
	val logMono: Mono<out Any> = if (channel == null) getChannel().ofType(GuildMessageChannel::class.java).flatMap {
		auriel.getGuildManager(guildId.orElseThrow()).guildLogger.logMessageDelete(author.orElseThrow(), it, "Deleted Message", reason, content, repostId)
	} else auriel.getGuildManager(guildId.orElseThrow()).guildLogger.logMessageDelete(author.orElseThrow(), channel, "Deleted Message", reason, content, repostId)
	return Flux.merge(del(reason), logMono).then()
}

fun Message.del(reason: String? = null): Mono<Void> {
	//return if (isDeleted()) return NOTHING else delete(reason).doOnSuccess { deletedMessages.put(id, EMPTY) }
	return delete(reason)
}

fun Message.acknowledge(emoji: ReactionEmoji = Emojis.CHECKMARK): Mono<Void> = addReaction(emoji)

fun <T> Mono<out T>.ackIfSuccess(message: Message, emoji: ReactionEmoji = Emojis.CHECKMARK): Mono<out T> = doOnSuccess { message.acknowledge(emoji).subscribe() }

//fun Snowflake.isDeletedMessage(): Boolean {
//	return deletedMessages.asMap().containsKey(this)
//}

fun <T> Mono<T>.async(): Mono<T> = subscribeOn(Schedulers.boundedElastic())

fun <T> Mono<T>.handleErrors(auriel: Auriel, ref: String = ""): Mono<T> = onErrorContinue { t, a ->
	println(if (ref.isBlank()) "yes you handled the following error" else "yes you handled the following error - $ref")
	//t.printStackTrace()
	auriel.warren.dm("Associated object: $a\n${if (ref.isBlank()) "" else "$ref - "}${t.javaClass.name}: ${t.message}\n${t.stackTrace.joinToString("\n")}").subscribe()
}

fun <T> Flux<T>.async(): Flux<T> = subscribeOn(Schedulers.boundedElastic())

fun <T> Flux<T>.handleErrors(auriel: Auriel, ref: String = ""): Flux<T> = onErrorContinue { t, a ->
	println(if (ref.isBlank()) "yes you handled the following error" else "yes you handled the following error - $ref")
	//t.printStackTrace()
	auriel.warren.dm("Associated object: $a\n${if (ref.isBlank()) "" else "$ref - "}${t.javaClass.name}: ${t.message}\n${t.stackTrace.joinToString("\n")}").subscribe()
}

@Deprecated("Use getDataSync(Auriel)", ReplaceWith("getDataSync(auriel)"))
fun Member.getData(auriel: Auriel) = auriel.getGuildManager(guildId).userDataManager.getData(id)

@Deprecated("Use getDataSync(Auriel)", ReplaceWith("getDataSync(auriel)"))
fun Member.updateData(auriel: Auriel, changes: (UserData) -> Unit): Mono<UpdateResult> = getData(auriel).flatMap {
	changes.invoke(it)
	it.update(auriel)
}

fun Member.getDataSync(auriel: Auriel) = auriel.getGuildManager(guildId).userDataManager.getDataSync(id)

@Deprecated("Use getDataSync(Auriel, Snowflake)", ReplaceWith("getDataSync(auriel, id)"))
fun User.getData(auriel: Auriel, guildId: Snowflake) = auriel.getGuildManager(guildId).userDataManager.getData(id)

fun User.getDataSync(auriel: Auriel, guildId: Snowflake) = auriel.getGuildManager(guildId).userDataManager.getDataSync(id)

@Deprecated("Use getDataSync(Auriel)", ReplaceWith("getDataSync(auriel, id)"))
fun User.updateData(auriel: Auriel, guildId: Snowflake, changes: (UserData) -> Unit): Mono<UpdateResult> = getData(auriel, guildId).flatMap {
	changes.invoke(it)
	it.update(auriel)
}

fun Guild.getData(auriel: Auriel): Mono<GuildData> = Mono.just(auriel.getGuildManager(id).guildData)

fun Guild.updateData(auriel: Auriel, changes: (GuildData) -> Unit): Mono<UpdateResult> = getData(auriel).flatMap {
	changes.invoke(it)
	auriel.updateGuildData(it)
}

fun GuildMessageChannel.getManager(auriel: Auriel) = auriel.getGuildManager(guildId).getChannelManager(id)

fun GuildMessageChannel.updateData(auriel: Auriel, changes: (ChannelManager) -> Unit): Mono<UpdateResult> {
	val manager = getManager(auriel)
	changes.invoke(manager)
	return manager.updateChannelData()
}

fun MessageChannel.message(message: String): MessageCreateMono = createMessage(if (message.length > 2000) message.substring(0, 2000) else message)

fun MessageChannel.message(spec: MessageCreateSpec): Mono<Message> = createMessage(
	if (spec.isContentPresent && spec.content().get().length > 2000) spec.withContent(
		spec.content().get().substring(0, 2000)
	) else spec
)

fun User.dm(message: String): Mono<Message> = privateChannel.flatMap { it.message(message) }

fun User.dm(spec: MessageCreateSpec): Mono<Message> = privateChannel.flatMap { it.message(spec) }

fun String.truncate(length: Int): String = this.substring(0, this.length.coerceAtMost(length))




