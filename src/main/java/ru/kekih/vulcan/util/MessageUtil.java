package ru.kekih.vulcan.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MessageUtil {

    public static void broadcast(String message) {
        Bukkit.getServer().broadcastMessage(message);
    }

    public static void send(Player player, String message) {
        player.sendMessage(message);
    }
}
