package com.leonel.velocicord.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class TextFormatter {

    public static Component toMinecraft(String raw) {
        if (raw.contains("<") && raw.contains(">")) {
            return MiniMessage.miniMessage().deserialize(raw);
        } else {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        }
    }

    public static String toDiscord(String raw) {
        String stripped = raw.replaceAll("(?i)&[0-9A-FK-OR]", "");
        stripped = stripped.replaceAll("<[^>]+>", "");
        return stripped;
    }
}