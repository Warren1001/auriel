package io.github.warren1001.auriel.d2.clone

data class CloneDataHelpeeInput(
	var gameName: String? = null,
	var password: String? = null,
	var location: String? = null,
	var furthestAct: String? = null,
) {
	fun isAllInput() = gameName != null && password != null && location != null && furthestAct != null
}
