package com.kabryxis.auriel.page;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PagedEmbed {
	
	private final List<Page> pages;
	
	private int     currentPageIndex = 0;
	private Page    currentPage      = null;
	private Message message          = null;
	
	PagedEmbed(List<Page> pages) {
		this.pages = pages;
		updatePage();
	}
	
	PagedEmbed(List<Page> pages, BiConsumer<? super PagedEmbed, ? super ReactionAddEvent> finishAction) {
		this.pages = pages;
		updatePage();
	}
	
	public boolean previousPage() {
		boolean b = currentPageIndex > 0;
		if (b) {
			currentPageIndex--;
			updatePage();
			message.removeAllReactions().block();
			message.edit(spec -> spec.setEmbed(getView())).block();
		}
		return b;
	}
	
	public boolean nextPage() {
		boolean b = currentPageIndex < pages.size() - 1;
		if (b) {
			currentPageIndex++;
			updatePage();
			message.removeAllReactions().block();
			message.edit(spec -> spec.setEmbed(getView())).block();
		}
		return b;
	}
	
	public void delete() {
		if (message != null) message.delete().block();
	}
	
	Consumer<? super EmbedCreateSpec> getView() {
		return currentPage.getPageCreator();
	}
	
	void setMessage(Message message) {
		this.message = message;
		addPageReactions();
	}
	
	void addPageReactions() {
		currentPage.getDefaultReactions().forEach(emoji -> message.addReaction(emoji).block());
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
