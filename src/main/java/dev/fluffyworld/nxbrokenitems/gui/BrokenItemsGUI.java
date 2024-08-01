package dev.fluffyworld.nxbrokenitems.gui;

import dev.fluffyworld.nxbrokenitems.NxBrokenItems;
import dev.fluffyworld.nxbrokenitems.utils.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
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

public class BrokenItemsGUI implements Listener {

    private final NxBrokenItems plugin;
    private final Economy economy;
    private final Map<UUID, Integer> playerPageMap = new HashMap<>();

    public BrokenItemsGUI(NxBrokenItems plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
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

        String title = MessageUtils.colorize(plugin.getConfig().getString("menu.restore.title", "&cBroken Items List"));
        int size = plugin.getConfig().getInt("menu.restore.size", 54);
        int itemsPerPage = size - 9; // Reserving last row for navigation

        Inventory inventory = Bukkit.createInventory(null, size, title);

        List<ItemStack> items = new ArrayList<>();
        for (String key : dataConfig.getConfigurationSection("restoreItem").getKeys(false)) {
            ItemStack item = dataConfig.getItemStack("restoreItem." + key);
            if (item != null) {
                addRestorationCostLore(item);
                items.add(item);
            }
        }

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            inventory.addItem(items.get(i));
        }

        if (page > 0) {
            ItemStack prevPage = createNavigationItem("previous-page");
            inventory.setItem(size - 9, prevPage);
        }

        if (endIndex < items.size()) {
            ItemStack nextPage = createNavigationItem("next-page");
            inventory.setItem(size - 1, nextPage);
        }

        player.openInventory(inventory);
        playerPageMap.put(playerUUID, page);
    }

    public void openInventory(Player player) {
        openInventory(player, 0);
    }

    private ItemStack createNavigationItem(String type) {
        FileConfiguration config = plugin.getConfig();
        Material material = Material.valueOf(config.getString("menu.restore.navigation-buttons." + type + ".material", "ARROW").toUpperCase());
        int customModelData = config.getInt("menu.restore.navigation-buttons." + type + ".custom-model-data", 0);
        String displayName = MessageUtils.colorize(config.getString("menu.restore.navigation-buttons." + type + ".display-name", ""));

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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String titlePrefix = MessageUtils.colorize(plugin.getConfig().getString("menu.restore.title", "&cBroken Items List"));
        if (event.getView().getTitle().startsWith(titlePrefix)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            UUID playerUUID = player.getUniqueId();
            ItemStack item = event.getCurrentItem();

            FileConfiguration config = plugin.getConfig();
            String nextPageDisplayName = MessageUtils.colorize(config.getString("menu.restore.navigation-buttons.next-page.display-name", "&aNext Page"));
            String prevPageDisplayName = MessageUtils.colorize(config.getString("menu.restore.navigation-buttons.previous-page.display-name", "&aPrevious Page"));

            if (item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null) {
                String itemName = item.getItemMeta().getDisplayName();
                int currentPage = playerPageMap.getOrDefault(playerUUID, 0);

                if (itemName.equals(nextPageDisplayName)) {
                    openInventory(player, currentPage + 1);
                } else if (itemName.equals(prevPageDisplayName)) {
                    openInventory(player, currentPage - 1);
                } else {
                    handleItemRestoration(player, item);
                }
                return;
            }

            handleItemRestoration(player, item);
        }
    }

    private int calculateRestorationCost(ItemStack item) {
        List<Integer> costs = plugin.getConfig().getIntegerList("costs");
        int defaultCostWithoutUnbreaking = plugin.getConfig().getInt("default-cost-without-unbreaking", 30000);

        if (item.getItemMeta() != null && item.getItemMeta().hasEnchant(Enchantment.DURABILITY)) {
            int unbreakingLevel = item.getEnchantmentLevel(Enchantment.DURABILITY);
            if (unbreakingLevel > 0 && unbreakingLevel <= costs.size()) {
                return costs.get(unbreakingLevel - 1);
            }
        }
        return defaultCostWithoutUnbreaking;
    }

    private void addRestorationCostLore(ItemStack item) {
        int cost = calculateRestorationCost(item);
        String loreFormat = plugin.getConfig().getString("menu.restore.lore.format", "&eRestoration Cost: &6{cost}");
        String formattedLore = MessageUtils.colorize(loreFormat.replace("{cost}", String.valueOf(cost)));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(formattedLore);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private void handleItemRestoration(Player player, ItemStack item) {
        UUID playerUUID = player.getUniqueId();
        FileConfiguration dataConfig = plugin.getDataConfig(playerUUID);
        int cost = calculateRestorationCost(item) * item.getAmount();

        if (PlaceholderAPI.setPlaceholders(player, "%fluffy_isfull%").equalsIgnoreCase("true")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.inventory-full")));
            return;
        }

        if (economy == null || !economy.has(player, cost)) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.not-enough-money").replace("{cost}", String.valueOf(cost))));
            return;
        }

        for (String key : dataConfig.getConfigurationSection("restoreItem").getKeys(false)) {
            ItemStack storedItem = dataConfig.getItemStack("restoreItem." + key);
            if (storedItem != null && isSameItem(storedItem, item)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        lore = lore.stream()
                                .filter(line -> !line.contains("ราคาที่ต้องจ่าย:"))
                                .collect(Collectors.toList());
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }

                player.getInventory().addItem(item);
                economy.withdrawPlayer(player, cost);
                dataConfig.set("restoreItem." + key, null);
                plugin.saveDataFile(playerUUID, dataConfig);
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.restore-success")));

                logRecovery(player.getName(), item);

                player.closeInventory();
                return;
            }
        }
    }

    private boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        if (meta1 == null || meta2 == null) {
            return item1.getType() == item2.getType();
        }

        meta1.setLore(null);
        meta2.setLore(null);

        ItemStack clone1 = item1.clone();
        ItemStack clone2 = item2.clone();
        clone1.setItemMeta(meta1);
        clone2.setItemMeta(meta2);

        return clone1.isSimilar(clone2);
    }



    private void logRecovery(String playerName, ItemStack item) {
        File logFile = new File(plugin.getDataFolder(), "log-recovery.yml");
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
        String logEntry = playerName + " restored " + item.getAmount() + "x " + displayName + lore + enchantmentsStr + " at " + currentTime;

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
