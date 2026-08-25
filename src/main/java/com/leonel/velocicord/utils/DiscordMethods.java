package com.leonel.velocicord.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiscordMethods {

    private static final Logger logger = LoggerFactory.getLogger(DiscordMethods.class);

    public static void sendMessage(JDA jda, long channelID, Object message, boolean debug) {
        TextChannel textChannel = jda.getTextChannelById(channelID);
        if (textChannel == null) {
            if (debug)
                logger.warn("Canal no encontrado: " + channelID);
            return;
        }

        if (message instanceof MessageEmbed embed) {
            textChannel.sendMessageEmbeds(embed).queue();
            if (debug)
                logger.info("Embed enviado a canal " + channelID);
        } else if (message instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof MessageEmbed) {
            List<MessageEmbed> embeds = list.stream()
                    .filter(e -> e instanceof MessageEmbed)
                    .map(e -> (MessageEmbed) e)
                    .toList();
            textChannel.sendMessageEmbeds(embeds).queue();
            if (debug)
                logger.info(embeds.size() + " embeds enviados a canal " + channelID);
        } else if (message instanceof String text) {
            textChannel.sendMessage(text).queue();
        } else {
            if (debug)
                logger.warn("Tipo de mensaje no soportado: " + message.getClass().getSimpleName());
        }
    }

    public static void sendAutoDeleteMessage(JDA jda, long channelID, Object message, long seconds, boolean debug) {
        TextChannel textChannel = jda.getTextChannelById(channelID);
        if (textChannel == null) {
            if (debug)
                logger.warn("Canal no encontrado para auto-delete: " + channelID);
            return;
        }

        if (message instanceof MessageEmbed embed) {
            textChannel.sendMessageEmbeds(embed)
                    .queue(sentMsg -> sentMsg.delete().queueAfter(seconds, TimeUnit.SECONDS));
            if (debug)
                logger.info("Embed enviado con auto-delete en " + seconds + "s");
        } else if (message instanceof String text) {
            textChannel.sendMessage(text)
                    .queue(sentMsg -> sentMsg.delete().queueAfter(seconds, TimeUnit.SECONDS));
        } else {
            if (debug)
                logger.warn("Tipo de mensaje no soportado para auto-delete: " + message.getClass().getSimpleName());
        }
    }

    public static void shutdownBot(JDA jda, boolean debug) {
        if (jda == null) {
            if (debug)
                logger.warn("JDA es null, no se puede apagar.");
            return;
        }

        try {
            jda.shutdown();
            if (debug)
                logger.info("Bot apagado correctamente.");
        } catch (Exception e) {
            logger.error("Error al apagar el bot.", e);
        }
    }
}