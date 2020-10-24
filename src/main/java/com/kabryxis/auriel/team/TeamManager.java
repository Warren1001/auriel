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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TeamManager {
	
	private static final Path DATA_PATH = Paths.get("data.json");
	
	private final Auriel               bot;
	private final Snowflake            guildId;
	private final Snowflake            categoryId;
	private final Gson                 gson;
	private final List<Team>           teams;
	private final Map<Snowflake, Team> userTeams;
	
	private PermissionSet       defaultPermissions;
	private PermissionOverwrite everyoneOverwrite;
	
	public TeamManager(Auriel bot) {
		this.bot = bot;
		guildId = Snowflake.of(735575872785874950L); // TODO make in json file
		categoryId = Snowflake.of(735656522142580789L); // TODO make in json file
		gson = new GsonBuilder().registerTypeAdapter(Team.class, new Team.Serializer(this)).setPrettyPrinting().create();
		teams = Collections.synchronizedList(loadData());
		userTeams = Collections.synchronizedMap(new HashMap<>());
		getGuild().flatMap(Guild::getEveryoneRole).subscribe(role -> {
			defaultPermissions = role.getPermissions();
			everyoneOverwrite = PermissionOverwrite.forRole(role.getId(), PermissionSet.none(), PermissionSet.of(Permission.VIEW_CHANNEL));
		});
	}
	
	public Mono<Guild> getGuild() {
		return bot.getGateway().getGuildById(guildId);
	}
	
	public Snowflake getCategoryId() {
		return categoryId;
	}
	
	public Mono<Team> createTeam(TeamContext context) {
		
		Set<PermissionOverwrite> permissionOverwrites = new HashSet<>(2);
		permissionOverwrites.add(everyoneOverwrite);
		
		Team team = new Team(this, "Team " + (teams.size() + 1), context);
		teams.add(team);
		
		return getGuild().flatMap(guild -> guild
				.createRole(spec -> spec.setName(team.getName()).setPermissions(defaultPermissions).setHoist(false).setMentionable(false))
				.doOnError(Throwable::printStackTrace).doOnSuccess(role -> {
					System.out.println("0.0");
					team.setRoleId(role);
					permissionOverwrites
							.add(PermissionOverwrite.forRole(role.getId(), PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()));
				}).then(Mono.when(guild.createVoiceChannel(spec -> {
					spec.setParentId(categoryId).setName(team.getName());
					spec.setPermissionOverwrites(permissionOverwrites);
				}).doOnError(Throwable::printStackTrace).doOnSuccess(team::setVoiceId), guild.createTextChannel(spec -> {
					spec.setParentId(categoryId).setName(team.getName());
					spec.setPermissionOverwrites(permissionOverwrites);
				}).doOnError(Throwable::printStackTrace).doOnSuccess(team::setTextId))).map(ignore -> team));
	}
	
	public void joinTeam(TeamContext context) {
		for (Hero character : context.getCharacters()) {
			for (Team team : teams) {
				if (team.hasCharacterSpace(character)) {
					team.join(context.getUserId());
					userTeams.put(context.getUserId(), team);
					return;
				}
			}
			if (context.prefersPrimary()) {
				break;
			}
		}
		createTeam(context).subscribe(team -> {
			System.out.println("1.1");
			team.join(context.getUserId());
			userTeams.put(context.getUserId(), team);
		});
	}
	
	public void leaveTeam(Snowflake userId) {
		Team team = userTeams.remove(userId);
		if (team != null) {
			System.out.println("made it");
			team.leave(userId);
		}
	}
	
	/*public Mono<Set<PermissionOverwrite>> getHiddenChannelPermissions(Snowflake ignoreRoleId) {
		return getGuild().flux()
				.flatMap(Guild::getRoles)
				.filter(role -> !role.getPermissions().contains(Permission.MANAGE_CHANNELS))
				.map(role -> role.getId().equals(ignoreRoleId) ?
						PermissionOverwrite.forRole(ignoreRoleId, PermissionSet.of(Permission.VIEW_CHANNEL), PermissionSet.none()) :
						PermissionOverwrite.forRole(role.getId(), PermissionSet.none(), PermissionSet.of(Permission.VIEW_CHANNEL)))
				.collect(Collectors.toSet());
	}*/
	
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
	
	private List<Team> loadData() {
		try {
			return gson.fromJson(new String(Files.readAllBytes(DATA_PATH)), new TypeToken<List<Team>>() {
			
			}.getType());
		} catch (IOException e) {
			if (e instanceof NoSuchFileException) {
				return new ArrayList<>();
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
		}).subscribe(guild -> Flux.fromIterable(teams).flatMap(team -> {
			Publisher<?> deleteRole  = guild.getRoleById(team.getRoleId()).flatMap(Role::delete);
			Publisher<?> deleteVoice = guild.getChannelById(team.getVoiceId()).flatMap(Channel::delete);
			Publisher<?> deleteText  = guild.getChannelById(team.getTextId()).flatMap(Channel::delete);
			
			return Mono.when(deleteRole, deleteVoice, deleteText);
		}).subscribe());
	}
	
}
