package com.kabryxis.auriel;

import com.kabryxis.auriel.ladderteam.LadderTeamManager;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Auriel {
	
	public static void main(String[] args) {
		
		String token;
		try {
			token = new String(Files.readAllBytes(Paths.get("token.txt")));
		} catch (IOException e) {
			e.printStackTrace();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return;
		}
		
		Auriel manager = new Auriel(token);
		
		manager.getGateway().onDisconnect().block();
		
	}
	
	private final GatewayDiscordClient gateway;
	private       LadderTeamManager    teamManager;
	
	public Auriel(String token) {
		
		gateway = DiscordClient.create(token).login().block();
		
		gateway.on(MessageCreateEvent.class).subscribe(new MessageListener(this));
		
		teamManager = new LadderTeamManager(this);
		
	}
	
	public GatewayDiscordClient getGateway() {
		return gateway;
	}
	
	public LadderTeamManager getLadderTeamManager() {
		return teamManager;
	}
	
}
