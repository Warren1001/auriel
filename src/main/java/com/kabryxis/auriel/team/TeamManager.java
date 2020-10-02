package com.kabryxis.auriel.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kabryxis.auriel.Auriel;
import discord4j.common.util.Snowflake;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Channel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TeamManager {
	
	private static final Path DATA_PATH = Paths.get("data.json");
	
	private final Auriel               bot;
	private final Snowflake            guildId;
	private final Snowflake            categoryId;
	private final Gson                 gson;
	private final Set<Team>            teams;
	private final Map<Snowflake, Team> userTeams;
	
	public TeamManager(Auriel bot) {
		this.bot = bot;
		guildId = Snowflake.of(735575872785874950L); // TODO make in json file
		categoryId = Snowflake.of(735656522142580789L); // TODO make in json file
		gson = new GsonBuilder().registerTypeAdapter(Team.class, new Team.Serializer(this)).setPrettyPrinting().create();
		teams = Collections.synchronizedSet(loadData());
		userTeams = Collections.synchronizedMap(new HashMap<>());
	}
	
	public Mono<Guild> getGuild() {
		return bot.getGateway().getGuildById(guildId);
	}
	
	public Snowflake getCategoryId() {
		return categoryId;
	}
	
	public Team createTeam(TeamContext context) {
		Team team = new Team(this, "Team " + (teams.size() + 1), context);
		teams.add(team);
		return team;
	}
	
	public void joinTeam(TeamContext context) {
		teams.forEach(team -> team.addJoinContext(context));
		Optional<Team> optionalTeam = context.getTeamToJoin();
		if(optionalTeam.isPresent()) {
		
		} else {
			getGuild().subscribe(guild -> guild.createRole(spec -> spec.setName(name).setPermissions(
					guild.getEveryoneRole().block().getPermissions()).setHoist(false).setMentionable(false))
					.doOnSuccess(this::setRoleId)
					.then(guild.createVoiceChannel(spec -> {
						spec.setParentId(manager.getCategoryId()).setName(name);
						manager.getHiddenChannelPermissions(getRoleId()).subscribe(spec::setPermissionOverwrites);
					}).doOnSuccess(this::setVoiceId).and(guild.createTextChannel(spec -> {
						spec.setParentId(manager.getCategoryId()).setName(name);
						manager.getHiddenChannelPermissions(getRoleId()).subscribe(spec::setPermissionOverwrites);
					}).doOnSuccess(this::setTextId)))
					.subscribe());
		}
		userTeams.put(context.getUserId(), team);
		team.join(context.getUserId());
	}
	
	public void leaveTeam(Snowflake userId) {
		Team team = userTeams.remove(userId);
		if (team != null) {
			System.out.println("made it");
			team.leave(userId);
		}
	}
	
	public Mono<Set<PermissionOverwrite>> getHiddenChannelPermissions(Snowflake ignoreRoleId) {
		return getGuild().flux().flatMap(Guild::getRoles).filter(role -> !role.getPermissions().contains(Permission.MANAGE_CHANNELS)).map(
				role -> role.getId().equals(ignoreRoleId) ? PermissionOverwrite.forRole(ignoreRoleId, PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none()) : PermissionOverwrite.forRole(role.getId(), PermissionSet.none(),
						PermissionSet.of(Permission.VIEW_CHANNEL))).collect(Collectors.toSet());
	}
	
	public void save() {
		if (teams.isEmpty()) {
			return;
		}
		try {
			Files.write(DATA_PATH, gson.toJson(teams).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Set<Team> loadData() {
		try {
			return gson.fromJson(new String(Files.readAllBytes(DATA_PATH)), new TypeToken<Set<Team>>() {
			
			}.getType());
		} catch (IOException e) {
			if (e instanceof NoSuchFileException) {
				return new HashSet<>();
			}
			throw new RuntimeException(e);
		}
	}
	
	public void purge() {
		getGuild().doAfterTerminate(() -> {
			teams.clear();
			if (DATA_PATH.toFile().exists()) {
				try {
					Files.delete(DATA_PATH);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).subscribe(guild -> Flux.fromIterable(teams).subscribe(team -> guild.getRoleById(team.getRoleId())
				.doOnSuccess(Role::delete)
				.and(guild.getChannelById(team.getVoiceId()).doOnSuccess(Channel::delete))
				.and(guild.getChannelById(team.getTextId()).doOnSuccess(Channel::delete))
				.subscribe()));
		
	}
	
}
