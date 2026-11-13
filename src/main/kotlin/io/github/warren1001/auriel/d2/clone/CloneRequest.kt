package io.github.warren1001.auriel.d2.clone

class CloneRequest(
	val helpee: HelpeeCloneValues,
	val helper: HelperCloneValues,
	var hasBeenVouched: Boolean = false,
)