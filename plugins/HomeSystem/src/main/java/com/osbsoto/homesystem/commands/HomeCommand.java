package com.osbsoto.homesystem.commands;

import com.osbsoto.homesystem.HomeSystem;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HomeCommand implements CommandExecutor {

    private final HomeSystem plugin;

    public HomeCommand(HomeSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("homesystem.home")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";

        Location home = plugin.getHomeManager().getHome(player.getUniqueId(), homeName);
        if (home == null) {
            if (homeName.equals("home")) {
                player.sendMessage(ChatColor.RED + "You have not set a home yet. Use /sethome to set one.");
            } else {
                player.sendMessage(ChatColor.RED + "Home '" + homeName + "' does not exist.");
            }
            return true;
        }

        int delay = plugin.getTeleportDelay();
        if (delay <= 0) {
            player.teleport(home);
            player.sendMessage(ChatColor.GREEN + "Teleported to home '" + homeName + "'.");
            return true;
        }

        Location startLocation = player.getLocation().clone();
        player.sendMessage(ChatColor.YELLOW + "Teleporting to home '" + homeName + "' in " + delay + " seconds. Don't move!");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.isCancelOnMove() && hasPlayerMoved(startLocation, player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "Teleport cancelled because you moved.");
                    return;
                }
                player.teleport(home);
                player.sendMessage(ChatColor.GREEN + "Teleported to home '" + homeName + "'.");
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
