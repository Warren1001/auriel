package com.kabryxis.auriel;

import com.kabryxis.auriel.page.PagedEmbedManager;
import com.kabryxis.auriel.team.TeamManager;
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
		
		new Auriel(token);
		
	}
	
	private GatewayDiscordClient gateway;
	private TeamManager          teamManager;
	private PagedEmbedManager    embedManager;
	
	public Auriel(String token) {
		
		gateway = DiscordClient.create(token).login().doOnError(Throwable::printStackTrace).block();
		gateway.on(MessageCreateEvent.class).doOnError(Throwable::printStackTrace).subscribe(new MessageListener(this));
		
		teamManager = new TeamManager(this);
		embedManager = new PagedEmbedManager(this);
		
		gateway.onDisconnect().block(); // todo handle any cleanup needed
	}
	
	public GatewayDiscordClient getGateway() {
		return gateway;
	}
	
	public TeamManager getLadderTeamManager() {
		return teamManager;
	}
	
	public PagedEmbedManager getEmbedManager() {
		return embedManager;
	}
	
}
