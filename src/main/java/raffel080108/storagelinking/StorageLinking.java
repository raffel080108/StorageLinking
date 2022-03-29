package raffel080108.storagelinking;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.event.EventPriority.*;
import static raffel080108.storagelinking.DataHandling.*;
import static raffel080108.storagelinking.StorageLinking.plugin;

@SuppressWarnings("unused")
public final class StorageLinking extends JavaPlugin {
    //For using plugin class
    private static StorageLinking plugin;
    public static StorageLinking plugin() {
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
        BukkitTask transferringTask = new ItemTransferring().runTaskTimer(plugin(), 0L, transferDelay);

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

//Reload command
@SuppressWarnings("NullableProblems")
class Commands implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0) {
            //Help message (if no arguments were provided)
            if (player != null) {
                player.sendMessage(new TextComponent(
                        ChatColor.BLUE + "" + ChatColor.BOLD + "StorageLinking commands:\n" +
                        ChatColor.YELLOW + "/storageLinking reload: " + ChatColor.GREEN + "Reloads the plugin configuration\n" +
                        ChatColor.YELLOW + "/storageLinking toggleParticles: " + ChatColor.GREEN + "Toggles whether particles should be enabled and displayed or not\n"));
            } else {
                getLogger().info(
                        "StorageLinking commands:\n" +
                        "/storageLinking reload: Reloads the plugin configuration\n" +
                        "/storageLinking toggleParticles: Toggles whether particles should be enabled and displayed or not\n");
            }
            return true;
        } else {
            //Reload command
            if (args[0].equalsIgnoreCase("reload")) {
                if (player != null)
                    if (!player.hasPermission("storageLinking.reload")) {
                        player.sendMessage(new TextComponent(ChatColor.RED + "You do not have permission to execute this command!"));
                        return true;
                    }

                loadConfig();

                if (player != null) {
                    player.sendMessage(new TextComponent(ChatColor.GREEN + "Configuration reloaded!"));
                } else {
                    getLogger().info("Configuration reloaded!");
                }
                return true;
            }

            //Toggle particles command
            if (args[0].equalsIgnoreCase("toggleParticles")) {
                if (player != null)
                    if (!player.hasPermission("storageLinking.toggleParticles")) {
                        player.sendMessage(new TextComponent(ChatColor.RED + "You do not have permission to execute this command!"));
                        return true;
                    }

                if (particlesEnabled) {
                    particlesEnabled = false;
                    plugin().getConfig().set("enable-particles", false);
                    plugin().saveConfig();
                } else {
                    particlesEnabled = true;
                    plugin().getConfig().set("enable-particles", true);
                    plugin().saveConfig();
                }

                if (player != null) {
                    player.sendMessage(new TextComponent(ChatColor.GREEN + "Successfully toggled particles to " + particlesEnabled));
                } else {
                    getLogger().info("Successfully toggled particles to " + particlesEnabled);
                }
                return true;
            }
        }

        if (player != null) {
            player.sendMessage(new TextComponent(ChatColor.RED + "Unknown arguments. Please type /storageLinking for a list of commands"));
        } else {
            getLogger().info("Unknown arguments. Please type /storageLinking for a list of commands");
        }
        return true;
    }
}

class DataHandling {
    static HashMap<Location, Location> storedLocations = new HashMap<>();
    static ArrayList<Material> containers = new ArrayList<>();

    static Material linkingItem;
    static boolean particlesEnabled = true;
    static long transferDelay = 8L;

    public static void loadConfig() {
        plugin().reloadConfig();
        FileConfiguration config = plugin().getConfig();

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
    }
}

class Linking implements Listener {
    HashMap<Player, Location> firstLocation = new HashMap<>();

