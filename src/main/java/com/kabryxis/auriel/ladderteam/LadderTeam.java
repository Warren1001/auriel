package com.kabryxis.auriel.ladderteam;

import com.google.gson.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;

import java.lang.reflect.Type;
import java.util.Set;

public class LadderTeam { // TODO channels need to have their permissions properly set
	
	private final LadderTeamManager manager;
	private       String            name;
	private final Snowflake         roleId;
	private final Snowflake         voiceId;
	private final Snowflake         textId;
	
	private int count            = 0;
	private int countAmazon      = 0;
	private int countAssassin    = 0;
	private int countNecromancer = 0;
	private int countBarbarian   = 0;
	private int countPaladin     = 0;
	private int countSorceress   = 0;
	private int countDruid       = 0;
	
	public LadderTeam(LadderTeamManager manager, String name) {
		this.manager = manager;
		this.name = name;
		Guild guild = manager.getGuild().block();
		this.roleId = guild.createRole(
				spec -> spec.setName(name).setPermissions(guild.getEveryoneRole().block().getPermissions()).setHoist(false).setMentionable(false))
				.block()
				.getId();
		Set<PermissionOverwrite> perms = manager.getHiddenChannelPermissions(roleId);
		this.voiceId = guild.createVoiceChannel(spec -> spec.setPermissionOverwrites(perms).setParentId(manager.getCategoryId()).setName(name))
				.block()
				.getId();
		this.textId = guild.createTextChannel(spec -> spec.setPermissionOverwrites(perms).setParentId(manager.getCategoryId()).setName(name))
				.block()
				.getId();
	}
	
	public LadderTeam(LadderTeamManager manager, String name, Snowflake roleId, Snowflake voiceId, Snowflake textId) {
		this.manager = manager;
		this.name = name;
		this.roleId = roleId;
		this.voiceId = voiceId;
		this.textId = textId;
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
	
	public boolean isFull() {
		return count == 8;
	}
	
	public boolean isCharacterFull(Character character) {
		switch (character) {
			case AMAZON:
				return countAmazon == Character.AMAZON.getRecommendedMaxAmount();
			case ASSASSIN:
				return countAssassin == Character.ASSASSIN.getRecommendedMaxAmount();
			case NECROMANCER:
				return countNecromancer == Character.NECROMANCER.getRecommendedMaxAmount();
			case BARBARIAN:
				return countBarbarian == Character.BARBARIAN.getRecommendedMaxAmount();
			case PALADIN:
				return countPaladin == Character.PALADIN.getRecommendedMaxAmount();
			case SORCERESS:
				return countSorceress == Character.SORCERESS.getRecommendedMaxAmount();
			case DRUID:
				return countDruid == Character.DRUID.getRecommendedMaxAmount();
			default:
				return true;
		}
	}
	
	public void changeName(String name) {
		this.name = name;
		Guild guild = manager.getGuild().block();
		guild.getRoleById(roleId).block().edit(spec -> spec.setName(name)).block();
		((VoiceChannel)guild.getChannelById(voiceId).block()).edit(spec -> spec.setName(name)).block();
		((TextChannel)guild.getChannelById(textId).block()).edit(spec -> spec.setName(name.replace(' ', '-').toLowerCase())).block();
	}
	
	@Override
	public String toString() {
		return String.format("LadderTeam[name=%s]", name);
	}
	
	public static class Serializer implements JsonSerializer<LadderTeam>, JsonDeserializer<LadderTeam> {
		
		private final LadderTeamManager manager;
		
		public Serializer(LadderTeamManager manager) {
			this.manager = manager;
		}
		
		@Override
		public JsonElement serialize(LadderTeam team, Type typeOfSrc, JsonSerializationContext context) {
			
			JsonObject json = new JsonObject();
			
			json.addProperty("name", team.name);
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
		public LadderTeam deserialize(JsonElement src, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			
			JsonObject json = (JsonObject)src;
			
			String     name    = json.get("name").getAsString();
			Snowflake  roleId  = Snowflake.of(json.get("role").getAsLong());
			Snowflake  voiceId = Snowflake.of(json.get("voice").getAsLong());
			Snowflake  textId  = Snowflake.of(json.get("text").getAsLong());
			LadderTeam team    = new LadderTeam(manager, name, roleId, voiceId, textId);
			
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
