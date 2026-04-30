package com.osbsoto.teleportsystem.commands;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final TeleportSystem plugin;

    public SetSpawnCommand(TeleportSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("teleportsystem.setspawn")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        plugin.getSpawnManager().setSpawn(player.getWorld().getName(), player.getLocation());
        player.sendMessage(ChatColor.GREEN + "Spawn has been set in world '" + player.getWorld().getName() + "'.");
        return true;
    }
}
