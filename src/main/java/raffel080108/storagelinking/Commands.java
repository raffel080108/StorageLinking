package raffel080108.storagelinking;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getLogger;
import static raffel080108.storagelinking.DataHandling.loadConfig;
import static raffel080108.storagelinking.DataHandling.particlesEnabled;
import static raffel080108.storagelinking.Main.plugin;

@SuppressWarnings("NullableProblems")
public class Commands implements CommandExecutor {
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