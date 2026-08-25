package com.leonel.velocicord.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.leonel.velocicord.VelociCord;
import com.leonel.velocicord.utils.DiscordMethods;
import com.leonel.velocicord.utils.TextFormatter;
import com.leonel.velocicord.utils.LuckPermsUtil;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.util.Optional;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class VelocityEventListener {
    private final VelociCord bridge;

    public VelocityEventListener(VelociCord bridge) {
        this.bridge = bridge;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (!bridge.getConfig().getBoolean("settings.chatSync", true))
            return;

        Player player = event.getPlayer();
        String serverKey = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("desconocido");

        String group = LuckPermsUtil.getPrimaryGroup(player);
        String formattedGroup = (!group.isBlank() && !group.equalsIgnoreCase("default"))
                ? "**" + capitalize(group) + "**"
                : "";

        String discordMessage = bridge.getConfig().getFormat("discord_to")
                .replace("%server%", serverKey)
                .replace("%user%", player.getUsername())
                .replace("%message%", TextFormatter.toDiscord(event.getMessage()))
                .replace("%role%", formattedGroup);

        long channelId = bridge.getConfig().getChannel(serverKey);
        if (channelId != 0 && bridge.getJDA() != null) {
            DiscordMethods.sendMessage(bridge.getJDA(), channelId, discordMessage, bridge.getConfig().isDebug());
        } else if (bridge.getConfig().isDebug()) {
            bridge.getLogger()
                    .warn("Canal de Discord no configurado o JDA no disponible para el servidor: " + serverKey);
        }
    }

    @Subscribe
    public void onServerJoin(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();
        Optional<RegisteredServer> previousServer = event.getPreviousServer();

        if (!bridge.getConfig().getBoolean("settings.joinLeaveMessages", true))
            return;
        if (!player.hasPermission("velocicord.notify.connection"))
            return;

        // Si el jugador ya estaba en el proxy y cambia de servidor
        previousServer.ifPresent(prevServer -> {
            String prevName = prevServer.getServerInfo().getName();
            // Evitar duplicar mensaje si el servidor de destino es el mismo que el anterior
            // (raro)
            if (prevName.equals(serverName))
                return;

            EmbedBuilder leaveEmbed = new EmbedBuilder()
                    .setDescription("**" + player.getUsername() + "** ha salido de **" + prevName + "**")
                    .setColor(Color.RED);

            long leaveChannelId = bridge.getConfig().getChannel(prevName);
            if (leaveChannelId != 0 && bridge.getJDA() != null) {
                DiscordMethods.sendMessage(bridge.getJDA(), leaveChannelId, leaveEmbed.build(),
                        bridge.getConfig().isDebug());
            }
        });

        // Mensaje de entrada al nuevo servidor
        EmbedBuilder joinEmbed = new EmbedBuilder()
                .setDescription("**" + player.getUsername() + "** se ha unido a **" + serverName + "**")
                .setColor(Color.GREEN);

        long joinChannelId = bridge.getConfig().getChannel(serverName);
        if (joinChannelId != 0 && bridge.getJDA() != null) {
            DiscordMethods.sendMessage(bridge.getJDA(), joinChannelId, joinEmbed.build(), bridge.getConfig().isDebug());
        } else if (bridge.getConfig().isDebug()) {
            bridge.getLogger().warn("Canal de Discord no configurado o JDA no disponible para alertas de conexión.");
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        String serverName = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("desconocido");

        if (!bridge.getConfig().getBoolean("settings.joinLeaveMessages", true))
            return;
        if (!player.hasPermission("velocicord.notify.connection"))
            return;

        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("**" + player.getUsername() + "** ha salido de **" + serverName + "**")
                .setColor(Color.RED);

        long leaveChannelId = bridge.getConfig().getChannel(serverName);
        if (leaveChannelId != 0 && bridge.getJDA() != null) {
            DiscordMethods.sendMessage(bridge.getJDA(), leaveChannelId, embed.build(), bridge.getConfig().isDebug());
        } else if (bridge.getConfig().isDebug()) {
            bridge.getLogger().warn("Canal de Discord no configurado o JDA no disponible para alertas de desconexión.");
        }
    }

    private String capitalize(String input) {
        if (input == null || input.isBlank())
            return "";
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}