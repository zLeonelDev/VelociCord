package com.leonel.velocicord;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.leonel.velocicord.commands.VelociCordCommand;
import com.leonel.velocicord.listeners.DiscordListener;
import com.leonel.velocicord.listeners.VelocityEventListener;
import com.leonel.velocicord.utils.DiscordMethods;
import com.leonel.velocicord.utils.LuckPermsUtil;
import com.leonel.velocicord.utils.VoiceStatusUpdater;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.EnumSet;

@Plugin(id = "velocicord", name = "VelociCord", version = VelociCord.VERSION, description = "Sincronización entre Velocity y Discord", authors = {
        "LeonelDev" })
public final class VelociCord {

    public static final String VERSION = "1.0.1";

    private final ProxyServer server;
    private final Logger logger;
    private final Path configDirectory;

    private Config config;
    private JDA jda;
    private VoiceStatusUpdater voiceUpdater;

    @Inject
    public VelociCord(ProxyServer server, Logger logger, @DataDirectory Path configDirectory) {
        this.server = server;
        this.logger = logger;
        this.configDirectory = configDirectory;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        logger.info("🚀 Iniciando VelociCord v" + VERSION);

        // Cargar configuración
        this.config = new Config(configDirectory, "config.yml", logger, server);

        // Inicializar LuckPerms
        LuckPermsUtil.init();

        // Iniciar Discord
        try {
            loadDiscord();
        } catch (Exception e) {
            logger.error("Error al cargar Discord:", e);
        }

        // Estado del bot
        if (config.getUseStatus()) {
            updateGameStatus(config.getGameStatus());
        }

        // Activar VoiceStatus
        if (config.isVoiceStatusEnabled()) {
            // DIAG
            logger.info("[DIAG] VelociCord.onProxyInit: creando VoiceStatusUpdater (previo="
                    + (voiceUpdater == null ? "null" : "voiceUpdater#" + System.identityHashCode(voiceUpdater)) + ")");
            voiceUpdater = new VoiceStatusUpdater(
                    server,
                    jda,
                    logger,
                    this,
                    config.getVoiceStatusChannelId(),
                    config.getVoiceStatusFormat(),
                    config.getVoiceStatusInterval());
            voiceUpdater.start();
            logger.info("VoiceStatus dinámico activado.");
        }

        // Registrar listeners
        server.getEventManager().register(this, new VelocityEventListener(this));

        // Registrar comandos
        CommandManager commandManager = server.getCommandManager();
        CommandMeta metaV = commandManager.metaBuilder("velocicord")
                .aliases("vc", "vcord", "discord")
                .build();
        commandManager.register(metaV, new VelociCordCommand(this));

        logger.info("Comando /velocicord registrado correctamente.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (voiceUpdater != null) {
            voiceUpdater.shutdown();
            if (config.isDebug())
                logger.info("VoiceStatus actualizado a 0 jugadores por apagado del proxy.");
        }

        if (jda != null) {
            DiscordMethods.shutdownBot(jda, config.isDebug());
            if (config.isDebug())
                logger.info("Bot de Discord detenido correctamente.");
        }
    }

    private void loadDiscord() {
        String token = config.getDiscordToken();
        if (token == null || token.isBlank() || token.equalsIgnoreCase("tu-token-aquí")) {
            logger.warn("Token de Discord inválido. El bot no se iniciará.");
            return;
        }

        try {
            setupDiscord(token, new DiscordListener(this));
            logger.info("Bot de Discord cargado correctamente.");
        } catch (Exception e) {
            logger.error("Error durante la inicialización de Discord:", e);
        }
    }

    private void setupDiscord(String token, Object... listeners) {
        try {
            jda = JDABuilder.create(token, EnumSet.allOf(GatewayIntent.class))
                    .setAutoReconnect(true)
                    .addEventListeners(listeners)
                    .setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .build()
                    .awaitReady();

            logger.info("JDA inicializado correctamente.");
        } catch (Exception e) {
            logger.error("Error al conectar con Discord:", e);
        }
    }

    public void doReload() {
        logger.info("[DIAG] VelociCord.doReload() INICIO. voiceUpdater previo="
                + (voiceUpdater == null ? "null" : "voiceUpdater#" + System.identityHashCode(voiceUpdater)));
        try {
            config.loadConfig();
            DiscordMethods.shutdownBot(jda, config.isDebug());
            setupDiscord(config.getDiscordToken(), new DiscordListener(this));

            if (voiceUpdater != null) {
                logger.info("[DIAG] VelociCord.doReload(): llamando shutdown() de voiceUpdater#"
                        + System.identityHashCode(voiceUpdater));
                voiceUpdater.shutdown();
                if (config.isVoiceStatusEnabled()) {
                    logger.info("[DIAG] VelociCord.doReload(): creando NUEVO VoiceStatusUpdater");
                    voiceUpdater = new VoiceStatusUpdater(
                            server,
                            jda,
                            logger,
                            this,
                            config.getVoiceStatusChannelId(),
                            config.getVoiceStatusFormat(),
                            config.getVoiceStatusInterval());
                    voiceUpdater.start();
                }
            }

            logger.info("VelociCord recargado correctamente.");
        } catch (Exception e) {
            logger.error("Error al recargar VelociCord:", e);
        }
        logger.info("[DIAG] VelociCord.doReload() FIN. voiceUpdater ahora="
                + (voiceUpdater == null ? "null" : "voiceUpdater#" + System.identityHashCode(voiceUpdater)));
    }

    public void updateGameStatus(String status) {
        if (jda == null) {
            logger.warn("No se puede actualizar el estado: JDA no está inicializado.");
            return;
        }

        String lower = status.toLowerCase().trim();
        String clean = status.replaceFirst("(?i)playing|watching|listening|custom", "").trim();

        Activity activity = switch (lower.split(" ")[0]) {
            case "playing" -> Activity.playing(clean);
            case "watching" -> Activity.watching(clean);
            case "listening" -> Activity.listening(clean);
            case "custom" -> Activity.customStatus(clean);
            default -> null;
        };

        if (activity != null) {
            jda.getPresence().setActivity(activity);
            logger.info("Estado del bot actualizado: " + activity.getName());
        } else {
            logger.warn("Modo de estado desconocido: " + status);
        }
    }

    public Config getConfig() {
        return config;
    }

    public ProxyServer getProxyServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public JDA getJDA() {
        return jda;
    }
}