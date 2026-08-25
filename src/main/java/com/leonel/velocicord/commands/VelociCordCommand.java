package com.leonel.velocicord.commands;

import com.leonel.velocicord.VelociCord;
import com.leonel.velocicord.utils.TextFormatter;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.dv8tion.jda.api.entities.Activity;

import java.util.ArrayList;
import java.util.List;

public class VelociCordCommand implements SimpleCommand {

    private final VelociCord bridge;

    public VelociCordCommand(VelociCord bridge) {
        this.bridge = bridge;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender, args);
            default -> sender.sendMessage(TextFormatter.toMinecraft(
                    "&cSubcomando desconocido. Usa &e/velocicord help &cpara ver la lista."
            ));
        }
    }

    private void sendHelp(CommandSource sender) {
        sender.sendMessage(TextFormatter.toMinecraft("&bVelociCord - Comandos disponibles:"));
        sender.sendMessage(TextFormatter.toMinecraft("&7/velocicord reload &8» &fRecarga la configuración y reinicia el bot."));
        sender.sendMessage(TextFormatter.toMinecraft(
                "&7/velocicord status <playing|watching|listening|custom> <mensaje> &8» &fCambia el estado del bot."
        ));
    }

    private void handleReload(CommandSource sender) {
        if (!hasPermission(sender, "velocicord.command.reload")) {
            sender.sendMessage(TextFormatter.toMinecraft("&cNo tienes permiso para usar este comando."));
            return;
        }

        try {
            bridge.doReload();
            sender.sendMessage(TextFormatter.toMinecraft("&aVelociCord recargado correctamente!"));
        } catch (Exception e) {
            bridge.getLogger().error("Error al recargar VelociCord.", e);
            sender.sendMessage(TextFormatter.toMinecraft("&cOcurrió un error al recargar VelociCord."));
        }
    }

    private void handleStatus(CommandSource sender, String[] args) {
        if (!hasPermission(sender, "velocicord.command.status")) {
            sender.sendMessage(TextFormatter.toMinecraft("&cNo tienes permiso para usar este comando."));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(TextFormatter.toMinecraft("&cUso: /velocicord status <playing|watching|listening|custom> <mensaje>"));
            return;
        }

        String mode = args[1].toLowerCase();

        // Concatenar argumentos desde el índice 2
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String statusText = sb.toString().trim();

        try {
            switch (mode) {
                case "playing" -> bridge.getJDA().getPresence().setActivity(Activity.playing(statusText));
                case "watching" -> bridge.getJDA().getPresence().setActivity(Activity.watching(statusText));
                case "listening" -> bridge.getJDA().getPresence().setActivity(Activity.listening(statusText));
                case "custom" -> bridge.getJDA().getPresence().setActivity(Activity.customStatus(statusText));
                default -> {
                    sender.sendMessage(TextFormatter.toMinecraft("&cModo inválido. Usa: &7playing, watching, listening o custom."));
                    return;
                }
            }

            bridge.getConfig().setGameStatus(mode + " " + statusText);
            sender.sendMessage(TextFormatter.toMinecraft("&aEstado del bot actualizado a: &f" + mode + " " + statusText));
        } catch (Exception e) {
            bridge.getLogger().error("Error al actualizar el estado del bot.", e);
            sender.sendMessage(TextFormatter.toMinecraft("&cOcurrió un error al actualizar el estado del bot."));
        }
    }

    private boolean hasPermission(CommandSource sender, String perm) {
        if (sender instanceof Player player) {
            return player.hasPermission(perm);
        }
        return true; // consola siempre tiene permiso
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = List.of("reload", "status");
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase())) suggestions.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("status")) {
            List<String> modes = List.of("playing", "watching", "listening", "custom");
            for (String m : modes) {
                if (m.startsWith(args[1].toLowerCase())) suggestions.add(m);
            }
        }

        return suggestions;
    }
}