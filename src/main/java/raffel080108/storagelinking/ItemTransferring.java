package raffel080108.storagelinking;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ItemTransferring extends BukkitRunnable {
    public ItemTransferring() {}

    StorageLinking main;
    public ItemTransferring(StorageLinking instance) {
        this.main = instance;
    }

    @Override
    public void run() {
        if (!main.storedLocations.isEmpty()) {
            for (HashMap.Entry<Location, Location> entry : main.storedLocations.entrySet()) {
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

    @SuppressWarnings("ConstantConditions")
    private void particles(Location location1, Location location2, Color particlesColor) {
        List<Location> inputLocations = List.of(location1, location2);
        for (Location inputLocation : inputLocations) {
            if (main.getConfig().getBoolean("enable-particles")) {
                List<Location> locations = new ArrayList<>();
                InventoryHolder holder = ((Container) inputLocation.getBlock().getState()).getInventory().getHolder();
                if (holder instanceof DoubleChest) {
                    DoubleChest doubleChest = ((DoubleChest) holder);
                    locations.add(((Chest) doubleChest.getLeftSide()).getLocation());
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