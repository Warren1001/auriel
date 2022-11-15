package io.github.warren1001.auriel.d2.item

abstract class EquippableItem(names: Map<String, String>, qlvl: Int, rlvl: Int, rstr: Int, rdex: Int,
	val durability: Int, val hasDurability: Boolean):
		Item(names, qlvl, rlvl, rstr, rdex)