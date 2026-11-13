package io.github.warren1001.auriel.d2.clone

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.warren1001.auriel.d2.Core
import io.github.warren1001.auriel.d2.LadderType
import io.github.warren1001.auriel.d2.Mode
import io.github.warren1001.auriel.d2.Platform

open class CloneDataSelection(
	var platform: Platform? = null,
	var mode: Mode? = null,
	var core: Core? = null,
	var type: LadderType? = null,
) {
	open fun hasSelectedAll() = platform != null && mode != null && core != null && type != null
	@JsonIgnore
	open fun getUniqueIdentifier() = "$platform+$mode+$core+$type"
}