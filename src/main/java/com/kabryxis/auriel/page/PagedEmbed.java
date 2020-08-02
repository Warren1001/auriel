package com.kabryxis.auriel.page;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;
import java.util.function.Consumer;

public class PagedEmbed implements Consumer<EmbedCreateSpec> {
	
	private final List<Page> pages;
	
	private int     currentPageIndex = 0;
	private Page    currentPage      = null;
	private Message message          = null;
	
	PagedEmbed(List<Page> pages) {
		this.pages = pages;
		updatePage();
	}
	
	@Override
	public void accept(EmbedCreateSpec spec) {
		currentPage.accept(spec);
		if (message != null) currentPage.getDefaultReactions().forEach(emoji -> message.addReaction(emoji).block());
	}
	
	void setMessage(Message message) {
		this.message = message;
		currentPage.getDefaultReactions().forEach(emoji -> message.addReaction(emoji).block());
	}
	
	public boolean previousPage() {
		boolean b = currentPageIndex > 0;
		if (b) {
			currentPageIndex--;
			updatePage();
			message.removeAllReactions().block();
			message.edit(spec -> spec.setEmbed(this)).block();
		}
		return b;
	}
	
	public boolean nextPage() {
		boolean b = currentPageIndex < pages.size() - 1;
		if (b) {
			currentPageIndex++;
			updatePage();
			message.removeAllReactions().block();
			message.edit(spec -> spec.setEmbed(this)).block();
		}
		return b;
	}
	
	void onReact(ReactionAddEvent event) {
		if (currentPage.getDefaultReactions().contains(event.getEmoji())) {
			currentPage.onReact(this, event);
		}
	}
	
	private void updatePage() {
		currentPage = pages.get(currentPageIndex);
	}
	
}
