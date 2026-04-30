package com.osbsoto.teleportsystem.commands;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BackCommand implements CommandExecutor {

    private final TeleportSystem plugin;

    public BackCommand(TeleportSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("teleportsystem.back")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Location lastLoc = plugin.getTeleportManager().getLastLocation(player.getUniqueId());
        if (lastLoc == null) {
            player.sendMessage(ChatColor.RED + "No previous location found.");
            return true;
        }

        int delay = plugin.getTeleportDelay();

        if (delay <= 0) {
            Location current = player.getLocation().clone();
            player.teleport(lastLoc);
            plugin.getTeleportManager().saveLastLocation(player.getUniqueId(), current);
            player.sendMessage(ChatColor.GREEN + "Teleported to your previous location.");
            return true;
        }

        Location startLocation = player.getLocation().clone();
        player.sendMessage(ChatColor.YELLOW + "Teleporting to your previous location in " + delay + " seconds. Don't move!");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.isCancelOnMove() && hasPlayerMoved(startLocation, player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "Teleport cancelled because you moved.");
                    return;
                }
                Location current = player.getLocation().clone();
                player.teleport(lastLoc);
                plugin.getTeleportManager().saveLastLocation(player.getUniqueId(), current);
                player.sendMessage(ChatColor.GREEN + "Teleported to your previous location.");
            }
        }.runTaskLater(plugin, delay * 20L);

        return true;
    }

    private boolean hasPlayerMoved(Location start, Location current) {
        return Math.abs(start.getX() - current.getX()) > 0.1
                || Math.abs(start.getY() - current.getY()) > 0.1
                || Math.abs(start.getZ() - current.getZ()) > 0.1;
    }
}
