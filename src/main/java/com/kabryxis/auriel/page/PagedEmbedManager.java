package com.kabryxis.auriel.page;

import com.kabryxis.auriel.Auriel;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PagedEmbedManager {
	
	private final Map<Snowflake, PagedEmbed> pagedEmbedIds = Collections.synchronizedMap(new HashMap<>());
	
	public PagedEmbedManager(Auriel bot) {
		bot.getGateway()
				.on(ReactionAddEvent.class)
				.doOnError(Throwable::printStackTrace)
				.filter(event -> !event.getUserId().equals(bot.getGateway().getSelfId()))
				.filter(event -> pagedEmbedIds.containsKey(event.getMessageId()))
				.subscribe(event -> pagedEmbedIds.get(event.getMessageId()).onReact(event));
		bot.getGateway()
				.on(MessageDeleteEvent.class)
				.doOnError(Throwable::printStackTrace)
				.subscribe(event -> pagedEmbedIds.remove(event.getMessageId()));
		bot.getGateway()
				.on(MessageUpdateEvent.class)
				.doOnError(Throwable::printStackTrace)
				.filter(event -> pagedEmbedIds.containsKey(event.getMessageId()))
				.map(event -> pagedEmbedIds.get(event.getMessageId()))
				.subscribe(PagedEmbed::addPageReactions);
	}
	
	public void sendPagedEmbed(MessageChannel channel, Page... pages) {
		
		PagedEmbed embed = new PagedEmbed(Arrays.asList(pages));
		channel.createEmbed(embed.getView()).subscribe(message -> {
			embed.setMessage(message);
			pagedEmbedIds.put(message.getId(), embed);
		});
		
	}
	
}
