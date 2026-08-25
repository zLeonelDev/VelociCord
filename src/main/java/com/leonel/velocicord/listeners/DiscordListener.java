package com.leonel.velocicord.listeners;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.leonel.velocicord.VelociCord;
import com.leonel.velocicord.utils.TextFormatter;
import com.leonel.velocicord.utils.DiscordMethods;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordListener extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(DiscordListener.class);
    private final VelociCord bridge;

    public DiscordListener(VelociCord bridge) {
        this.bridge = bridge;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        event.getJDA().getGuilds().forEach(guild -> guild.loadMembers());
        logger.info("Bot de Discord conectado correctamente.");
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = message.getAuthor();
        if (author == null || author.isBot() || bridge.getJDA().getSelfUser().getIdLong() == author.getIdLong())
            return;

        TextChannel channel = (TextChannel) event.getChannel();
        Member member = event.isFromGuild() ? event.getMember() : null;

        String name = member != null ? member.getEffectiveName() : author.getName();
        String content = message.getContentDisplay();
        boolean canUseColors = member != null && member.hasPermission(Permission.MESSAGE_MANAGE);
        String sanitizedContent = canUseColors ? content : TextFormatter.toDiscord(content);

        // Rol más alto
        Role topRole = member != null && !member.getRoles().isEmpty() ? member.getRoles().get(0) : null;
        String roleName = topRole != null ? topRole.getName() : "Sin rol";
        String roleColor = topRole != null && topRole.getColor() != null
                ? String.format("#%02x%02x%02x", topRole.getColor().getRed(), topRole.getColor().getGreen(),
                        topRole.getColor().getBlue())
                : "#aaaaaa";

        // Formato
        String format = bridge.getConfig().getFormat("discord_from")
                .replace("%user%", name)
                .replace("%message%", sanitizedContent)
                .replace("%role%", roleName)
                .replace("%color%", roleColor);

        Component componentMsg = TextFormatter.toMinecraft(format);

        long channelId = channel.getIdLong();
        boolean sent = false;

        // Canal global
        if (channelId == bridge.getConfig().getChannel("global")) {
            bridge.getProxyServer().sendMessage(componentMsg);
            sent = true;
        } else {
            // Otros servidores
            for (RegisteredServer server : bridge.getProxyServer().getAllServers()) {
                long serverChannelId = bridge.getConfig().getChannel(server.getServerInfo().getName());
                if (serverChannelId != 0 && channelId == serverChannelId) {
                    for (Player player : server.getPlayersConnected()) {
                        player.sendMessage(Identity.nil(), componentMsg);
                    }
                    sent = true;
                    break;
                }
            }
        }
    }
}