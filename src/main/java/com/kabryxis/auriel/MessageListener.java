package com.kabryxis.auriel;

import com.kabryxis.auriel.page.Page;
import com.kabryxis.auriel.team.Hero;
import com.kabryxis.auriel.team.Ladder;
import com.kabryxis.auriel.team.TeamContext;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;

import java.util.function.Consumer;

public class MessageListener implements Consumer<MessageCreateEvent> {
	
	private static final ReactionEmoji EMOJI_ONE   = ReactionEmoji.unicode("1️⃣");
	private static final ReactionEmoji EMOJI_TWO   = ReactionEmoji.unicode("2️⃣");
	private static final ReactionEmoji EMOJI_THREE = ReactionEmoji.unicode("3️⃣");
	private static final ReactionEmoji EMOJI_FOUR  = ReactionEmoji.unicode("4️⃣");
	private static final ReactionEmoji EMOJI_FIVE  = ReactionEmoji.unicode("5️⃣");
	private static final ReactionEmoji EMOJI_SIX   = ReactionEmoji.unicode("6️⃣");
	private static final ReactionEmoji EMOJI_SEVEN = ReactionEmoji.unicode("7️⃣");
	
	private final Auriel bot;
	
	public MessageListener(Auriel bot) {
		this.bot = bot;
	}
	
	@Override
	public void accept(MessageCreateEvent event) { // TODO eventually overhaul
		Message        message  = event.getMessage();
		String[]       args     = message.getContent().split(" ", 2);
		String         command  = args[0].toLowerCase();
		String         argument = args.length == 2 ? args[1] : null;
		MessageChannel channel  = message.getChannel().block();
		switch (command) {
			case "!ping":
				channel.createMessage("Pong!").block();
				break;
			case "!stop":
				bot.getLadderTeamManager().save();
				bot.getGateway().logout().block();
				break;
			case "!join":
				TeamContext.Builder builder = TeamContext
						.builder(event.getMember().orElseThrow(() -> new IllegalStateException("A message was sent without a member?")).getId());
				bot.getEmbedManager().sendPagedEmbed(channel,
						Page.builder().setPageSupplier(spec -> spec.setDescription(":one: USEast\n:two: USWest\n:three: Europe\n:four: Asia"))
								.addReactionActionNext(EMOJI_ONE, e -> builder.realm(Ladder.Realm.EAST))
								.addReactionActionNext(EMOJI_TWO, e -> builder.realm(Ladder.Realm.WEST))
								.addReactionActionNext(EMOJI_THREE, e -> builder.realm(Ladder.Realm.EUROPE))
								.addReactionActionNext(EMOJI_FOUR, e -> builder.realm(Ladder.Realm.ASIA)).build(),
						Page.builder().setPageSupplier(spec -> spec.setDescription(":one: Softcore\n:two: Hardcore")).addDefaultPreviousAction()
								.addReactionActionNext(EMOJI_ONE, e -> builder.core(Ladder.Core.SOFTCORE))
								.addReactionActionNext(EMOJI_TWO, e -> builder.core(Ladder.Core.HARDCORE)).build(),
						Page.builder().setPageSupplier(spec -> spec.setDescription(":one: Expansion\n:two: Classic")).addDefaultPreviousAction()
								.addReactionActionNext(EMOJI_ONE, e -> builder.type(Ladder.Type.EXPANSION))
								.addReactionActionNext(EMOJI_TWO, e -> builder.type(Ladder.Type.CLASSIC)).build(),
						Page.builder().setPageSupplier(spec -> spec.setDescription(
								"React to the characters you want to play in the order of which you who you want to play most.\n" +
										"If you do not want to play a specific character, do not react to that character.\n\n" +
										":one: Amazon\n:two: Assassin\n:three: Necromancer\n:four: Barbarian\n:five: Paladin\n" +
										":six: Sorceress\n:seven: Druid"))
								.addReactionActionPrevious(ReactionEmoji.unicode("⬅️"), e -> builder.clearHeroes())
								.addReactionAction(EMOJI_ONE, e -> builder.hero(Hero.AMAZON))
								.addReactionAction(EMOJI_TWO, e -> builder.hero(Hero.ASSASSIN))
								.addReactionAction(EMOJI_THREE, e -> builder.hero(Hero.NECROMANCER))
								.addReactionAction(EMOJI_FOUR, e -> builder.hero(Hero.BARBARIAN))
								.addReactionAction(EMOJI_FIVE, e -> builder.hero(Hero.PALADIN))
								.addReactionAction(EMOJI_SIX, e -> builder.hero(Hero.SORCERESS))
								.addReactionAction(EMOJI_SEVEN, e -> builder.hero(Hero.DRUID)).addDefaultNextAction().build(),
						Page.builder().setPageSupplier(spec -> spec.setDescription(
								"Would you prefer to create a new team playing as your primary character " +
										"before joining a pre-existing team as your non-primary characters?\n" +
										"*Yes* will put you into a new team as your primary character in the event there are no existing teams that have room for your primary character.\n" +
										"*No* will put you into a pre-existing team as one of your non-primary characters in the event there are no existing teams that have room for your primary character.\n" +
										"If there are no pre-existing teams with room for any of your selected characters, a new one will be created and you will join as your primary character regardless.\n\n" +
										":white_check_mark: Yes\n:x: No")).addReactionActionDelete(ReactionEmoji.unicode("✅"), e -> {
							builder.prefersPrimary();
							bot.getLadderTeamManager().joinTeam(builder.build());
						}).addReactionActionDelete(ReactionEmoji.unicode("❌"), e -> bot.getLadderTeamManager().joinTeam(builder.build())).build());
				break;
			case "!leave":
				bot.getLadderTeamManager()
						.leaveTeam(event.getMember().orElseThrow(() -> new IllegalStateException("A message was sent without a member?")).getId());
				break;
			case "!purge":
				bot.getLadderTeamManager().purge();
				break;
			default:
				break;
		}
	}
	
}
