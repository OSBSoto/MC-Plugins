package com.osbsoto.teleportsystem.commands;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpAcceptCommand implements CommandExecutor {

    private final TeleportSystem plugin;

    public TpAcceptCommand(TeleportSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("teleportsystem.tpaccept")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (!plugin.getTeleportManager().hasPendingRequest(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You have no pending teleport requests.");
            return true;
        }

        UUID requesterUUID = plugin.getTeleportManager().getRequester(player.getUniqueId());
        plugin.getTeleportManager().clearRequest(player.getUniqueId());

        Player requester = Bukkit.getPlayer(requesterUUID);
        if (requester == null) {
            player.sendMessage(ChatColor.RED + "The player who sent the request is no longer online.");
            return true;
        }

        plugin.getTeleportManager().saveLastLocation(requester.getUniqueId(), requester.getLocation());
        requester.teleport(player.getLocation());

        requester.sendMessage(ChatColor.GREEN + "Your teleport request was accepted. Teleporting to " + player.getName() + ".");
        player.sendMessage(ChatColor.GREEN + "Accepted teleport request from " + requester.getName() + ".");
        return true;
    }
}
