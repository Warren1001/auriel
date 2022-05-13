package io.github.warren1001.auriel.command

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.user.Permission
import io.github.warren1001.auriel.util.Filter
import reactor.core.publisher.Mono
import java.time.Duration

class CommandManager(private val auriel: Auriel) {
	
	var prefix = "!"
	
	private val commands = mutableMapOf<String, Command>()
	
	init {
		registerCommand("ping") { it.event.message.reply("Pong!", true, Duration.ofSeconds(5L)) }
		registerCommand("setperm") { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ')
			ctx.event.guild.flatMap { it.getMemberById(Snowflake.of(args[0])) }.flatMap { user -> user.updateData(auriel) { it.permission = args[1].toInt() } }
				.ackIfSuccess(ctx.event.message)
		}
		registerCommand("oom", permission = Permission.MODERATOR) { ctx ->
			if (ctx.arguments.isNotEmpty()) {
				val value = ctx.arguments.toBoolean()
				ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.setOnlyOneMessage(value) } }.ackIfSuccess(ctx.event.message)
			} else NOTHING
		}
		registerCommand("ma") { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ')
			val duration = args[0].toLong()
			val interval = args[1].toLong()
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.setMaxMessageAge(duration, interval) } }.ackIfSuccess(ctx.event.message)
		}
		registerCommand("filter") { ctx ->
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap {
				it.updateData(auriel) {
					it.addFilter(Filter(Regex(ctx.arguments, setOf(RegexOption.IGNORE_CASE, RegexOption.LITERAL))))
				}
			}.ackIfSuccess(ctx.event.message)
		}
		registerCommand("gfilter") { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ', limit = 2)
			if (args[0].equals("add", true)) {
				val arg1 = args[1]
				if (arg1.contains(' ')) {
					val args1 = arg1.split(' ', limit = 2)
					ctx.event.guild.flatMap { it.updateData(auriel) { it.filters.add(Filter(Regex(args[0], setOf(RegexOption.IGNORE_CASE, RegexOption.LITERAL)), args1[1])) } }
						.ackIfSuccess(ctx.event.message)
				} else {
					ctx.event.guild.flatMap { it.updateData(auriel) { it.filters.add(Filter(Regex(arg, setOf(RegexOption.IGNORE_CASE, RegexOption.LITERAL)))) } }.ackIfSuccess(ctx.event.message)
				}
			} else if (args[0].equals("remove", true)) {
				ctx.event.guild.flatMap { it.updateData(auriel) { it.filters.removeIf { it.replacement.equals(args[1], true) } } }.ackIfSuccess(ctx.event.message)
			} else NOTHING
		}
		registerCommand("togglerepost") { ctx ->
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.toggleRepost() } }
				.ackIfSuccess(ctx.event.message)
		}
		registerCommand("linelimit") { ctx ->
			val arg = ctx.arguments
			val amount = arg.toInt()
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.setLineLimit(amount) } }
				.ackIfSuccess(ctx.event.message)
		}
		registerCommand("muteroleid") { ctx ->
			val id = Snowflake.of(ctx.arguments)
			ctx.event.guild.flatMap { it.updateData(auriel) { it.muteRoleId = id } }.ackIfSuccess(ctx.event.message)
		}
		registerCommand("logchannelid") { ctx ->
			val id = Snowflake.of(ctx.arguments)
			ctx.event.guild.flatMap { it.updateData(auriel) { it.logChannelId = id } }.ackIfSuccess(ctx.event.message)
		}
		registerCommand("youtube") { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ')
			if (args[0].equals("message", true)) {
				ctx.event.guild.flatMap {
					val guildManager = auriel.getGuildManager(it.id)
					if (guildManager.youtubeAnnouncer != null) {
						guildManager.youtubeAnnouncer!!.data.message = args.subList(1, args.size).joinToString(separator = " ")
						auriel.updateYoutubeData(guildManager.youtubeAnnouncer!!.data)
						ctx.event.message.reply("Set message to '${guildManager.youtubeAnnouncer!!.data.message}'")
					} else {
						ctx.event.message.reply("YouTube announcements have not been setup.")
					}
				}
			} else {
				ctx.event.guild.map { auriel.getGuildManager(it.id).startYoutubeAnnouncer(it.id, args[0], Snowflake.of(args[1]), Snowflake.of(args[2]), Snowflake.of(args[3])) }
					.ackIfSuccess(ctx.event.message)
			}
		}
		registerCommand("mod") { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ')
			if (args.size == 2) {
				val member = ctx.event.member.orElseThrow()
				val targetId = Snowflake.of(args[1])
				if (args[0].equals("add", true)) {
					auriel.gateway.getGuildById(ctx.event.guildId.orElseThrow()).flatMap { it.getMemberById(targetId) }.flatMap { it.updateData(auriel) { it.permission = Permission.MODERATOR } }
						.ackIfSuccess(ctx.event.message)
				} else if (args[0].equals("remove", true)) {
					auriel.gateway.getGuildById(ctx.event.guildId.orElseThrow()).flatMap { it.getMemberById(targetId) }.flatMap { it.updateData(auriel) { it.permission = Permission.EVERYONE } }
						.ackIfSuccess(ctx.event.message)
				} else NOTHING
			} else NOTHING
		}
		registerCommand("startclone") { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ')
			val guildId = ctx.event.guildId.orElseThrow()
			val helperChannelId = Snowflake.of(args[0])
			val requestChannelId = Snowflake.of(args[1])
			auriel.getGuildManager(guildId).startCloneQueue(ctx.event.message, helperChannelId, requestChannelId).ackIfSuccess(ctx.event.message)
		}
		registerCommand("endclone") { ctx ->
			auriel.getGuildManager(ctx.event.guildId.orElseThrow()).stopCloneQueue()
			ctx.event.message.acknowledge()
		}
		registerCommand("rolegivemsg", permission = Permission.MODERATOR) { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ', limit = 2)
			return@registerCommand if (args.size == 2 && args[1].isNotEmpty()) {
				val guildId = ctx.event.guildId.orElseThrow()
				val roleId = Snowflake.of(args[0])
				val message = args[1]
				auriel.getGuildManager(guildId).sendRoleGiveMsg(ctx.event.message.channelId, roleId, message).ackIfSuccess(ctx.event.message)
			} else {
				ctx.event.message.reply("Usage: `rolegivemsg <role id> <message>`")
			}
			
		}
	}
	
	fun registerCommand(name: String, permission: Int = Permission.OWNER, action: (CommandContext) -> Mono<out Any>) {
		commands[name.lowercase()] = Command(auriel, name.lowercase(), permission, action)
	}
	
	fun handle(event: MessageCreateEvent): Mono<out Any> {
		
		if (!isCommand(event)) return NOTHING
		
		val message = event.message
		val content = message.content.substring(prefix.length)
		val args = if (content.contains(' ')) content.split(' ', limit = 2) else mutableListOf(content)
		val commandName = args[0].lowercase()
		
		if (!commands.containsKey(commandName)) return NOTHING
		
		val command = commands[commandName]!!
		val arguments = if (args.size == 1) "" else args[1]
		
		return message.authorAsMember.flatMap { it.getData(auriel) }.flatMap { if (it.hasPermission(command.permission)) command.action.invoke(CommandContext(event, arguments)) else NOTHING }
	}
	
	fun isCommand(event: MessageCreateEvent) = event.message.content.startsWith(prefix, ignoreCase = true)
	
	
}