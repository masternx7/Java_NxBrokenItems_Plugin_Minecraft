package dev.fluffyworld.nxbrokenitems;

import dev.fluffyworld.nxbrokenitems.commands.NxBrokenItemsCommand;
import dev.fluffyworld.nxbrokenitems.gui.BrokenItemsGUI;
import dev.fluffyworld.nxbrokenitems.gui.DeleteItemsGUI;
import dev.fluffyworld.nxbrokenitems.listeners.ItemBreakListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public final class NxBrokenItems extends JavaPlugin {

    private Economy economy;
    private DeleteItemsGUI deleteItemsGUI;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ItemBreakListener(this), this);

        NxBrokenItemsCommand commandExecutor = new NxBrokenItemsCommand(this);
        getCommand("nxbrokenitems").setExecutor(commandExecutor);
        getCommand("nxbrokenitems").setTabCompleter(commandExecutor);

        saveDefaultConfig();

        createLogRecoveryFile();
        createLogDeleteFile();
    }

    @Override
    public void onDisable() {
    }



    public FileConfiguration getDataConfig(UUID playerUUID) {
        File playerDataFile = new File(getDataFolder(), "dataUser" + File.separator + playerUUID.toString() + ".yml");
        if (!playerDataFile.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void saveDataFile(UUID playerUUID, FileConfiguration dataConfig) {
        File playerDataFile = new File(getDataFolder(), "dataUser" + File.separator + playerUUID.toString() + ".yml");
        try {
            dataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration createDataFile(UUID playerUUID) {
        File playerDataFile = new File(getDataFolder(), "dataUser" + File.separator + playerUUID.toString() + ".yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.getParentFile().mkdirs();
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void reloadDataFile(UUID playerUUID) {
        File playerDataFile = new File(getDataFolder(), "dataUser" + File.separator + playerUUID.toString() + ".yml");
        if (playerDataFile.exists()) {
            FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
            try {
                dataConfig.load(playerDataFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            createDataFile(playerUUID);
        }
    }

    private void createLogRecoveryFile() {
        File logFile = new File(getDataFolder(), "log-recovery.yml");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                FileConfiguration logConfig = YamlConfiguration.loadConfiguration(logFile);
                logConfig.set("logs", new ArrayList<String>());
                logConfig.save(logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void createLogDeleteFile() {
        File logFile = new File(getDataFolder(), "log-item-delete.yml");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                FileConfiguration logConfig = YamlConfiguration.loadConfiguration(logFile);
                logConfig.set("logs", new ArrayList<String>());
                logConfig.save(logFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
