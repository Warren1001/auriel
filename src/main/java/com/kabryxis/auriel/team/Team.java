package com.kabryxis.auriel.team;

import com.google.gson.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Team { // TODO channels need to have their permissions properly set
	
	private final TeamManager    manager;
	private final Ladder.Realm   realm;
	private final Ladder.Core    core;
	private final Ladder.Type    type;
	private final Set<Snowflake> users;
	
	private Snowflake roleId;
	private Snowflake voiceId;
	private Snowflake textId;
	
	private String name;
	private int    count            = 0;
	private int    countAmazon      = 0;
	private int    countAssassin    = 0;
	private int    countNecromancer = 0;
	private int    countBarbarian   = 0;
	private int    countPaladin     = 0;
	private int    countSorceress   = 0;
	private int    countDruid       = 0;
	
	public Team(TeamManager manager, String name, Ladder.Realm realm, Ladder.Core core, Ladder.Type type) {
		this.manager = manager;
		this.realm = realm;
		this.core = core;
		this.type = type;
		this.name = name;
		this.users = Collections.synchronizedSet(new HashSet<>());
	}
	
	public Team(TeamManager manager, String name, Ladder.Realm realm, Ladder.Core core, Ladder.Type type, Snowflake roleId, Snowflake voiceId, Snowflake textId, Set<Snowflake> users) {
		this.manager = manager;
		this.realm = realm;
		this.core = core;
		this.type = type;
		this.name = name;
		this.roleId = roleId;
		this.voiceId = voiceId;
		this.textId = textId;
		this.users = Collections.synchronizedSet(users);
	}
	
	public Team(TeamManager manager, String name, TeamContext context) {
		this(manager, name, context.getRealm(), context.getCore(), context.getType());
	}
	
	public String getName() {
		return name;
	}
	
	public Snowflake getRoleId() {
		return roleId;
	}
	
	public Snowflake getVoiceId() {
		return voiceId;
	}
	
	public Snowflake getTextId() {
		return textId;
	}
	
	public boolean hasSpace() {
		return count < 8;
	}
	
	public boolean hasCharacterSpace(Hero character) {
		if (!hasSpace()) {
			return false;
		}
		switch (character) {
			case AMAZON:
				return countAmazon < Hero.AMAZON.getRecommendedMaxAmount();
			case ASSASSIN:
				return countAssassin < Hero.ASSASSIN.getRecommendedMaxAmount();
			case NECROMANCER:
				return countNecromancer < Hero.NECROMANCER.getRecommendedMaxAmount();
			case BARBARIAN:
				return countBarbarian < Hero.BARBARIAN.getRecommendedMaxAmount();
			case PALADIN:
				return countPaladin < Hero.PALADIN.getRecommendedMaxAmount();
			case SORCERESS:
				return countSorceress < Hero.SORCERESS.getRecommendedMaxAmount();
			case DRUID:
				return countDruid < Hero.DRUID.getRecommendedMaxAmount();
			default:
				return false;
		}
	}
	
	/*public void addJoinContext(TeamContext context) {
		if (hasSpace() && context.getRealm() == realm && context.getCore() == core && context.getType() == type) {
			context.getPreferredChars().stream().filter(this::hasCharacterSpace).forEachOrdered(c -> context.addPreferredTeam(c, this));
		}
	}*/
	
	public void changeName(String name) {
		this.name = name;
		manager.getGuild().subscribe(guild -> Flux.concat(guild.getRoleById(roleId).map(role -> role.edit(spec -> spec.setName(name))),
				guild.getChannelById(voiceId).map(channel -> ((VoiceChannel)channel).edit(spec -> spec.setName(name))),
				guild.getChannelById(textId)
						.map(channel -> ((TextChannel)channel).edit(spec -> spec.setName(name.replace(' ', '-').toLowerCase())))));
	}
	
	public void join(Snowflake userId) {
		System.out.println("0");
		manager.getGuild().flatMap(guild -> {
			System.out.println(userId.asLong());
			return guild.getMemberById(userId);
		}).flatMap(member -> {
			System.out.println(roleId.asLong());
			return member.addRole(roleId);
		}).subscribe(ignore -> users.add(userId));
	}
	
	public void leave(Snowflake userId) {
		manager.getGuild().flatMap(guild -> guild.getMemberById(userId)).subscribe(member -> {
			member.removeRole(roleId);
			users.remove(userId);
		});
	}
	
	@Override
	public String toString() {
		return String.format("Team[name=%s]", name);
	}
	
	void setRoleId(Role role) {
		roleId = role.getId();
	}
	
	void setVoiceId(VoiceChannel channel) {
		voiceId = channel.getId();
	}
	
	void setTextId(TextChannel channel) {
		textId = channel.getId();
	}
	
	public static class Serializer implements JsonSerializer<Team>, JsonDeserializer<Team> {
		
		private final TeamManager manager;
		
		public Serializer(TeamManager manager) {
			this.manager = manager;
		}
		
		@Override
		public JsonElement serialize(Team team, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
			
			JsonObject json = new JsonObject();
			
			json.addProperty("name", team.name);
			json.addProperty("realm", team.realm.name());
			json.addProperty("core", team.core.name());
			json.addProperty("type", team.type.name());
			json.addProperty("role", team.roleId.asLong());
			json.addProperty("voice", team.voiceId.asLong());
			json.addProperty("text", team.textId.asLong());
			
			if (team.count != 0) {
				json.addProperty("count", team.count);
			}
			if (team.countAmazon != 0) {
				json.addProperty("countz", team.countAmazon);
			}
			if (team.countAssassin != 0) {
				json.addProperty("counta", team.countAssassin);
			}
			if (team.countNecromancer != 0) {
				json.addProperty("countn", team.countNecromancer);
			}
			if (team.countBarbarian != 0) {
				json.addProperty("countb", team.countBarbarian);
			}
			if (team.countPaladin != 0) {
				json.addProperty("countp", team.countPaladin);
			}
			if (team.countSorceress != 0) {
				json.addProperty("counts", team.countSorceress);
			}
			if (team.countDruid != 0) {
				json.addProperty("countd", team.countDruid);
			}
			if (!team.users.isEmpty()) {
				json.addProperty("users", serializeUsers(team.users));
			}
			
			return json;
		}
		
		@Override
		public Team deserialize(JsonElement src, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			
			JsonObject json = (JsonObject)src;
			
			String         name    = json.get("name").getAsString();
			Ladder.Realm   realm   = Ladder.Realm.valueOf(json.get("realm").getAsString());
			Ladder.Core    core    = Ladder.Core.valueOf(json.get("core").getAsString());
			Ladder.Type    type    = Ladder.Type.valueOf(json.get("type").getAsString());
			Snowflake      roleId  = Snowflake.of(json.get("role").getAsLong());
			Snowflake      voiceId = Snowflake.of(json.get("voice").getAsLong());
			Snowflake      textId  = Snowflake.of(json.get("text").getAsLong());
			Set<Snowflake> users   = json.has("users") ? deserializeUsers(json.get("users").getAsString()) : new HashSet<>();
			Team           team    = new Team(manager, name, realm, core, type, roleId, voiceId, textId, users);
			
			if (json.has("count")) {
				team.count = json.get("count").getAsInt();
			}
			if (json.has("countz")) {
				team.countAmazon = json.get("countz").getAsInt();
			}
			if (json.has("counta")) {
				team.countAssassin = json.get("counta").getAsInt();
			}
			if (json.has("countn")) {
				team.countNecromancer = json.get("countn").getAsInt();
			}
			if (json.has("countb")) {
				team.countBarbarian = json.get("countb").getAsInt();
			}
			if (json.has("countp")) {
				team.countPaladin = json.get("countp").getAsInt();
			}
			if (json.has("counts")) {
				team.countSorceress = json.get("counts").getAsInt();
			}
			if (json.has("countd")) {
				team.countDruid = json.get("countd").getAsInt();
			}
			
			return team;
		}
		
		private String serializeUsers(Set<Snowflake> users) {
			return String.join(",", users.stream().map(Snowflake::asString).collect(Collectors.toSet()));
		}
		
		private Set<Snowflake> deserializeUsers(String serialized) {
			return Stream.of(serialized.split(",")).map(Snowflake::of).collect(Collectors.toSet());
		}
		
	}
	
	public static class Builder {
		
		private final TeamManager manager;
		
		public Builder(TeamManager manager) {
			this.manager = manager;
		}
		
		private String         name;
		private Ladder.Realm   realm;
		private Ladder.Core    core;
		private Ladder.Type    type;
		private Set<Snowflake> users;
		private Snowflake      roleId;
		private Snowflake      voiceId;
		private Snowflake      textId;
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setContext(TeamContext context) {
			setRealm(context.getRealm());
			setCore(context.getCore());
			setType(context.getType());
		}
		
		public Ladder.Realm getRealm() {
			return realm;
		}
		
		public void setRealm(Ladder.Realm realm) {
			this.realm = realm;
		}
		
		public Ladder.Core getCore() {
			return core;
		}
		
		public void setCore(Ladder.Core core) {
			this.core = core;
		}
		
		public Ladder.Type getType() {
			return type;
		}
		
		public void setType(Ladder.Type type) {
			this.type = type;
		}
		
		public Set<Snowflake> getUsers() {
			return users;
		}
		
		public void setUsers(Set<Snowflake> users) {
			this.users = users;
		}
		
		public Snowflake getRoleId() {
			return roleId;
		}
		
		public void setRoleId(Snowflake roleId) {
			this.roleId = roleId;
		}
		
		public Snowflake getVoiceId() {
			return voiceId;
		}
		
		public void setVoiceId(Snowflake voiceId) {
			this.voiceId = voiceId;
		}
		
		public Snowflake getTextId() {
			return textId;
		}
		
		public void setTextId(Snowflake textId) {
			this.textId = textId;
		}
		
		public Team build() {
			return new Team(manager, name, realm, core, type, roleId, voiceId, textId, users);
		}
		
	}
	
}
