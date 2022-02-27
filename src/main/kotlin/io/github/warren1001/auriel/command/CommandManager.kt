package io.github.warren1001.auriel.command

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import io.github.warren1001.auriel.*
import io.github.warren1001.auriel.user.Permission
import io.github.warren1001.auriel.util.Filter
import reactor.core.publisher.Mono
import java.time.Duration

class CommandManager(private val auriel: Auriel) {
	
	var prefix = "!"
	
	private val commands = mutableMapOf<String, Command>()
	
	init {
		registerCommand("ping", permission = Permission.EVERYONE) { it.event.message.reply("Pong!", true, Duration.ofSeconds(5L)) }
		registerCommand("setperm") { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ')
			ctx.event.guild.flatMap { it.getMemberById(Snowflake.of(args[0])) }.flatMap { user -> user.updateData(auriel) { it.permission = args[1].toInt() } }
				.then(ctx.event.message.reply("Set user '${args[0]}' permissions to '${args[1]}'"))
		}
		registerCommand("oom") { ctx ->
			val value = ctx.arguments.toBoolean()
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.setOnlyOneMessage(value) } }
		}
		registerCommand("ma") { ctx ->
			val arg = ctx.arguments
			val args = arg.split(' ')
			val duration = args[0].toLong()
			val interval = args[1].toLong()
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.setMaxMessageAge(duration, interval) } }
		}
		registerCommand("filter") { ctx ->
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.addFilter(Filter(Regex.fromLiteral(ctx.arguments))) } }
		}
		registerCommand("gfilter") { ctx ->
			val arg = ctx.arguments
			if (arg.contains(' ')) {
				val args = ctx.arguments.split(' ', limit = 2)
				ctx.event.guild.flatMap { it.updateData(auriel) { it.filters.add(Filter(Regex.fromLiteral(args[0]), args[1])) } }
			} else {
				ctx.event.guild.flatMap { it.updateData(auriel) { it.filters.add(Filter(Regex.fromLiteral(arg))) } }
			}
		}
		registerCommand("togglerepost") { ctx ->
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.toggleRepost() } }
		}
		registerCommand("linelimit") { ctx ->
			val arg = ctx.arguments
			val amount = arg.toInt()
			ctx.event.message.channel.ofType(GuildMessageChannel::class.java).flatMap { it.updateData(auriel) { it.setLineLimit(amount) } }
		}
		registerCommand("muteroleid") { ctx ->
			val id = Snowflake.of(ctx.arguments)
			ctx.event.guild.flatMap { it.updateData(auriel) { it.muteRoleId = id } }
		}
		registerCommand("logchannelid") { ctx ->
			val id = Snowflake.of(ctx.arguments)
			ctx.event.guild.flatMap { it.updateData(auriel) { it.logChannelId = id } }
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