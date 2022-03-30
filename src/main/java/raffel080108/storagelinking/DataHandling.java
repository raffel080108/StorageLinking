package raffel080108.storagelinking;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getScheduler;
import static raffel080108.storagelinking.Main.plugin;

public class DataHandling {
    static HashMap<Location, Location> storedLocations = new HashMap<>();
    static ArrayList<Material> containers = new ArrayList<>();

    static Material linkingItem;
    static boolean particlesEnabled = true;
    static long transferDelay = 8L;
    static boolean displayNoPermission = true;

    public static void loadConfig() {
        plugin().reloadConfig();
        FileConfiguration config = plugin().getConfig();

        //Cancel transferring-task (For config reloads)
        getScheduler().cancelTasks(plugin());

        //Load containers
        containers.clear();
        List<String> configContainers = config.getStringList("containers");
        if (configContainers.size() > 0) {
            for (String container : configContainers) {
                Material material = Material.matchMaterial(container);
                if (material != null) {
                    containers.add(material);
                } else {
                    getLogger().severe("Error while loading config-string \"containers\" - Please check your config.yml file or delete it, to regenerate it. Default values will be used for now");
                    containers.add(Material.CHEST);
                    containers.add(Material.BARREL);
                    containers.add(Material.TRAPPED_CHEST);
                    return;
                }
            }
            getLogger().info("Containers loaded successfully");
        } else {
            containers.add(Material.CHEST);
            containers.add(Material.BARREL);
            containers.add(Material.CHEST);
            List<String> defaultContainers = List.of("CHEST", "BARREL", "TRAPPED_CHEST");
            config.set("containers", defaultContainers);
            plugin().saveConfig();
            getLogger().warning("Config-string \"containers\" was empty or didn't exist and has been set to it's default values");
        }

        //Load linking-item
        String linkingItemData = config.getString("linkingItem");
        if (linkingItemData != null) {
            Material item = Material.matchMaterial(linkingItemData);
            if (item != null) {
                linkingItem = item;
                getLogger().info("Linking-item loaded successfully");
            } else {
                getLogger().severe("Error while loading config-string \"linkingItem\" - Please check your config.yml file or delete it, to regenerate it. Default value will be used for now");
                linkingItem = Material.STICK;
            }
        } else {
            config.set("linkingItem", "Stick");
            getLogger().warning("Config-string \"linkingItem\" was empty or didn't exist and has been set to it's default value");
        }

        //Load particles boolean
        if (config.contains("enable-particles")) {
            particlesEnabled = config.getBoolean("enable-particles");
        } else {
            config.set("enable-particles", true);
            getLogger().warning("Config-string \"enable-particles\" was empty or didn't exist and has been set to it's default value");
        }

        //Load transfer-delay
        if (config.contains("transfer-delay")) {
            if (config.getInt("transfer-delay") > 0) {
                transferDelay = config.getInt("transfer-delay");
            } else {
                config.set("transfer-delay", 8);
                plugin().saveConfig();
                getLogger().severe("Config-string \"enable-particles\" had an invalid value and has been set to it's default value");
            }
        } else {
            config.set("transfer-delay", 8);
            plugin().saveConfig();
            getLogger().warning("Config-string \"enable-particles\" was empty or didn't exist and has been set to it's default value");
        }

        //Check if optional usage permission is active
        if (config.contains("enable-usage-permission")) {
            if (config.contains("no-usage-permission-message")) {
                displayNoPermission = config.getBoolean("no-usage-permission-message");
            } else {
                config.set("no-usage-permission-message", true);
                getLogger().warning("Config-string \"no-usage-permission-message\" was empty or didn't exist and has been set to it's default value");
            }
        } else {
            config.set("enable-usage-permission", false);
            getLogger().warning("Config-string \"enable-usage-permission\" was empty or didn't exist and has been set to it's default value");
        }

        new ItemTransferring().runTaskTimer(plugin(), 0L, transferDelay);
    }
}