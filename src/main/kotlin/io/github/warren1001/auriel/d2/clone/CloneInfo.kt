package io.github.warren1001.auriel.d2.clone

data class CloneInfo(val requesterId: String, val position: Int, var helperId : String = "",
                     var gameName: String, var password: String, var spawned: String, var furthestAct: String, var otherInfo: String = "None")