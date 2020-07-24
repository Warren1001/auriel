package com.kabryxis.auriel;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.function.Consumer;

public class MessageListener implements Consumer<MessageCreateEvent> {
	
	private final Auriel bot;
	
	public MessageListener(Auriel bot) {
		this.bot = bot;
	}
	
	@Override
	public void accept(MessageCreateEvent event) { // TODO eventually overhaul
		Message  message  = event.getMessage();
		String[] args     = message.getContent().split(" ", 2);
		String   command  = args[0].toLowerCase();
		String   argument = args.length == 2 ? args[1] : null;
		switch (command) {
			case "!ping":
				MessageChannel channel = message.getChannel().block();
				channel.createMessage("Pong!").block();
				break;
			case "!stop":
				bot.getLadderTeamManager().save();
				bot.getGateway().logout().block();
				break;
			case "!createteam":
				bot.getLadderTeamManager().createTeam(argument);
				break;
			case "!purge":
				bot.getLadderTeamManager().purge();
				break;
			default:
				break;
		}
	}
	
}
