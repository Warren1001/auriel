package io.github.warren1001.auriel.d2.clone

import com.fasterxml.jackson.annotation.JsonIgnore

data class CloneDataHelpee(
	val _id: String,
	val idPersonalThread: String,
	var selections: CloneDataHelpeeSelection? = null,
	var inputs: CloneDataHelpeeInput? = null,
	var position: Int? = null,
	var beenHelped: Boolean = false,
	@JsonIgnore var queue: CloneQueue? = null,
)