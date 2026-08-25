package com.leonel.velocicord;

import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {

    private final Logger logger;
    private final ProxyServer server;
    private final File configFile;
    private Map<String, Object> configData;
    private final Yaml yaml;

    public Config(Path configDir, String fileName, Logger logger, ProxyServer server) {
        this.logger = logger;
        this.server = server;

        if (!configDir.toFile().exists()) {
            configDir.toFile().mkdirs();
        }

        this.configFile = new File(configDir.toFile(), fileName);

        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);

        if (!configFile.exists()) {
            logger.info("Archivo de configuración no encontrado. Copiando config.yml desde el JAR...");
            copyDefaultConfigFromJar(fileName);
        }

        loadConfig();
        updateServerChannels();
    }

    /** Copia config.yml desde el JAR si no existe */
    private void copyDefaultConfigFromJar(String fileName) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (in == null) {
                logger.warn("No se encontró config.yml dentro del JAR, se generará uno por defecto.");
                configData = generateDefaultConfig();
                saveConfig();
                return;
            }

            try (OutputStream out = new FileOutputStream(configFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            logger.info("config.yml copiado correctamente desde el JAR.");
        } catch (IOException e) {
            logger.error("Error copiando config.yml desde el JAR", e);
            configData = generateDefaultConfig();
            saveConfig();
        }
    }

    /** Genera configuración por defecto si no hay un config.yml */
    private Map<String, Object> generateDefaultConfig() {
        Map<String, Object> root = new LinkedHashMap<>();

        // Añadir sección settings
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("joinLeaveMessages", true);
        settings.put("chatSync", true);
        settings.put("debug", false);

        Map<String, Object> discord = new LinkedHashMap<>();
        discord.put("botToken", "tu-token-aquí");
        discord.put("status", "playing Velocity Server");
        discord.put("useConfigStatus", true);

        Map<String, Object> channels = new LinkedHashMap<>();
        channels.put("global", 0L);

        Map<String, Object> voiceStatus = new LinkedHashMap<>();
        voiceStatus.put("enabled", false);
        voiceStatus.put("channelId", 0L);
        voiceStatus.put("format", "🎮 Jugadores: %count%");
        voiceStatus.put("updateIntervalSeconds", 30);

        Map<String, Object> format = new LinkedHashMap<>();
        Map<String, Object> discordFormat = new LinkedHashMap<>();
        discordFormat.put("from",
                "<gray>[<blue><bold>DISCORD</bold><gray>] <color:%color%>%role%</color> <gray>| <color:%color%>%user%</color> » <white>%message%");
        discordFormat.put("to", "%role% %user% » %message%");
        format.put("discord", discordFormat);

        root.put("settings", settings);
        root.put("discord", discord);
        root.put("channels", channels);
        root.put("voiceStatus", voiceStatus);
        root.put("format", format);

        return root;
    }

    /** Carga el config.yml */
    public void loadConfig() {
        try {
            this.configData = yaml.load(new FileReader(configFile));
            if (this.configData == null) {
                this.configData = generateDefaultConfig();
                saveConfig();
            }
        } catch (IOException e) {
            logger.error("Error cargando config.yml", e);
            this.configData = generateDefaultConfig();
            saveConfig();
        }
    }

    /** Guarda el config.yml */
    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(configData, writer);
        } catch (IOException e) {
            logger.error("Error guardando config.yml", e);
        }
    }

    /** Actualiza automáticamente los IDs de los servidores según Velocity */
    @SuppressWarnings("unchecked")
    private void updateServerChannels() {
        Object obj = configData.get("channels");
        if (!(obj instanceof Map))
            return;

        Map<String, Object> channels = (Map<String, Object>) obj;

        for (var srv : server.getAllServers()) {
            String name = srv.getServerInfo().getName();
            if (!channels.containsKey(name)) {
                channels.put(name, 0L); // Inicializamos con 0
            }
        }

        saveConfig();
    }

    // --- GETTERS UTILES ---

    public long getChannel(String key) {
        Object val = ((Map<?, ?>) configData.get("channels")).get(key);
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    /**
     * Obtiene un valor string de cualquier sección del config
     * 
     * @param path Ruta en formato "seccion.clave" o simplemente "clave" para la
     *             raíz
     * @return El valor como string, o cadena vacía si no existe
     */
    public String getString(String path) {
        return getString(path, "");
    }

    /**
     * Obtiene un valor string con valor por defecto
     * 
     * @param path         Ruta en formato "seccion.clave" o simplemente "clave"
     * @param defaultValue Valor por defecto si no existe
     * @return El valor como string
     */
    public String getString(String path, String defaultValue) {
        Object value = getNestedValue(path);
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Obtiene un valor booleano de cualquier sección del config
     * 
     * @param path Ruta en formato "seccion.clave" o simplemente "clave"
     * @return El valor como boolean, o false si no existe
     */
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    /**
     * Obtiene un valor booleano con valor por defecto
     * 
     * @param path         Ruta en formato "seccion.clave" o simplemente "clave"
     * @param defaultValue Valor por defecto si no existe
     * @return El valor como boolean
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = getNestedValue(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * Verifica si el modo debug está activado
     */
    public boolean isDebug() {
        return getBoolean("settings.debug", false);
    }

    /**
     * Método auxiliar para obtener valores anidados
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = configData;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) next;
        }

        return current.get(parts[parts.length - 1]);
    }

    public String getDiscordToken() {
        return getString("discord.botToken", "");
    }

    public String getGameStatus() {
        return getString("discord.status", "playing Velocity Server");
    }

    public void setGameStatus(String status) {
        ((Map<String, Object>) configData.get("discord")).put("status", status);
        saveConfig();
    }

    public boolean getUseStatus() {
        return getBoolean("discord.useConfigStatus", true);
    }

    public boolean isVoiceStatusEnabled() {
        return getBoolean("voiceStatus.enabled", false);
    }

    public long getVoiceStatusChannelId() {
        Object val = ((Map<?, ?>) configData.get("voiceStatus")).get("channelId");
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    public String getVoiceStatusFormat() {
        return getString("voiceStatus.format", "🎮 Jugadores: %count%");
    }

    public int getVoiceStatusInterval() {
        Object val = ((Map<?, ?>) configData.get("voiceStatus")).get("updateIntervalSeconds");
        return val instanceof Number ? ((Number) val).intValue() : 30;
    }

    public String getFormat(String type) {
        Map<String, Object> format = (Map<String, Object>) configData.get("format");
        Map<String, Object> discordFormat = (Map<String, Object>) format.get("discord");
        Object val = switch (type) {
            case "discord_from" -> discordFormat.get("from");
            case "discord_to" -> discordFormat.get("to");
            default -> "";
        };
        return val instanceof String ? (String) val : "";
    }
}