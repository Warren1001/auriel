package com.kabryxis.auriel;

public enum Character {
	
	AMAZON(2), ASSASSIN(2), NECROMANCER(2), BARBARIAN(1), PALADIN(2), SORCERESS(3), DRUID(2);
	
	private final int recomMaxAmount;
	
	Character(int recomMaxAmount) {
		this.recomMaxAmount = recomMaxAmount;
	}
	
	public int getRecommendedMaxAmount() {
		return recomMaxAmount;
	}
	
}
