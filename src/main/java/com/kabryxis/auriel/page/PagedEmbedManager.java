package com.kabryxis.auriel.page;

import com.kabryxis.auriel.Auriel;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PagedEmbedManager {
	
	private final Map<Snowflake, PagedEmbed> pagedEmbedIds = new HashMap<>();
	
	public PagedEmbedManager(Auriel bot) {
		bot.getGateway().on(ReactionAddEvent.class).doOnError(Throwable::printStackTrace).subscribe(event -> {
			if (event.getUserId().equals(bot.getGateway().getSelfId())) return;
			PagedEmbed embed = pagedEmbedIds.get(event.getMessageId());
			if (embed != null) {
				embed.onReact(event);
			}
		});
		bot.getGateway()
				.on(MessageDeleteEvent.class)
				.doOnError(Throwable::printStackTrace)
				.subscribe(event -> pagedEmbedIds.remove(event.getMessageId()));
		bot.getGateway().on(MessageUpdateEvent.class).doOnError(Throwable::printStackTrace).subscribe(event -> {
			PagedEmbed embed = pagedEmbedIds.get(event.getMessageId());
			if (embed != null) embed.addPageReactions();
		});
	}
	
	public void sendPagedEmbed(MessageChannel channel, Page... pages) {
		
		PagedEmbed embed   = new PagedEmbed(Arrays.asList(pages));
		Message    message = channel.createEmbed(embed.getView()).block();
		
		embed.setMessage(message);
		pagedEmbedIds.put(message.getId(), embed);
	}
	
}
