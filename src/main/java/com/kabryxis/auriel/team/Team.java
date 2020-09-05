package com.kabryxis.auriel.team;

import com.google.gson.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;

import java.util.Set;

public class Team { // TODO channels need to have their permissions properly set
	
	private final TeamManager  manager;
	private final Ladder.Realm realm;
	private final Ladder.Core  core;
	private final Ladder.Type  type;
	private       String       name;
	private final Snowflake    roleId;
	private final Snowflake    voiceId;
	private final Snowflake    textId;
	
	private int count            = 0;
	private int countAmazon      = 0;
	private int countAssassin    = 0;
	private int countNecromancer = 0;
	private int countBarbarian   = 0;
	private int countPaladin     = 0;
	private int countSorceress   = 0;
	private int countDruid       = 0;
	
	public Team(TeamManager manager, String name, Ladder.Realm realm, Ladder.Core core, Ladder.Type type) {
		this.manager = manager;
		this.realm = realm;
		this.core = core;
		this.type = type;
		this.name = name;
		Guild guild = manager.getGuild().block();
		this.roleId = guild.createRole(spec -> spec.setName(name)
				.setPermissions(guild.getEveryoneRole().block().getPermissions())
				.setHoist(false)
				.setMentionable(false)).block().getId();
		Set<PermissionOverwrite> perms = manager.getHiddenChannelPermissions(roleId);
		this.voiceId = guild.createVoiceChannel(spec -> spec.setPermissionOverwrites(perms).setParentId(manager.getCategoryId()).setName(name))
				.block()
				.getId();
		this.textId = guild.createTextChannel(spec -> spec.setPermissionOverwrites(perms).setParentId(manager.getCategoryId()).setName(name))
				.block()
				.getId();
	}
	
	public Team(TeamManager manager, String name, Ladder.Realm realm, Ladder.Core core, Ladder.Type type, Snowflake roleId, Snowflake voiceId,
			Snowflake textId) {
		this.manager = manager;
		this.realm = realm;
		this.core = core;
		this.type = type;
		this.name = name;
		this.roleId = roleId;
		this.voiceId = voiceId;
		this.textId = textId;
	}
	
	public Team(TeamManager manager, String name, TeamContext context) {
		this(manager, name, context.getRealm(), context.getCore(), context.getType());
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
	
	public void addJoinContext(TeamContext context) {
		if (hasSpace() && context.getRealm() == realm && context.getCore() == core && context.getType() == type) {
			context.getPreferredChars().stream().filter(this::hasCharacterSpace).forEachOrdered(c -> context.addPreferredTeam(c, this));
		}
	}
	
	public void changeName(String name) {
		this.name = name;
		Guild guild = manager.getGuild().block();
		guild.getRoleById(roleId).block().edit(spec -> spec.setName(name)).block();
		((VoiceChannel)guild.getChannelById(voiceId).block()).edit(spec -> spec.setName(name)).block();
		((TextChannel)guild.getChannelById(textId).block()).edit(spec -> spec.setName(name.replace(' ', '-').toLowerCase())).block();
	}
	
	public void join(Snowflake userId) {
		manager.getGuild().block().getMemberById(userId).block().addRole(roleId).block();
	}
	
	public void leave(Snowflake userId) {
		manager.getGuild().block().getMemberById(userId).block().removeRole(roleId).block();
	}
	
	@Override
	public String toString() {
		return String.format("Team[name=%s]", name);
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
			
			if (team.count != 0) json.addProperty("count", team.count);
			if (team.countAmazon != 0) json.addProperty("countz", team.countAmazon);
			if (team.countAssassin != 0) json.addProperty("counta", team.countAssassin);
			if (team.countNecromancer != 0) json.addProperty("countn", team.countNecromancer);
			if (team.countBarbarian != 0) json.addProperty("countb", team.countBarbarian);
			if (team.countPaladin != 0) json.addProperty("countp", team.countPaladin);
			if (team.countSorceress != 0) json.addProperty("counts", team.countSorceress);
			if (team.countDruid != 0) json.addProperty("countd", team.countDruid);
			
			return json;
		}
		
		@Override
		public Team deserialize(JsonElement src, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			
			JsonObject json = (JsonObject)src;
			
			String       name    = json.get("name").getAsString();
			Ladder.Realm realm   = Ladder.Realm.valueOf(json.get("realm").getAsString());
			Ladder.Core  core    = Ladder.Core.valueOf(json.get("core").getAsString());
			Ladder.Type  type    = Ladder.Type.valueOf(json.get("type").getAsString());
			Snowflake    roleId  = Snowflake.of(json.get("role").getAsLong());
			Snowflake    voiceId = Snowflake.of(json.get("voice").getAsLong());
			Snowflake    textId  = Snowflake.of(json.get("text").getAsLong());
			Team         team    = new Team(manager, name, realm, core, type, roleId, voiceId, textId);
			
			if (json.has("count")) team.count = json.get("count").getAsInt();
			if (json.has("countz")) team.countAmazon = json.get("countz").getAsInt();
			if (json.has("counta")) team.countAssassin = json.get("counta").getAsInt();
			if (json.has("countn")) team.countNecromancer = json.get("countn").getAsInt();
			if (json.has("countb")) team.countBarbarian = json.get("countb").getAsInt();
			if (json.has("countp")) team.countPaladin = json.get("countp").getAsInt();
			if (json.has("counts")) team.countSorceress = json.get("counts").getAsInt();
			if (json.has("countd")) team.countDruid = json.get("countd").getAsInt();
			
			return team;
		}
		
	}
	
}
