package dev.fluffyworld.nxbrokenitems.utils;

import org.bukkit.ChatColor;

public class MessageUtils {
    public static String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
