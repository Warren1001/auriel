package io.github.warren1001.auriel.d2.clone

import com.fasterxml.jackson.annotation.JsonIgnore

data class CloneDataHelper(
	var selections: CloneDataHelperSelection? = null,
	var completed: Int = 0,
	var restricted: Boolean = false,
	var idPersonalThread: String? = null,
	@JsonIgnore var queue: CloneQueue? = null,
	//var completedSession: MutableList<String>? = null,
)