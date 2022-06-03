package io.github.warren1001.auriel.user

import discord4j.common.util.Snowflake

data class ModerationInfo(val by: Snowflake, val info: String)