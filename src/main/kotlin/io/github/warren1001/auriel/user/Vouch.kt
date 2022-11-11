package io.github.warren1001.auriel.user

data class Vouch(val id: Long, val vouchedBy: String, val reason: String, val timestamp: Long)