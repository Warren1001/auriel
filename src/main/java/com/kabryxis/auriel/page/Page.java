package com.kabryxis.auriel.page;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Page {
	
	public static Builder builder() {
		return new Builder();
	}
	
	private final Map<ReactionEmoji, Set<BiConsumer<? super PagedEmbed, ? super ReactionAddEvent>>> reactionActions;
	private final List<ReactionEmoji>                                                               reactionEmojiOrder;
	private final Consumer<? super EmbedCreateSpec>                                                 pageSupplier;
	
	private Page(Map<ReactionEmoji, Set<BiConsumer<? super PagedEmbed, ? super ReactionAddEvent>>> reactionActions,
			List<ReactionEmoji> reactionEmojiOrder, Consumer<? super EmbedCreateSpec> pageSupplier) {
		this.reactionActions = reactionActions;
		this.reactionEmojiOrder = reactionEmojiOrder;
		this.pageSupplier = pageSupplier;
	}
	
	void onReact(PagedEmbed embed, ReactionAddEvent event) {
		Set<BiConsumer<? super PagedEmbed, ? super ReactionAddEvent>> actions = reactionActions.get(event.getEmoji());
		if (actions != null) {
			actions.forEach(consumer -> consumer.accept(embed, event));
		}
	}
	
	List<ReactionEmoji> getDefaultReactions() {
		return reactionEmojiOrder;
	}
	
	Consumer<? super EmbedCreateSpec> getPageCreator() {
		return pageSupplier;
	}
	
	public static class Builder {
		
		private static final ReactionEmoji                                            RIGHT_ARROW_EMOJI  = ReactionEmoji.unicode("➡");
		private static final BiConsumer<? super PagedEmbed, ? super ReactionAddEvent> RIGHT_ARROW_ACTION = (embed, event) -> embed.nextPage();
		private static final ReactionEmoji                                            LEFT_ARROW_EMOJI   = ReactionEmoji.unicode("⬅");
		private static final BiConsumer<? super PagedEmbed, ? super ReactionAddEvent> LEFT_ARROW_ACTION  = (embed, event) -> embed.previousPage();
		
		private final Map<ReactionEmoji, Set<BiConsumer<? super PagedEmbed, ? super ReactionAddEvent>>> reactionActions    = new HashMap<>();
		private final List<ReactionEmoji>                                                               reactionEmojiOrder = new ArrayList<>();
		
		private Builder() {}
		
		public Builder addReactionAction(ReactionEmoji emoji, BiConsumer<? super PagedEmbed, ? super ReactionAddEvent> action) {
			reactionActions.computeIfAbsent(emoji, ignore -> new HashSet<>()).add(action);
			reactionEmojiOrder.add(emoji);
			return this;
		}
		
		public Builder addReactionAction(ReactionEmoji emoji, Consumer<? super ReactionAddEvent> action) {
			return addReactionAction(emoji, (embed, event) -> action.accept(event));
		}
		
		public Builder addReactionActionPrevious(ReactionEmoji emoji, Consumer<? super ReactionAddEvent> action) {
			return addReactionAction(emoji, (embed, event) -> {
				embed.previousPage();
				action.accept(event);
			});
		}
		
		public Builder addReactionActionNext(ReactionEmoji emoji, Consumer<? super ReactionAddEvent> action) {
			return addReactionAction(emoji, (embed, event) -> {
				embed.nextPage();
				action.accept(event);
			});
		}
		
		public Builder addReactionActionDelete(ReactionEmoji emoji, Consumer<? super ReactionAddEvent> action) {
			return addReactionAction(emoji, (embed, event) -> {
				embed.delete();
				action.accept(event);
			});
		}
		
		public Builder addDefaultNextAction() {
			return addReactionAction(RIGHT_ARROW_EMOJI, RIGHT_ARROW_ACTION);
		}
		
		public Builder addDefaultPreviousAction() {
			return addReactionAction(LEFT_ARROW_EMOJI, LEFT_ARROW_ACTION);
		}
		
		private Consumer<? super EmbedCreateSpec> pageSupplier = null;
		
		public Builder setPageSupplier(Consumer<? super EmbedCreateSpec> pageSupplier) {
			this.pageSupplier = pageSupplier;
			return this;
		}
		
		public Page build() {
			return new Page(reactionActions, reactionEmojiOrder, pageSupplier);
		}
		
	}
	
}
