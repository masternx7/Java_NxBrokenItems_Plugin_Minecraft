package dev.fluffyworld.nxbrokenitems.commands;

import dev.fluffyworld.nxbrokenitems.NxBrokenItems;
import dev.fluffyworld.nxbrokenitems.gui.BrokenItemsGUI;
import dev.fluffyworld.nxbrokenitems.gui.DeleteItemsGUI;
import dev.fluffyworld.nxbrokenitems.utils.MessageUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class NxBrokenItemsCommand implements CommandExecutor, TabCompleter {

    private final NxBrokenItems plugin;
    private final BrokenItemsGUI brokenItemsGUI;
    private final DeleteItemsGUI deleteItemsGUI;
    private Economy economy;

    public NxBrokenItemsCommand(NxBrokenItems plugin) {
        this.plugin = plugin;
        if (setupEconomy()) {
            this.deleteItemsGUI = new DeleteItemsGUI(plugin);
            this.brokenItemsGUI = new BrokenItemsGUI(plugin, economy);
        } else {
            plugin.getLogger().warning("Vault not found! Economy functions will be disabled.");
            this.brokenItemsGUI = null;
            this.deleteItemsGUI = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.usage")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "restore":
                if (!player.hasPermission("nxbrokenitems.restore")) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (brokenItemsGUI == null) {
                    player.sendMessage("Vault is not enabled, this command is disabled.");
                    return true;
                }
                brokenItemsGUI.openInventory(player);
                return true;
            case "delete":
                if (!player.hasPermission("nxbrokenitems.delete")) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                if (deleteItemsGUI == null) {
                    player.sendMessage("Vault is not enabled, this command is disabled.");
                    return true;
                }
                deleteItemsGUI.openInventory(player);
                return true;
            case "reload":
                if (!player.hasPermission("nxbrokenitems.reload")) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.no-permission")));
                    return true;
                }
                reloadPlugin(player);
                return true;
            default:
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.usage")));
                return true;
        }
    }

    private void reloadPlugin(Player player) {
        UUID playerUUID = player.getUniqueId();
        plugin.reloadDataFile(playerUUID);
        plugin.reloadConfig();
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.reload-success")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("restore", "delete", "reload");
        }
        return Collections.emptyList();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault != null && vault.isEnabled()) {
            economy = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
        }

        return economy != null;
    }
}
