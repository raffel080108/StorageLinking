/*
 StorageLinking © 2022 by Raphael "raffel080108" Roehrig is licensed under CC BY-NC-SA 4.0. To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/4.0/
 */

package raffel080108.storagelinking;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.List;

import static org.bukkit.event.EventPriority.HIGH;

public class Linking implements Listener {
    StorageLinking main;
    public Linking(StorageLinking instance) {
        this.main = instance;
    }

    private HashMap<Player, Location> firstLocation = new HashMap<>();

    @EventHandler(priority=HIGH)
    public void playerInteractEvent(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;
        Player player = event.getPlayer();
        Location blockLocation = event.getClickedBlock().getLocation();

        @SuppressWarnings("ConstantConditions")
        Material linkingItem = Material.matchMaterial(main.getConfig().getString("linking-item"));
        if (linkingItem == null) {
            linkingItem = Material.STICK;
            main.getLogger().warning("Config-string \"linking-item\" is invalid - Please check your config.yml file. Default value will be used for now");
        }

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) && player.getEquipment().getItemInMainHand().getType().equals(linkingItem) && main.containers.contains(event.getClickedBlock().getType())) {
            if (main.getConfig().getBoolean("enable-usage-permission") && !player.hasPermission("storageLinking.use")) {
                event.setCancelled(true);
                firstLocation.remove(player);
                if (main.getConfig().getBoolean("no-usage-permission-message"))
                    player.sendMessage(new TextComponent("§cYou do not have permission to link containers"));

                return;
            }

            //Linking
            if (!player.isSneaking()) {
                if (!(main.storedLocations.containsKey(blockLocation) || main.storedLocations.containsValue(blockLocation))) {
                    if (!firstLocation.containsKey(player)) {
                        event.setCancelled(true);
                        firstLocation.put(player, blockLocation);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                        player.sendMessage(new TextComponent("§aClick another container to link this container with. Use Shift+LeftClick to cancel"));
                    } else {
                        if (!firstLocation.get(player).equals(blockLocation) && antiDoubleChest(blockLocation, firstLocation.get(player))) {
                            event.setCancelled(true);
                            main.storedLocations.put(firstLocation.get(player), blockLocation);
                            firstLocation.remove(player);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                            int x = blockLocation.getBlockX();
                            int y = blockLocation.getBlockY();
                            int z = blockLocation.getBlockZ();
                            player.sendMessage(new TextComponent("§aSuccessfully linked this container to the container at §cX: " + x + ", Y: " + y + ", Z: " + z));
                            player.sendMessage(new TextComponent("§6To unlink these containers, use Shift+LeftClick on one of them. Items will transfer from the block you selected first to the block you selected second"));
                        } else {
                            event.setCancelled(true);
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                            player.sendMessage(new TextComponent("§4You cannot link a container to itself"));
                        }
                    }
                } else {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    player.sendMessage(new TextComponent("§4This container is already linked with another container. Use Shift+LeftClick to unlink them"));
                }
            } else {
                //Unlinking
                event.setCancelled(true);
                if (main.storedLocations.containsKey(blockLocation) || main.storedLocations.containsValue(blockLocation)) {
                    for (HashMap.Entry<Location, Location> entry : main.storedLocations.entrySet()) {
                        if (entry.getKey().equals(blockLocation) || entry.getValue().equals(blockLocation)) {
                            Location location1 = entry.getKey();
                            Location location2 = entry.getValue();
                            int x1 = location1.getBlockX();
                            int y1 = location1.getBlockY();
                            int z1 = location1.getBlockZ();
                            int x2 = location2.getBlockX();
                            int y2 = location2.getBlockY();
                            int z2 = location2.getBlockZ();
                            main.storedLocations.remove(entry.getKey());

                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                            player.sendMessage(new TextComponent("§aSuccessfully unlinked containers at §cX: " + x1 + ", Y: " + y1 + ", Z: " + z1 + "§a and §cX: " + x2 + ", Y: " + y2 + ", Z: " + z2));
                            break;
                        }
                    }
                } else {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1f, 1f);
                    player.sendMessage(new TextComponent("§4Container is not linked"));
                }
            }
        }

        //Linking cancelling
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK) && firstLocation.containsKey(player) && player.isSneaking() && !main.containers.contains(event.getClickedBlock().getType()) && player.getEquipment().getItemInMainHand().getType().equals(linkingItem)) {
            event.setCancelled(true);
            firstLocation.remove(player);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            player.sendMessage(new TextComponent("§cCancelled linking"));
        }
    }

    private boolean antiDoubleChest(Location location, Location otherLocation) {
        BlockState state = location.getBlock().getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            InventoryHolder holder = chest.getInventory().getHolder();
            if (holder instanceof DoubleChest) {
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

        if (main.containers.contains(blockLocation.getBlock().getType())) {
            if (main.storedLocations.containsKey(blockLocation) || main.storedLocations.containsValue(blockLocation)) {
                for (HashMap.Entry<Location, Location> entry : main.storedLocations.entrySet()) {
                    if (entry.getKey().equals(blockLocation) || entry.getValue().equals(blockLocation)) {
                        main.storedLocations.remove(entry.getKey());

                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                        player.sendMessage(new TextComponent("§cYou broke a linked container and removed it's link"));
                        break;
                    }
                }
            }
        }
    }
}