    @EventHandler(priority=HIGH)
    public void playerLeftClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;
        Player player = event.getPlayer();
        Location blockLocation = event.getClickedBlock().getLocation();

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) && player.getEquipment().getItemInMainHand().getType().equals(linkingItem) && containers.contains(event.getClickedBlock().getType())) {
            //Linking
            if (!player.isSneaking()) {
                if (!(storedLocations.containsKey(blockLocation) || storedLocations.containsValue(blockLocation))) {
                    if (!firstLocation.containsKey(player)) {
                        event.setCancelled(true);
                        firstLocation.put(player, blockLocation);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                        player.sendMessage(new TextComponent(ChatColor.GREEN + "Click another container to link this container with. Use Shift+LeftClick to cancel"));
                    } else {
                        if (!firstLocation.get(player).equals(blockLocation) && antiDoubleChest(blockLocation, firstLocation.get(player))) {
                            event.setCancelled(true);
                            storedLocations.put(firstLocation.get(player), blockLocation);
                            firstLocation.remove(player);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                            int x = blockLocation.getBlockX();
                            int y = blockLocation.getBlockY();
                            int z = blockLocation.getBlockZ();
                            player.sendMessage(new TextComponent(ChatColor.GREEN + "Successfully linked this container to the container at " + ChatColor.RED + "X: " + x + ", Y: " + y + ", Z: " + z));
                            player.sendMessage(new TextComponent(ChatColor.GOLD + "To unlink these containers, use Shift+LeftClick on one of them. Items will transfer from the block you selected first to the block you selected second"));
                        } else {
                            event.setCancelled(true);
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                            player.sendMessage(new TextComponent(ChatColor.DARK_RED + "You cannot link a container to itself"));
                        }
                    }
                } else {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    player.sendMessage(new TextComponent(ChatColor.DARK_RED + "This container is already linked with another container. Use Shift+LeftClick to unlink them"));
                }
            } else {
                event.setCancelled(true);
                if (storedLocations.containsKey(blockLocation) || storedLocations.containsValue(blockLocation)) {
                    for (HashMap.Entry<Location, Location> entry : storedLocations.entrySet()) {
                        if (entry.getKey().equals(blockLocation) || entry.getValue().equals(blockLocation)) {
                            Location location1 = entry.getKey();
                            Location location2 = entry.getValue();
                            int x1 = location1.getBlockX();
                            int y1 = location1.getBlockY();
                            int z1 = location1.getBlockZ();
                            int x2 = location2.getBlockX();
                            int y2 = location2.getBlockY();
                            int z2 = location2.getBlockZ();
                            storedLocations.remove(entry.getKey());

                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                            player.sendMessage(new TextComponent(ChatColor.GREEN + "Successfully unlinked containers at " + ChatColor.RED + "X: " + x1 + ", Y: " + y1 + ", Z: " + z1 + ChatColor.GREEN + " and " + ChatColor.RED + "X: " + x2 + ", Y: " + y2 + ", Z: " + z2));
                            break;
                        }
                    }
                } else {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    player.sendMessage(new TextComponent(ChatColor.DARK_RED + "Container is not linked"));
                }
            }
        }

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) && firstLocation.containsKey(player) && player.isSneaking() && !containers.contains(event.getClickedBlock().getType()) && player.getEquipment().getItemInMainHand().getType().equals(linkingItem)) {
            event.setCancelled(true);
            firstLocation.remove(player);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            player.sendMessage(new TextComponent(ChatColor.RED + "Cancelled linking"));
        }
    }

    public boolean antiDoubleChest(Location location, Location otherLocation) {
        BlockState state = location.getBlock().getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            Inventory inventory = chest.getInventory();
            if (inventory instanceof DoubleChestInventory) {
                World world = location.getWorld();
                double x = location.getX(), y = location.getY(), z = location.getZ();
                Location x1, x2, z1, z2;
                x1 = new Location(world, x + 1, y, z);
                x2 = new Location(world, x - 1, y, z);
                z1 = new Location(world, x, y, z + 1);
                z2 = new Location(world, x, y, z - 1);

                List<Location> blocks = List.of(x1, x2, z1, z2);
                return !blocks.contains(otherLocation);
            }
        }
        return true;
    }

    @EventHandler
    public void blockBreakEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location blockLocation = event.getBlock().getLocation();

        if (containers.contains(blockLocation.getBlock().getType())) {
            if (storedLocations.containsKey(blockLocation) || storedLocations.containsValue(blockLocation)) {
                for (HashMap.Entry<Location, Location> entry : storedLocations.entrySet()) {
                    if (entry.getKey().equals(blockLocation) || entry.getValue().equals(blockLocation)) {
                        storedLocations.remove(entry.getKey());

                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                        player.sendMessage(new TextComponent(ChatColor.RED + "You broke a linked container and removed it's link"));
                        break;
                    }
                }
            }
        }
    }

    HashMap<Location, Player> ignitePlayer = new HashMap<>();
    @EventHandler
    public void blockIgniteEvent(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (player != null)
            ignitePlayer.put(event.getBlock().getLocation(), player);
    }

    @EventHandler
    public void blockExplodeEvent(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            Location location = block.getLocation();
            if (storedLocations.containsKey(location) || storedLocations.containsValue(location)) {
                for (HashMap.Entry<Location, Location> entry : storedLocations.entrySet()) {
                    if (entry.getKey().equals(location) || entry.getValue().equals(location)) {
                        storedLocations.remove(entry.getKey());
                        break;
                    }
                }

                if (ignitePlayer.containsKey(event.getBlock().getLocation())) {
                    Player player = ignitePlayer.get(location);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    player.sendMessage(new TextComponent(ChatColor.RED + "You ignited an explosion that broke one or multiple linked container(s) and have removed their link(s)"));
                    ignitePlayer.remove(location);
                }
            }
        }
    }
}

class ItemTransferring extends BukkitRunnable {
    public ItemTransferring() {}

