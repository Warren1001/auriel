package io.github.warren1001.auriel.d2.clone

import io.github.warren1001.auriel.d2.*

class CloneDataHelpeeSelection(
	platform: Platform? = null,
	var server: Server? = null,
	mode: Mode? = null,
	core: Core? = null,
	type: LadderType? = null,
) : CloneDataSelection(platform, mode, core, type) {
	override fun hasSelectedAll() = super.hasSelectedAll() && server != null
}
