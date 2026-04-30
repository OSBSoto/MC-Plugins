package com.osbsoto.homesystem.commands;

import com.osbsoto.homesystem.HomeSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelHomeCommand implements CommandExecutor {

    private final HomeSystem plugin;

    public DelHomeCommand(HomeSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("homesystem.delhome")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /delhome <name>");
            return true;
        }

        String homeName = args[0].toLowerCase();
        boolean deleted = plugin.getHomeManager().deleteHome(player.getUniqueId(), homeName);

        if (deleted) {
            plugin.getHomeManager().saveHomes();
            player.sendMessage(ChatColor.GREEN + "Home '" + homeName + "' has been deleted.");
        } else {
            player.sendMessage(ChatColor.RED + "Home '" + homeName + "' does not exist.");
        }

        return true;
    }
}