    @Override
    public void run() {
        if (!storedLocations.isEmpty()) {
            for (HashMap.Entry<Location, Location> entry : storedLocations.entrySet()) {
                Location blockFrom = entry.getKey();
                Location blockTo = entry.getValue();

                Inventory target = ((Container) blockTo.getBlock().getState()).getInventory();
                Inventory source = ((Container) blockFrom.getBlock().getState()).getInventory();

                if (!source.isEmpty()) {
                    ItemStack sourceSlot = null;

                    loops:
                    for (ItemStack slotFrom : source.getContents()) {
                        if (slotFrom != null) {
                            for (ItemStack slotTo : target.getContents()) {
                                if (slotTo == null || (slotTo.getType().equals(slotFrom.getType()) && slotTo.getAmount() < slotTo.getMaxStackSize())) {
                                    sourceSlot = source.getItem(source.first(slotFrom));
                                    break loops;
                                }
                            }
                        }
                    }

                    if (sourceSlot != null) {
                        if (blockFrom.getChunk().isLoaded() && blockTo.getChunk().isLoaded()) {
                            if (sourceSlot.hasItemMeta())
                                if (sourceSlot.getItemMeta() instanceof BlockStateMeta) {
                                    BlockStateMeta meta = (BlockStateMeta) sourceSlot.getItemMeta();
                                    if (meta.getBlockState() instanceof ShulkerBox) {
                                        ItemStack item = new ItemStack(sourceSlot.getType());
                                        BlockStateMeta itemMeta = (BlockStateMeta) item.getItemMeta();
                                        ShulkerBox shulker  = (ShulkerBox) itemMeta.getBlockState();

                                        shulker.getInventory().setContents(((ShulkerBox) meta.getBlockState()).getInventory().getContents());
                                        meta.setBlockState(shulker);
                                        shulker.update();
                                        item.setItemMeta(meta);

                                        target.addItem(item);
                                        source.setItem(source.first(sourceSlot), null);
                                        particles(blockFrom, blockTo, Color.LIME);
                                        continue;
                                    }
                                }

                            target.addItem(new ItemStack(sourceSlot.getType(), 1));
                            sourceSlot.setAmount(sourceSlot.getAmount() - 1);
                            particles(blockFrom, blockTo, Color.LIME);
                        } else {
                            particles(blockFrom, blockTo, Color.YELLOW);
                        }
                    } else {
                        particles(blockFrom, blockTo, Color.RED);
                    }
                }
            }
        }
    }

    public void particles(Location location1, Location location2, Color particlesColor) {
        List<Location> inputLocations = List.of(location1, location2);
        for (Location inputLocation : inputLocations) {
            if (particlesEnabled) {
                List<Location> locations = new ArrayList<>();
                InventoryHolder holder = ((Container) inputLocation.getBlock().getState()).getInventory().getHolder();
                if (holder instanceof DoubleChest) {
                    DoubleChest doubleChest = ((DoubleChest) holder);
                    //noinspection ConstantConditions
                    locations.add(((Chest) doubleChest.getLeftSide()).getLocation());
                    //noinspection ConstantConditions
                    locations.add(((Chest) doubleChest.getRightSide()).getLocation());
                } else {
                    locations.add(inputLocation);
                }

                for (Location location : locations) {
                    BoundingBox bb = location.getBlock().getBoundingBox();
                    Location offset = bb.getMin().toLocation(location.getBlock().getWorld());

                    Vector basisX = new Vector(Math.abs(bb.getMinX() - bb.getMaxX()), 0, 0);
                    Vector basisY = new Vector(0, Math.abs(bb.getMinY() - bb.getMaxY()), 0);
                    Vector basisZ = new Vector(0, 0, Math.abs(bb.getMinZ() - bb.getMaxZ()));
                    
                    drawParticle(basisX, offset, particlesColor); //bottomFrontX
                    drawParticle(basisY, offset, particlesColor); //frontY
                    drawParticle(basisZ, offset, particlesColor); //bottomFrontZ
                    drawParticle(basisX, offset.clone().add(basisZ), particlesColor); //bottomBackX
                    drawParticle(basisZ, offset.clone().add(basisX), particlesColor); //bottomBackZ
                    drawParticle(basisY, offset.clone().add(basisX), particlesColor); //rightY
                    drawParticle(basisY, offset.clone().add(basisZ), particlesColor); //leftY
                    drawParticle(basisY, offset.clone().add(basisX).add(basisZ), particlesColor); //backY
                    drawParticle(basisX, offset.clone().add(basisY), particlesColor); //topFrontX
                    drawParticle(basisZ, offset.clone().add(basisY), particlesColor); //topFrontZ
                    drawParticle(basisX, offset.clone().add(basisY).add(basisZ), particlesColor); //topBackX
                    drawParticle(basisZ, offset.clone().add(basisY).add(basisX), particlesColor); //topBackZ
                }
            }
        }
    }

    private void drawParticle(Vector toDraw, Location offset, Color color) {
        for (double d = 0; d <= toDraw.length(); d += 0.01) {
            Location toSpawn = offset.clone().add(toDraw.clone().multiply(d));
            toSpawn.getWorld().spawnParticle(Particle.REDSTONE, toSpawn, 2, 0, 0, 0, 0, new Particle.DustOptions(color, 0.2f));
        }
    }
}
