package dev.fluffyworld.nxbrokenitems.gui;

import dev.fluffyworld.nxbrokenitems.NxBrokenItems;
import dev.fluffyworld.nxbrokenitems.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DeleteItemsGUI implements Listener {

    private final NxBrokenItems plugin;
    private final Map<UUID, Integer> playerPageMap = new HashMap<>();
    private final Map<UUID, ItemStack> confirmDeleteMap = new HashMap<>();

    public DeleteItemsGUI(NxBrokenItems plugin) {
        this.plugin = plugin;
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(this, plugin);
    }

    public void openInventory(Player player, int page) {
        UUID playerUUID = player.getUniqueId();
        FileConfiguration dataConfig = plugin.getDataConfig(playerUUID);

        if (dataConfig == null || !dataConfig.contains("restoreItem")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.no-broken-items")));
            return;
        }

        String title = MessageUtils.colorize(plugin.getConfig().getString("menu.delete.title", "&cDelete Broken Items"));
        int size = plugin.getConfig().getInt("menu.delete.size", 54);
        int itemsPerPage = size - 9; // Reserving last row for navigation

        Inventory inventory = Bukkit.createInventory(null, size, title);

        List<ItemStack> items = new ArrayList<>();
        for (String key : dataConfig.getConfigurationSection("restoreItem").getKeys(false)) {
            ItemStack item = dataConfig.getItemStack("restoreItem." + key);
            if (item != null) {
                items.add(item);
            }
        }

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            inventory.addItem(items.get(i));
        }

        if (page > 0) {
            ItemStack prevPage = createNavigationItem("menu.delete.navigation-buttons.previous-page");
            inventory.setItem(size - 9, prevPage);
        }

        if (endIndex < items.size()) {
            ItemStack nextPage = createNavigationItem("menu.delete.navigation-buttons.next-page");
            inventory.setItem(size - 1, nextPage);
        }

        player.openInventory(inventory);
        playerPageMap.put(playerUUID, page);
    }

    public void openInventory(Player player) {
        openInventory(player, 0);
    }

    private ItemStack createNavigationItem(String configPath) {
        FileConfiguration config = plugin.getConfig();
        Material material = Material.valueOf(config.getString(configPath + ".material", "ARROW").toUpperCase());
        int customModelData = config.getInt(configPath + ".custom-model-data", 0);
        String displayName = MessageUtils.colorize(config.getString(configPath + ".display-name", ""));

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (customModelData != 0) {
                meta.setCustomModelData(customModelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openConfirmationInventory(Player player, ItemStack itemToDelete) {
        String title = MessageUtils.colorize(plugin.getConfig().getString("menu.confirm.title", "&cConfirm Deletion"));
        int size = plugin.getConfig().getInt("menu.confirm.size", 27);

        Inventory confirmationInventory = Bukkit.createInventory(null, size, title);

        ItemStack confirmItem = createNavigationItem("menu.confirm.buttons.confirm");
        ItemStack cancelItem = createNavigationItem("menu.confirm.buttons.cancel");

        confirmationInventory.setItem(11, confirmItem);
        confirmationInventory.setItem(15, cancelItem);

        player.openInventory(confirmationInventory);
        confirmDeleteMap.put(player.getUniqueId(), itemToDelete);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String deleteTitlePrefix = MessageUtils.colorize(plugin.getConfig().getString("menu.delete.title", "&cDelete Broken Items"));
        String confirmTitlePrefix = MessageUtils.colorize(plugin.getConfig().getString("menu.confirm.title", "&cConfirm Deletion"));

        if (event.getView().getTitle().startsWith(deleteTitlePrefix)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            UUID playerUUID = player.getUniqueId();
            ItemStack item = event.getCurrentItem();

            FileConfiguration config = plugin.getConfig();
            String nextPageDisplayName = MessageUtils.colorize(config.getString("menu.delete.navigation-buttons.next-page.display-name", "&aNext Page"));
            String prevPageDisplayName = MessageUtils.colorize(config.getString("menu.delete.navigation-buttons.previous-page.display-name", "&aPrevious Page"));

            if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null) {
                String itemName = item.getItemMeta().getDisplayName();
                int currentPage = playerPageMap.getOrDefault(playerUUID, 0);

                if (itemName.equals(nextPageDisplayName)) {
                    openInventory(player, currentPage + 1);
                } else if (itemName.equals(prevPageDisplayName)) {
                    openInventory(player, currentPage - 1);
                } else {
                    openConfirmationInventory(player, item);
                }
                return;
            }

            openConfirmationInventory(player, item);

        } else if (event.getView().getTitle().startsWith(confirmTitlePrefix)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            UUID playerUUID = player.getUniqueId();
            ItemStack item = event.getCurrentItem();

            FileConfiguration config = plugin.getConfig();
            String confirmDisplayName = MessageUtils.colorize(config.getString("menu.confirm.buttons.confirm.display-name", "&aConfirm"));
            String cancelDisplayName = MessageUtils.colorize(config.getString("menu.confirm.buttons.cancel.display-name", "&cCancel"));

            if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null) {
                String itemName = item.getItemMeta().getDisplayName();

                if (itemName.equals(confirmDisplayName)) {
                    handleItemDeletion(player, confirmDeleteMap.get(playerUUID));
                    confirmDeleteMap.remove(playerUUID);
                    openInventory(player, playerPageMap.getOrDefault(playerUUID, 0));
                } else if (itemName.equals(cancelDisplayName)) {
                    confirmDeleteMap.remove(playerUUID);
                    openInventory(player, playerPageMap.getOrDefault(playerUUID, 0));
                }
                return;
            }
        }
    }

    private void handleItemDeletion(Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();
        FileConfiguration dataConfig = plugin.getDataConfig(playerUUID);

        for (String key : dataConfig.getConfigurationSection("restoreItem").getKeys(false)) {
            ItemStack storedItem = dataConfig.getItemStack("restoreItem." + key);
            if (storedItem != null && storedItem.isSimilar(item)) {
                dataConfig.set("restoreItem." + key, null);
                plugin.saveDataFile(playerUUID, dataConfig);
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.delete-success")));

                logDeletion(player.getName(), item);

                return;
            }
        }
    }

    private void logDeletion(String playerName, ItemStack item) {
        File logFile = new File(plugin.getDataFolder(), "log-item-delete.yml");
        FileConfiguration logConfig = YamlConfiguration.loadConfiguration(logFile);

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        String displayName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        List<String> loreList = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : List.of();

        Map<Enchantment, Integer> enchantments = item.getItemMeta().getEnchants();
        String enchantmentsStr = enchantments.isEmpty() ? "" : " with enchantments: " + enchantments.entrySet().stream()
                .map(e -> e.getKey().getKey().getKey() + " " + e.getValue())
                .collect(Collectors.joining(", "));

        String lore = loreList.isEmpty() ? "" : " with lore: " + String.join(", ", loreList);
        String logEntry = playerName + " deleted " + item.getAmount() + "x " + displayName + lore + enchantmentsStr + " at " + currentTime;

        if (!logConfig.contains("logs")) {
            logConfig.set("logs", new ArrayList<>());
        }

        List<String> dailyLogs = logConfig.getStringList(currentDate);
        dailyLogs.add(logEntry);
        logConfig.set(currentDate, dailyLogs);

        try {
            logConfig.save(logFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
