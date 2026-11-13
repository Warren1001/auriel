package io.github.warren1001.auriel.d2.clone

import com.fasterxml.jackson.annotation.JsonIgnore

data class CloneInfo(
    val requesterId: String,
    val position: Int,
    @JsonIgnore var helperId: String = "",
    var platform: String,
    var mode: String,
    var core: String,
    var ladder: String,
    var server: String,
    var gameName: String,
    var password: String,
    var spawned: String,
    var furthestAct: String,
    var otherInfo: String = "None"
)