package com.kabryxis.auriel;

import com.kabryxis.auriel.team.*;
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
		Message        message  = event.getMessage();
		String[]       args     = message.getContent().split(" ", 2);
		String         command  = args[0].toLowerCase();
		String         argument = args.length == 2 ? args[1] : null;
		MessageChannel channel  = message.getChannel().block();
		switch (command) {
			case "!ping":
				channel.createMessage("Pong!").block();
				break;
			case "!stop":
				bot.getLadderTeamManager().save();
				bot.getGateway().logout().block();
				break;
			case "!createteam":
				bot.getLadderTeamManager().createTeam(TeamContext.builder(null) // TODO fix null
						.realm(Realm.EAST).core(Core.SOFTCORE).type(Type.EXPANSION).preferredChar(Hero.SORCERESS).build());
				break;
			case "!purge":
				bot.getLadderTeamManager().purge();
				break;
			default:
				break;
		}
	}
	
}
