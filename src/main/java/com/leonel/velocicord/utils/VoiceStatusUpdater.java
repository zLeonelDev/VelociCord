package com.leonel.velocicord.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import com.leonel.velocicord.VelociCord;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public class VoiceStatusUpdater {

    private final ProxyServer proxy;
    private final JDA jda;
    private final Logger logger;
    private final VelociCord bridge;
    private final long channelId;
    private final String format;
    private final int intervalSeconds;
    private final int instanceHash; // DIAG
    private ScheduledTask scheduledTask; // DIAG

    public VoiceStatusUpdater(ProxyServer proxy, JDA jda, Logger logger, VelociCord bridge,
            long channelId, String format, int intervalSeconds) {
        this.proxy = proxy;
        this.jda = jda;
        this.logger = logger;
        this.bridge = bridge;
        this.channelId = channelId;
        this.format = format;
        this.intervalSeconds = intervalSeconds;
        this.instanceHash = System.identityHashCode(this); // DIAG
        logger.info("[DIAG] VoiceStatusUpdater creado. instanceHash=" + instanceHash // DIAG
                + " channelId=" + channelId + " intervalSeconds=" + intervalSeconds); // DIAG
    }

    public void start() {
        // DIAG
        logger.info("[DIAG] VoiceStatusUpdater#" + instanceHash + " start() llamado. intervalSeconds="
                + intervalSeconds);
        scheduledTask = proxy.getScheduler().buildTask(bridge, this::updateVoiceStatus)
                .repeat(intervalSeconds, TimeUnit.SECONDS)
                .schedule();
        logger.info("[DIAG] VoiceStatusUpdater#" + instanceHash + " tarea creada. taskHash="
            + System.identityHashCode(scheduledTask));
        if (bridge.getConfig().isDebug())
            logger.info("VoiceStatusUpdater iniciado. Actualización cada " + intervalSeconds + "s");
    }

    public void shutdown() {
        // DIAG
        logger.info("[DIAG] VoiceStatusUpdater#" + instanceHash + " shutdown() llamado. taskHash="
            + (scheduledTask == null ? "null" : System.identityHashCode(scheduledTask)));
        updateVoiceStatusForced(0);
        if (bridge.getConfig().isDebug())
            logger.info("VoiceStatusUpdater detenido.");
    }

    private void updateVoiceStatus() {
        int count = proxy.getAllPlayers().size();
        // DIAG
        logger.info("[DIAG] VoiceStatusUpdater#" + instanceHash + " updateVoiceStatus() entry."
                + " intervalSeconds=" + intervalSeconds
                + " ts=" + System.currentTimeMillis()
                + " count=" + count);
        updateVoiceStatusForced(count);
    }

    private void updateVoiceStatusForced(int count) {
        String newName = format.replace("%count%", String.valueOf(count));

        GuildChannel channel = jda.getGuildChannelById(channelId);
        if (channel == null) {
            logger.warn("Canal de voz no encontrado con ID: " + channelId);
            return;
        }

        // Solo VoiceChannel o Category pueden tener el nombre cambiado
        if (!(channel instanceof VoiceChannel)) {
            if (bridge.getConfig().isDebug())
                logger.warn(
                        "El canal con ID " + channelId + " no es un VoiceChannel. No se puede actualizar el nombre.");
            return;
        }

        VoiceChannel voiceChannel = (VoiceChannel) channel;

        if (!voiceChannel.getName().equals(newName)) {
            // DIAG
            logger.info("[DIAG] VoiceStatusUpdater#" + instanceHash
                    + " ANTES de setName().queue(). oldName=\"" + voiceChannel.getName()
                    + "\" newName=\"" + newName + "\" ts=" + System.currentTimeMillis());
            voiceChannel.getManager().setName(newName).queue(
                    success -> {
                        // DIAG
                        logger.info("[DIAG] VoiceStatusUpdater#" + instanceHash
                                + " setName SUCCESS callback. newName=\"" + newName + "\"");
                        if (bridge.getConfig().isDebug())
                            logger.info("Canal de voz actualizado: " + newName);
                    },
                    error -> {
                        // DIAG
                        logger.info("[DIAG] VoiceStatusUpdater#" + instanceHash
                                + " setName ERROR callback. newName=\"" + newName + "\""
                                + " errorClass=" + (error == null ? "null" : error.getClass().getName())
                                + " errorMsg=" + (error == null ? "null" : error.getMessage()));
                        if (bridge.getConfig().isDebug())
                            logger.warn("No se pudo actualizar el canal de voz: " + error.getMessage());
                    });
        } else {
            // DIAG
            logger.info("[DIAG] VoiceStatusUpdater#" + instanceHash
                    + " nombre sin cambios. currentName=\"" + voiceChannel.getName()
                    + "\" targetName=\"" + newName + "\"");
        }
    }
}