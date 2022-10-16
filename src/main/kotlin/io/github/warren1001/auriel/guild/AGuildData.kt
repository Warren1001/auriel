package io.github.warren1001.auriel.guild

import io.github.warren1001.auriel.util.filter.Filter
import io.github.warren1001.auriel.util.filter.WordFilter
import io.github.warren1001.auriel.util.youtube.YoutubeData

data class AGuildData(val _id: String) {
	
	val wordFilters = mutableSetOf<WordFilter>()
	val spamFilters = mutableSetOf<Filter>()
	var logChannelId: String? = null
	var youtubeData: YoutubeData = YoutubeData()
	var crosspost = false
	var fallbackChannelId: String? = null
	var cloneHelperMessage: String = "**ATTENTION!**\n" +
			"Clicking the button will open up a form which will immediately start helping people. Do not click it if you are not ready to help people.\n" +
			"Upon clicking the button, you will be given the requester's information. Click `Submit` when you have finished helping them.\n" +
			"Do not click `Submit` before you have finished helping them. If you need out of the form temporarily, click `Cancel` or outside of the form.\n" +
			"Clicking the button again will return you to the same requester's information incase you need to view it again.\n\n" +
			"People remaining: %remaining%"
	var cloneHelpeeMessage: String = "**ATTENTION, PLEASE READ IF YOU ARE NEW TO DCLONE OR REQUESTING HELP!**\n" +
			"Diablo Clone will spawn in the specified region in Hell difficulty only! You MUST be in a game before `Diablo Invades Sanctuary` happens.\n" +
			"If you plan to request help, please follow these steps:\n" +
			"- Create a lobby game, the quick play games are hard to join.\n" +
			"- Make sure the game is in Hell!\n" +
			"- Disable the level restriction! We may not be able to join your game if you forget this and then you're out of luck.\n" +
			"- Make sure the allowed player amount for the game is higher than 1, or else nobody can join you.\n" +
			"- Do not make the game name or password obvious. Thieves actively try to join common games to steal Annihilus's. Create an elaborate password.\n" +
			"- Click the button below once you are in the game and input your information to join the queue. Then wait patiently for a direct message indicating that your helper is on the way.\n" +
			"- Please do not spawn Diablo Clone yourself unless you are seriously intent on trying to kill him first. Helpers have their preferred spawn locations.\n" +
			"- No, Diablo Clone does **not** despawn under any circumstances. As long as you are in the game, he will always be there.\n\n" +
			"Currently assisting position #%position% in queue."
	var cloneHelpeeRequestButton: String = "I need help w/ DClone"
	var cloneHelpeeCancelButton: String = "I no longer need help"
	var cloneHelperBeginButton: String = "Begin helping"
	var cloneHelperMentionButton: String = "Get mention"
	
}