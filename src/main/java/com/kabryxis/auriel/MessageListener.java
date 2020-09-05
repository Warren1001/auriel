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
				TeamContext.Builder builder = TeamContext.builder(event.getMember()
						.orElseThrow(() -> new IllegalStateException("A message was sent without a member?"))
						.getId());
				bot.getEmbedManager().sendPagedEmbed(channel,
						Page.builder()
								.setPageSupplier(spec -> spec.setDescription(
										"Select :one: for USEast\nSelect :two: for USWest\nSelect :three: for Europe\nSelect :four: for " + "Asia"))
								.addReactionActionNext(EMOJI_ONE, e -> builder.realm(Ladder.Realm.EAST))
								.addReactionActionNext(EMOJI_TWO, e -> builder.realm(Ladder.Realm.WEST))
								.addReactionActionNext(EMOJI_THREE, e -> builder.realm(Ladder.Realm.EUROPE))
								.addReactionActionNext(EMOJI_FOUR, e -> builder.realm(Ladder.Realm.ASIA))
								.build(),
						Page.builder()
								.setPageSupplier(spec -> spec.setDescription("Select :one: for Softcore\nSelect :two: for Hardcore"))
								.addDefaultPreviousAction()
								.addReactionActionNext(EMOJI_ONE, e -> builder.core(Ladder.Core.SOFTCORE))
								.addReactionActionNext(EMOJI_TWO, e -> builder.core(Ladder.Core.HARDCORE))
								.build(),
						Page.builder()
								.setPageSupplier(spec -> spec.setDescription("Select :one: for Expansion\nSelect :two: for Classic"))
								.addDefaultPreviousAction()
								.addReactionActionNext(EMOJI_ONE, e -> builder.type(Ladder.Type.EXPANSION))
								.addReactionActionNext(EMOJI_TWO, e -> builder.type(Ladder.Type.CLASSIC))
								.build(),
						Page.builder()
								.setPageSupplier(spec -> spec.setDescription(
										"Choose the characters that you want to play, multiple options allowed\nYour preference for character is " +
												"given by the order at which you react\n\nReact with :one: for Amazon\nReact with :two: for " +
												"Assassin\nReact with :three: for Necromancer\nReact with :four: for Barbarian\nReact with :five: " +
												"for Paladin\nReact with :six: for Sorceress\nReact with :seven: for Druid"))
								.addReactionActionPrevious(ReactionEmoji.unicode("⬅️"), e -> builder.clearHeroes())
								.addReactionAction(EMOJI_ONE, e -> builder.hero(Hero.AMAZON))
								.addReactionAction(EMOJI_TWO, e -> builder.hero(Hero.ASSASSIN))
								.addReactionAction(EMOJI_THREE, e -> builder.hero(Hero.NECROMANCER))
								.addReactionAction(EMOJI_FOUR, e -> builder.hero(Hero.BARBARIAN))
								.addReactionAction(EMOJI_FIVE, e -> builder.hero(Hero.PALADIN))
								.addReactionAction(EMOJI_SIX, e -> builder.hero(Hero.SORCERESS))
								.addReactionAction(EMOJI_SEVEN, e -> builder.hero(Hero.DRUID))
								.addReactionActionDelete(ReactionEmoji.unicode("✅"), e -> bot.getLadderTeamManager().joinTeam(builder.build()))
								.build());
				break;
			case "!leave":
				
				break;
			case "!purge":
				bot.getLadderTeamManager().purge();
				break;
			default:
				break;
		}
	}
	
}
