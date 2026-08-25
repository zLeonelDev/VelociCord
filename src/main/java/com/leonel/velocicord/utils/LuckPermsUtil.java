package com.leonel.velocicord.utils;

import com.velocitypowered.api.proxy.Player;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import java.util.Set;
import java.util.stream.Collectors;

public class LuckPermsUtil {

    private static LuckPerms luckPerms;

    public static void init() {
        try {
            luckPerms = LuckPermsProvider.get();
            System.out.println("LuckPerms inicializado correctamente.");
        } catch (IllegalStateException e) {
            luckPerms = null;
            System.out.println("LuckPerms no disponible. Se desactivan funciones de prefijo y permisos.");
        }
    }

    public static boolean isAvailable() {
        return luckPerms != null;
    }

    public static String getPrefix(Player player) {
        if (luckPerms == null) return "";

        try {
            var adapter = luckPerms.getPlayerAdapter(Player.class);
            CachedMetaData meta = adapter.getMetaData(player);
            return meta.getPrefix() != null ? meta.getPrefix() : "";
        } catch (Exception e) {
            System.out.println("Error al obtener prefijo de LuckPerms para " + player.getUsername());
            return "";
        }
    }

    public static boolean hasPermission(Player player, String permission) {
        if (luckPerms == null) return false;

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return false;

            QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(player);
            if (queryOptions == null) return false;
            return user.getCachedData().getPermissionData(queryOptions).checkPermission(permission).asBoolean();
        } catch (Exception e) {
            System.out.println("Error al verificar permiso '" + permission + "' para " + player.getUsername());
            return false;
        }
    }

    public static String getPrimaryGroup(Player player) {
        if (luckPerms == null) return "";

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user != null ? user.getPrimaryGroup() : "";
        } catch (Exception e) {
            System.out.println("Error al obtener grupo primario de " + player.getUsername());
            return "";
        }
    }

    public static Set<String> getGroups(Player player) {
        if (luckPerms == null) return Set.of();

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) return Set.of();

            return user.getNodes().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .map(node -> ((InheritanceNode) node).getGroupName())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            System.out.println("Error al obtener grupos de " + player.getUsername());
            return Set.of();
        }
    }
}