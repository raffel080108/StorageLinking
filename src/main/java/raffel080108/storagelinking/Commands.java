package raffel080108.storagelinking;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Commands implements CommandExecutor {
    StorageLinking main;
    public Commands(StorageLinking instance) {
        this.main = instance;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            //Help message (if no arguments were provided)
            if (player != null) {
                player.sendMessage(new TextComponent(
                        "§9§lStorageLinking commands:\n" +
                                 "§e/storageLinking reload: §aReloads the plugin configuration\n" +
                                 "§e/storageLinking toggleParticles: §aToggles, whether particles should be displayed from containers transferring items\n"));
            } else {
                main.getLogger().info(
                        "StorageLinking commands:\n" +
                                "/storageLinking reload: Reloads the plugin configuration\n" +
                                "/storageLinking toggleParticles: Toggles, whether particles should be displayed from containers transferring items\n");
            }
            return true;
        } else {
            //Reload command
            if (args[0].equalsIgnoreCase("reload")) {
                if (player != null)
                    if (!player.hasPermission("storageLinking.reload")) {
                        player.sendMessage(new TextComponent("§cYou do not have permission to execute this command!"));
                        return true;
                    }

                Bukkit.getScheduler().cancelTasks(main);
                main.reloadConfig();

                //Check for invalid config
                try {
                    new YamlConfiguration().load(new File(main.getDataFolder(), "config.yml"));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidConfigurationException e) {
                    try {
                        Files.createFile(Path.of(main.getDataFolder().getPath(), "old_config.yml"));
                        Files.copy(Paths.get(new File(main.getDataFolder(), "config.yml").getPath()), Paths.get(new File(main.getDataFolder(), "old_config.yml").getPath()), REPLACE_EXISTING);
                        main.saveResource("config.yml", true);
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        main.getLogger().severe("Could not save old config");
                    }

                    main.getLogger().warning("Invalid config detected - Current config was backed up to old_config.yml and a new config.yml generated");
                }

                //Load containers
                main.containers.clear();
                List<String> configContainers = main.getConfig().getStringList("containers");
                if (configContainers.size() > 0) {
                    for (String container : configContainers) {
                        Material material = Material.matchMaterial(container);
                        if (material != null) {
                            main.containers.add(material);
                        } else {
                            main.getLogger().warning("Could not load config-string \"containers\" - Please check your config.yml file. Default values will be used for now");
                            main.containers.add(Material.CHEST);
                            main.containers.add(Material.BARREL);
                            main.containers.add(Material.TRAPPED_CHEST);
                            return true;
                        }
                    }

                    main.getLogger().info("Containers loaded successfully");
                } else {
                    main.containers.add(Material.CHEST);
                    main.containers.add(Material.BARREL);
                    main.containers.add(Material.CHEST);
                    List<String> defaultContainers = List.of("CHEST", "BARREL", "TRAPPED_CHEST");
                    main.getConfig().set("containers", defaultContainers);
                    main.saveConfig();

                    main.getLogger().warning("Config-string \"containers\" is empty - Please check your config.yml file. Default values will be used for now");
                }
                new ItemTransferring().runTaskTimer(main, 0L, main.getConfig().getLong("transfer-delay"));

                if (player != null) {
                    player.sendMessage(new TextComponent("§aConfiguration reloaded!"));
                } else {
                    main.getLogger().info("Configuration reloaded!");
                }
                return true;
            }

            //Toggle particles command
            if (args[0].equalsIgnoreCase("toggleParticles")) {
                if (player != null)
                    if (!player.hasPermission("storageLinking.toggleParticles")) {
                        player.sendMessage(new TextComponent("§cYou do not have permission to execute this command!"));
                        return true;
                    }

                if (main.getConfig().getBoolean("enable-particles")) {
                    main.getConfig().set("enable-particles", false);
                    main.saveConfig();
                } else {
                    main.getConfig().set("enable-particles", true);
                    main.saveConfig();
                }

                if (player != null) {
                    player.sendMessage(new TextComponent("§aSuccessfully toggled particles to " + main.getConfig().getBoolean("enable-particles")));
                } else {
                    main.getLogger().info("Successfully toggled particles to " + main.getConfig().getBoolean("enable-particles"));
                }
                return true;
            }
        }

        if (player != null) {
            player.sendMessage(new TextComponent("§cUnknown arguments. Please type /storageLinking for a list of commands"));
        } else {
            main.getLogger().info("Unknown arguments. Please type /storageLinking for a list of commands");
        }
        return true;
    }
}