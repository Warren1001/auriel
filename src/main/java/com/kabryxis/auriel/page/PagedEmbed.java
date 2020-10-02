package com.kabryxis.auriel.page;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PagedEmbed {
	
	private final List<Page> pages;
	
	private int     currentPageIndex = 0;
	private Page    currentPage      = null;
	private Message message          = null;
	
	PagedEmbed(List<Page> pages) {
		this.pages = pages;
		updatePage();
	}
	
	public boolean previousPage() {
		boolean b = currentPageIndex > 0;
		if (b) {
			currentPageIndex--;
			updatePage();
			Flux.concat(message.removeAllReactions(), message.edit(spec -> spec.setEmbed(getView()))).subscribe();
		}
		return b;
	}
	
	public boolean nextPage() {
		boolean b = currentPageIndex < pages.size() - 1;
		if (b) {
			currentPageIndex++;
			updatePage();
			Flux.concat(message.removeAllReactions(), message.edit(spec -> spec.setEmbed(getView()))).subscribe();
		}
		return b;
	}
	
	public void delete() {
		if (message != null) message.delete().subscribe();
	}
	
	Consumer<? super EmbedCreateSpec> getView() {
		return currentPage.getPageCreator();
	}
	
	void setMessage(Message message) {
		this.message = message;
		addPageReactions();
	}
	
	void addPageReactions() {
		Flux.concat(currentPage.getDefaultReactions().stream().map(message::addReaction).collect(Collectors.toList())).subscribe();
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
