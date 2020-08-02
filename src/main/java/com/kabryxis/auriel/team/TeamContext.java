package com.kabryxis.auriel.team;

import discord4j.common.util.Snowflake;
import org.apache.commons.lang3.Validate;

import javax.validation.constraints.NotNull;
import java.util.*;

public class TeamContext {
	
	@NotNull
	public static Builder builder(@NotNull Snowflake userId) {
		return new Builder(Objects.requireNonNull(userId, "userId cannot be null"));
	}
	
	private final Snowflake  userId;
	private final Realm      realm;
	private final Core       core;
	private final Type       type;
	private final List<Hero> preferredChars;
	private final Team[]     preferredTeams;
	private final List<Hero> optionalChars;
	private final Team[]     optionalTeams;
	
	public TeamContext(@NotNull Snowflake userId, @NotNull Realm realm, @NotNull Core core, @NotNull Type type, @NotNull List<Hero> preferredChars,
			@NotNull List<Hero> optionalChars) {
		this.userId = Objects.requireNonNull(userId, "userId cannot be null");
		this.realm = Objects.requireNonNull(realm, "realm cannot be null");
		this.core = Objects.requireNonNull(core, "core cannot be null");
		this.type = Objects.requireNonNull(type, "type cannot be null");
		Validate.notEmpty(preferredChars, "preferredChars cannot be empty");
		this.preferredChars = Objects.requireNonNull(preferredChars, "preferredChars cannot be null");
		this.preferredTeams = new Team[preferredChars.size()];
		this.optionalChars = Objects.requireNonNull(optionalChars, "optionalChars cannot be null");
		this.optionalTeams = new Team[optionalChars.size()];
	}
	
	@NotNull
	public Snowflake getUserId() {
		return userId;
	}
	
	@NotNull
	public Realm getRealm() {
		return realm;
	}
	
	@NotNull
	public Core getCore() {
		return core;
	}
	
	@NotNull
	public Type getType() {
		return type;
	}
	
	@NotNull
	public List<Hero> getPreferredChars() {
		return preferredChars;
	}
	
	@NotNull
	public List<Hero> getOptionalChars() {
		return optionalChars;
	}
	
	public void addPreferredTeam(@NotNull Hero hero, @NotNull Team team) {
		Validate.notNull(hero, "hero cannot be null");
		Validate.notNull(team, "team cannot be null");
		int index = 0;
		while (preferredChars.get(index) != hero) {
			index++;
		}
		if (preferredTeams[index] == null) preferredTeams[index] = team;
	}
	
	public void addOptionalTeam(@NotNull Hero hero, @NotNull Team team) {
		Validate.notNull(hero, "hero cannot be null");
		Validate.notNull(team, "team cannot be null");
		int index = 0;
		while (optionalChars.get(index) != hero) {
			index++;
		}
		if (optionalTeams[index] == null) optionalTeams[index] = team;
	}
	
	@NotNull
	public Optional<Team> getTeamToJoin() {
		for (Team team : preferredTeams) {
			if (team != null) return Optional.of(team);
		}
		for (Team team : optionalTeams) {
			if (team != null) return Optional.of(team);
		}
		return Optional.empty();
	}
	
	public static class Builder {
		
		private final Snowflake userId;
		
		private Builder(Snowflake userId) {
			this.userId = userId;
		}
		
		private Realm realm = null;
		
		@NotNull
		public Builder realm(@NotNull Realm realm) {
			this.realm = Objects.requireNonNull(realm, "realm cannot be null");
			return this;
		}
		
		private Core core = null;
		
		@NotNull
		public Builder core(@NotNull Core core) {
			this.core = Objects.requireNonNull(core, "core cannot be null");
			return this;
		}
		
		private Type type = Type.EXPANSION;
		
		@NotNull
		public Builder type(@NotNull Type type) {
			this.type = Objects.requireNonNull(type, "type cannot be null");
			return this;
		}
		
		private final List<Hero> preferredChars = new ArrayList<>();
		
		@NotNull
		public Builder preferredChar(@NotNull Hero hero) {
			preferredChars.add(Objects.requireNonNull(hero, "hero cannot be null"));
			return this;
		}
		
		@NotNull
		public Builder preferredChar(@NotNull Hero... heroes) {
			preferredChars.addAll(Arrays.asList(Objects.requireNonNull(heroes, "heroes cannot be null")));
			return this;
		}
		
		private final List<Hero> optionalChars = new ArrayList<>();
		
		@NotNull
		public Builder optionalChar(@NotNull Hero hero) {
			optionalChars.add(Objects.requireNonNull(hero, "hero cannot be null"));
			return this;
		}
		
		@NotNull
		public Builder optionalChar(@NotNull Hero... heroes) {
			optionalChars.addAll(Arrays.asList(Objects.requireNonNull(heroes, "heroes cannot be null")));
			return this;
		}
		
		@NotNull
		public TeamContext build() {
			Validate.notNull(realm, "Realm cannot be null");
			Validate.notNull(core, "Core cannot be null");
			Validate.isTrue(!preferredChars.isEmpty(), "There must be at least one preferred character.");
			return new TeamContext(userId, realm, core, type, preferredChars, optionalChars);
		}
		
	}
	
}
