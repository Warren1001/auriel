package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.Auriel
import net.dv8tion.jda.api.Permission

data class ConfigData(val key: String, val description: String, val permission: Permission = Permission.BAN_MEMBERS,
                      val allowedTypes: Set<ConfigDataType>, val defaultValue: Any, val setDefault: (Auriel, String, Any) -> Unit,
                      val saveChanges: (ConfigContext) -> Unit,
                      val modifyValue: (ConfigContext, String) -> Boolean, val valueChanged: (ConfigContext) -> Unit) {
	
	fun hasAllowedType(type: ConfigDataType) = allowedTypes.contains(type)
	
	fun isAllowedType(value: Any) = allowedTypes.any { it.isType(value) }
	
	fun prettyPrint(): String {
		return "**$key**\n" +
				"Description: $description\n" +
				"Permission: ${permission.name}\n" +
				"Allowed Types: ${allowedTypes.map { it.configSubCommands }.flatten().joinToString { ", " }}\n" +
				"Default Value: $defaultValue"
	}

}