package raffel080108.storagelinking;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getScheduler;
import static raffel080108.storagelinking.Main.plugin;

@SuppressWarnings("NullableProblems")
public class Commands implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
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

                getScheduler().cancelTasks(plugin);
                plugin.reloadConfig();
                //Check for invalid config
                YamlConfiguration yamlConfig = new YamlConfiguration();
                try {
                    yamlConfig.load(new File(plugin.getDataFolder(), "config.yml"));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidConfigurationException e) {
                    try {
                        Files.createFile(Path.of(plugin.getDataFolder().getPath(), "old_config.yml"));
                        Files.copy(Paths.get(new File(plugin.getDataFolder(), "config.yml").getPath()), Paths.get(new File(plugin.getDataFolder(), "old_config.yml").getPath()), REPLACE_EXISTING);
                        plugin.saveResource("config.yml", true);
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        getLogger().severe("Could not save old config");
                    }

                    getLogger().warning("Invalid config detected - Current config was backed up to old_config.yml and a new config.yml generated");
                }

                new ItemTransferring().runTaskTimer(plugin, 0L, plugin.getConfig().getLong("transfer-delay"));

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

                if (plugin.getConfig().getBoolean("enable-particles")) {
                    plugin.getConfig().set("enable-particles", false);
                    plugin.saveConfig();
                } else {
                    plugin.getConfig().set("enable-particles", true);
                    plugin.saveConfig();
                }

                if (player != null) {
                    player.sendMessage(new TextComponent(ChatColor.GREEN + "Successfully toggled particles to " + plugin.getConfig().getBoolean("enable-particles")));
                } else {
                    getLogger().info("Successfully toggled particles to " + plugin.getConfig().getBoolean("enable-particles"));
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