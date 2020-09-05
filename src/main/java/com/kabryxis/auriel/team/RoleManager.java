package com.kabryxis.auriel.team;

import com.kabryxis.auriel.Auriel;
import discord4j.common.util.Snowflake;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class RoleManager {
	
	private static final Path DATA = Paths.get("roles.json");
	
	private final Map<Hero, Snowflake> characterRoles = Collections.synchronizedMap(new EnumMap<>(Hero.class));
	
	private final Auriel bot;
	
	public RoleManager(Auriel bot) {
		this.bot = bot;
	}
	
}
