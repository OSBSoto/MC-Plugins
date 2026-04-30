package com.osbsoto.teleportsystem.commands;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpawnCommand implements CommandExecutor {

    private final TeleportSystem plugin;

    public SpawnCommand(TeleportSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("teleportsystem.spawn")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        String worldName = player.getWorld().getName();
        Location spawnLoc = plugin.getSpawnManager().getSpawn(worldName);

        if (spawnLoc == null) {
            // Fall back to the world's default spawn if custom spawn is not set
            spawnLoc = player.getWorld().getSpawnLocation();
        }

        final Location destination = spawnLoc;
        int delay = plugin.getTeleportDelay();

        if (delay <= 0) {
            plugin.getTeleportManager().saveLastLocation(player.getUniqueId(), player.getLocation());
            player.teleport(destination);
            player.sendMessage(ChatColor.GREEN + "Teleported to spawn.");
            return true;
        }

        Location startLocation = player.getLocation().clone();
        player.sendMessage(ChatColor.YELLOW + "Teleporting to spawn in " + delay + " seconds. Don't move!");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.isCancelOnMove() && hasPlayerMoved(startLocation, player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "Teleport cancelled because you moved.");
                    return;
                }
                plugin.getTeleportManager().saveLastLocation(player.getUniqueId(), player.getLocation());
                player.teleport(destination);
                player.sendMessage(ChatColor.GREEN + "Teleported to spawn.");
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
