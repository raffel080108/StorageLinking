/*
 StorageLinking © 2022 by Raphael "raffel080108" Roehrig is licensed under CC BY-NC-SA 4.0. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/4.0/
 */

package raffel080108.storagelinking;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class StorageLinking extends JavaPlugin {
    //Data file
    private File dataFile;
    private FileConfiguration data;

    //Plugin data handling
    public HashMap<Location, Location> storedLocations = new HashMap<>();
    public ArrayList<Material> containers = new ArrayList<>();

    //Normal Plugin stuff
    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "data.yml").exists())
            saveResource("data.yml", false);

        dataFile = new File(this.getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);

        saveDefaultConfig();
        this.reloadConfig();

        //Check for invalid config
        try {
            new YamlConfiguration().load(new File(this.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            try {
                Files.createFile(Path.of(this.getDataFolder().getPath(), "old_config.yml"));
                Files.copy(Paths.get(new File(this.getDataFolder(), "config.yml").getPath()), Paths.get(new File(this.getDataFolder(), "old_config.yml").getPath()), REPLACE_EXISTING);
                this.saveResource("config.yml", true);
            } catch (IOException e2) {
                e2.printStackTrace();
                this.getLogger().severe("Could not save old config");
            }

            this.getLogger().warning("Invalid config detected - Current config was backed up to old_config.yml and a new config.yml generated");
        }

        //Load containers
        containers.clear();
        List<String> configContainers = this.getConfig().getStringList("containers");
        if (configContainers.size() > 0) {
            for (String container : configContainers) {
                Material material = Material.matchMaterial(container);
                if (material != null) {
                    containers.add(material);
                } else {
                    this.getLogger().warning("Could not load config-string \"containers\" - Please check your config.yml file. Default values will be used for now");
                    containers.add(Material.CHEST);
                    containers.add(Material.BARREL);
                    containers.add(Material.TRAPPED_CHEST);
                }
            }

            this.getLogger().info("Containers loaded successfully");
        } else {
            containers.add(Material.CHEST);
            containers.add(Material.BARREL);
            containers.add(Material.CHEST);
            List<String> defaultContainers = List.of("CHEST", "BARREL", "TRAPPED_CHEST");
            this.getConfig().set("containers", defaultContainers);
            this.saveConfig();

            this.getLogger().warning("Config-string \"containers\" is empty - Please check your config.yml file. Default values will be used for now");
        }

        //Load stored locations
        List<String> locationsList = this.data.getStringList("stored-locations");
        if (!locationsList.isEmpty()) {
            for (String key : locationsList) {
                String[] locations = key.split(" \\| ");

                String[] split1 = locations[0].split(", ");
                World world1 = Bukkit.getServer().getWorld(split1[0]);

                String[] split2 = locations[1].split(", ");
                World world2 = Bukkit.getServer().getWorld(split2[0]);

                Location location1;
                Location location2;

                if (world1 != null && world2 != null) {
                    try {
                        location1 = new Location(world1, Double.parseDouble(split1[1]), Double.parseDouble(split1[2]), Double.parseDouble(split1[3]));
                        location2 = new Location(world2, Double.parseDouble(split2[1]), Double.parseDouble(split2[2]), Double.parseDouble(split2[3]));
                    } catch (NumberFormatException e) {
                        this.getLogger().warning("A loaded link had invalid coordinates data associated with it. This link will not be active unless the value is manually changed and the plugin reloaded");
                        continue;
                    }
                } else {
                    this.getLogger().warning("A loaded link had an invalid world associated with it. This link will not be active unless the value is manually changed and the plugin reloaded");
                    continue;
                }

                if (!(location1.getBlock().getState() instanceof Container) || !(location2.getBlock().getState() instanceof Container)) {
                    this.getLogger().warning("The block at a loaded location was not of type container. The link involving the location will not be active");
                    continue;
                }

                if (!containers.contains(location1.getBlock().getType()) || !containers.contains(location2.getBlock().getType())) {
                    this.getLogger().warning("A location was loaded, that is not of any type of container listed in the configuration. The link involving this container will not be active unless the configuration is changed");
                    continue;
                }

                storedLocations.put(location1, location2);
            }

            this.getLogger().info("Previously saved locations loaded successfully");
        } else this.getLogger().info("No previously saved locations found");

        Bukkit.getPluginManager().registerEvents(new Linking(this), this);
        //noinspection ConstantConditions
        this.getCommand("storageLinking").setExecutor(new Commands(this));

        new ItemTransferring(this).runTaskTimer(this, 0L, this.getConfig().getLong("transfer-delay"));

        this.getLogger().info("The StorageLinking plugin has started!");
    }

    @Override
    public void onDisable() {
        //Save Locations
        if (!new File(getDataFolder(), "data.yml").exists())
            saveResource("data.yml", false);

        if (!storedLocations.isEmpty()) {
            List<String> locations = new ArrayList<>();
            for (HashMap.Entry<Location, Location> entry : storedLocations.entrySet()) {
                Location location1 = entry.getKey();
                String world1 = location1.getWorld().getName();
                double x1 = location1.getX(), y1 = location1.getY(), z1 = location1.getZ();

                Location location2 = entry.getValue();
                String world2 = location2.getWorld().getName();
                double x2 = location2.getX(), y2 = location2.getY(), z2 = location2.getZ();

                locations.add(world1 + ", " + x1 + ", " +  y1 + ", " + z1 + " | " + world2 + ", " + x2 + ", " +  y2 + ", " + z2 + "");
            }
            data.set("stored-locations", locations);
            try {
                data.save(dataFile);
                this.getLogger().info("Stored locations saved to data.yml");
            } catch (IOException e) {
                e.printStackTrace();
                this.getLogger().severe("Could not save stored locations - All links that were not previously saved will be lost!");
            }
        } else {
            data.set("stored-locations", "");
            try {
                data.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.getLogger().info("No stored locations found to save");
        }

        this.getLogger().info("The StorageLinking plugin has stopped!");
    }
}


