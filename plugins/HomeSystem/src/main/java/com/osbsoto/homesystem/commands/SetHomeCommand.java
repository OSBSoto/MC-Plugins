package com.osbsoto.homesystem.commands;

import com.osbsoto.homesystem.HomeSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetHomeCommand implements CommandExecutor {

    private final HomeSystem plugin;

    public SetHomeCommand(HomeSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("homesystem.sethome")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";

        // Check home limit
        if (!player.hasPermission("homesystem.multiplehomes")) {
            int current = plugin.getHomeManager().getHomeCount(player.getUniqueId());
            int max = plugin.getMaxHomes();
            if (current >= max && !plugin.getHomeManager().hasHome(player.getUniqueId(), homeName)) {
                player.sendMessage(ChatColor.RED + "You have reached the maximum number of homes (" + max + ").");
                player.sendMessage(ChatColor.RED + "Delete a home with /delhome before setting a new one.");
                return true;
            }
        }

        plugin.getHomeManager().setHome(player.getUniqueId(), homeName, player.getLocation());
        plugin.getHomeManager().saveHomes();

        player.sendMessage(ChatColor.GREEN + "Home '" + homeName + "' has been set.");
        return true;
    }
}
