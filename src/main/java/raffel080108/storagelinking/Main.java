package raffel080108.storagelinking;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static raffel080108.storagelinking.DataHandling.*;

@SuppressWarnings("unused")
public final class Main extends JavaPlugin {
    //For using plugin class
    private static Main plugin;
    public static Main plugin() {
        return plugin;
    }

    //Data file
    File dataFile;
    FileConfiguration data;

    //Normal Plugin stuff
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        plugin = this;
        if (!new File(getDataFolder(), "data.yml").exists())
            saveResource("data.yml", false);

        dataFile = new File(plugin().getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);

        saveDefaultConfig();
        loadConfig();

        Bukkit.getPluginManager().registerEvents(new Linking(), this);
        this.getCommand("storageLinking").setExecutor(new Commands());

        //Load stored locations
        List<String> locationsList = plugin().data.getStringList("stored-locations");
        if (!locationsList.isEmpty()) {
            for (String key : locationsList) {
                String[] locations = key.split(" \\| ");

                String[] splitKey = locations[0].split(", ");
                World world1 = Bukkit.getServer().getWorld(splitKey[0]);

                String[] splitValue = locations[1].split(", ");
                World world2 = Bukkit.getServer().getWorld(splitValue[0]);

                Location location1;
                Location location2;

                if (world1 != null && world2 != null) {
                    try {
                        location1 = new Location(world1, Double.parseDouble(splitKey[1]), Double.parseDouble(splitKey[2]), Double.parseDouble(splitKey[3]));
                        location2 = new Location(world2, Double.parseDouble(splitValue[1]), Double.parseDouble(splitValue[2]), Double.parseDouble(splitValue[3]));
                    } catch (NumberFormatException e) {
                        getLogger().severe("Stored Locations could not be loaded, please make sure you did not change anything inside of data.yml");
                        getLogger().severe("Any previously stored links will not be active but all other plugin functionality should be normal");
                        break;
                    }
                } else {
                    getLogger().severe("Stored Locations could not be loaded, please make sure you did not change anything inside of data.yml");
                    getLogger().severe("Any previously stored links will not be active but all other plugin functionality should be normal");
                    break;
                }

                if (!(location1.getBlock().getState() instanceof Container) || !(location2.getBlock().getState() instanceof Container)) {
                    getLogger().warning("The block at a loaded location was not of type container. The link involving the location will not be active");
                    continue;
                }

                if (!containers.contains(location1.getBlock().getType()) || !containers.contains(location2.getBlock().getType()))
                    getLogger().warning("A location was loaded, that is not of any type of container listed in the configuration. The link involving this container will not be active unless the configuration is changed");

                storedLocations.put(location1, location2);
            }
        } else {
            getLogger().info("No previously saved locations found");
        }

        getLogger().info("The StorageLinking plugin has started!");
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
                getLogger().info("Stored locations saved to data.yml");
            } catch (IOException e) {
                e.printStackTrace();
                getLogger().severe("Could not save stored locations - All links that were not previously saved will be lost!");
            }
        } else {
            data.set("stored-locations", "");
            try {
                data.save(dataFile);
            } catch (IOException ignored) {}

            getLogger().info("No stored locations found to save");
        }
        getLogger().info("The StorageLinking plugin has stopped!");
    }
}


