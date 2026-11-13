package io.github.warren1001.auriel.d2.clone

import io.github.warren1001.auriel.d2.Core
import io.github.warren1001.auriel.d2.LadderType
import io.github.warren1001.auriel.d2.Mode
import io.github.warren1001.auriel.d2.Platform

class CloneDataHelperSelection(
	platform: Platform? = null,
	mode: Mode? = null,
	core: Core? = null,
	type: LadderType? = null,
): CloneDataSelection(platform, mode, core, type)