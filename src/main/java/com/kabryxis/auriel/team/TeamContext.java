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
	
	private final Snowflake    userId;
	private final Ladder.Realm realm;
	private final Ladder.Core  core;
	private final Ladder.Type  type;
	private final List<Hero>   characters;
	private final boolean      prefersPrimary;
	
	public TeamContext(@NotNull Snowflake userId, @NotNull Ladder.Realm realm, @NotNull Ladder.Core core, @NotNull Ladder.Type type, @NotNull List<Hero> characters, boolean prefersPrimary) {
		this.userId = Objects.requireNonNull(userId, "userId cannot be null");
		this.realm = Objects.requireNonNull(realm, "realm cannot be null");
		this.core = Objects.requireNonNull(core, "core cannot be null");
		this.type = Objects.requireNonNull(type, "type cannot be null");
		Validate.notEmpty(characters, "characters cannot be empty");
		this.characters = Objects.requireNonNull(characters, "preferredChars cannot be null");
		this.prefersPrimary = prefersPrimary;
	}
	
	@NotNull
	public Snowflake getUserId() {
		return userId;
	}
	
	@NotNull
	public Ladder.Realm getRealm() {
		return realm;
	}
	
	@NotNull
	public Ladder.Core getCore() {
		return core;
	}
	
	@NotNull
	public Ladder.Type getType() {
		return type;
	}
	
	@NotNull
	public List<Hero> getCharacters() {
		return characters;
	}
	
	public boolean prefersPrimary() {
		return prefersPrimary;
	}
	
	public static class Builder {
		
		private final Snowflake userId;
		
		private Builder(Snowflake userId) {
			this.userId = userId;
		}
		
		private Ladder.Realm realm = null;
		
		@NotNull
		public Builder realm(@NotNull Ladder.Realm realm) {
			this.realm = Objects.requireNonNull(realm, "realm cannot be null");
			return this;
		}
		
		private Ladder.Core core = null;
		
		@NotNull
		public Builder core(@NotNull Ladder.Core core) {
			this.core = Objects.requireNonNull(core, "core cannot be null");
			return this;
		}
		
		private Ladder.Type type = Ladder.Type.EXPANSION;
		
		@NotNull
		public Builder type(@NotNull Ladder.Type type) {
			this.type = Objects.requireNonNull(type, "type cannot be null");
			return this;
		}
		
		private final List<Hero> heroes = new ArrayList<>();
		
		@NotNull
		public Builder hero(@NotNull Hero hero) {
			heroes.add(Objects.requireNonNull(hero, "hero cannot be null"));
			return this;
		}
		
		@NotNull
		public Builder heroes(@NotNull Hero... heroes) {
			this.heroes.addAll(Arrays.asList(Objects.requireNonNull(heroes, "heroes cannot be null")));
			return this;
		}
		
		public void clearHeroes() {
			heroes.clear();
		}
		
		private boolean prefersPrimary = false;
		
		public Builder prefersPrimary() {
			prefersPrimary = true;
			return this;
		}
		
		@NotNull
		public TeamContext build() {
			Validate.notNull(realm, "Realm cannot be null");
			Validate.notNull(core, "Core cannot be null");
			Validate.isTrue(!heroes.isEmpty(), "There must be at least one character selected.");
			return new TeamContext(userId, realm, core, type, heroes, prefersPrimary);
		}
		
	}
	
}
