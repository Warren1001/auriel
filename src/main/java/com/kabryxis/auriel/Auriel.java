package com.kabryxis.auriel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Auriel {
	
	private static final Path DATA_PATH = Paths.get("data.json");
	
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
	
	private final GatewayDiscordClient    gateway;
	private final Snowflake               guildId;
	private final Snowflake               categoryId;
	private final Gson                    gson;
	private final Map<String, LadderTeam> ladderTeams;
	
	public Auriel(String token) {
		
		gateway = DiscordClient.create(token).login().block();
		guildId = Snowflake.of(735575872785874950L); // TODO make in json file
		categoryId = Snowflake.of(735656522142580789L); // TODO ^
		
		gateway.on(MessageCreateEvent.class).subscribe(new MessageListener(this));
		
		gson = new GsonBuilder().registerTypeAdapter(LadderTeam.class, new LadderTeam.Serializer(this)).setPrettyPrinting().create();
		ladderTeams = Collections.synchronizedMap(loadData());
		
	}
	
	public GatewayDiscordClient getGateway() {
		return gateway;
	}
	
	public Mono<Guild> getGuild() {
		return gateway.getGuildById(guildId);
	}
	
	public Snowflake getCategoryId() {
		return categoryId;
	}
	
	public LadderTeam createTeam(String name) {
		return ladderTeams.computeIfAbsent(name, n -> new LadderTeam(this, n));
	}
	
	public Set<PermissionOverwrite> getHiddenChannelPermissions(Snowflake ignoreRoleId) {
		return getGuild().block().getRoles().filter(role -> !role.getPermissions().contains(Permission.MANAGE_CHANNELS)).map(role -> {
			if (role.getId().equals(ignoreRoleId)) {
				return PermissionOverwrite.forRole(ignoreRoleId, PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none());
			}
			return PermissionOverwrite.forRole(role.getId(), PermissionSet.none(), PermissionSet.of(Permission.VIEW_CHANNEL));
		}).collect(Collectors.toSet()).block();
	}
	
	public void save() {
		if (ladderTeams.isEmpty()) return;
		try {
			Files.write(DATA_PATH, gson.toJson(ladderTeams).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Map<String, LadderTeam> loadData() {
		try {
			return gson.fromJson(new String(Files.readAllBytes(DATA_PATH)), new TypeToken<Map<String, LadderTeam>>() {}.getType());
		} catch (IOException e) {
			if (e instanceof NoSuchFileException) return new HashMap<>();
			throw new RuntimeException(e);
		}
	}
	
	public void purge() {
		Guild guild = getGuild().block();
		ladderTeams.values().forEach(team -> {
			guild.getRoleById(team.getRoleId()).block().delete("Purged").block();
			guild.getChannelById(team.getVoiceId()).block().delete("Purged").block();
			guild.getChannelById(team.getTextId()).block().delete("Purged").block();
		});
		ladderTeams.clear();
		if (DATA_PATH.toFile().exists()) {
			try {
				Files.delete(DATA_PATH);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
