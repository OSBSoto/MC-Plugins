package com.osbsoto.teleportsystem.commands;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpDenyCommand implements CommandExecutor {

    private final TeleportSystem plugin;

    public TpDenyCommand(TeleportSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("teleportsystem.tpdeny")) {
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
        if (requester != null) {
            requester.sendMessage(ChatColor.RED + player.getName() + " denied your teleport request.");
        }

        player.sendMessage(ChatColor.YELLOW + "Denied teleport request from " + (requester != null ? requester.getName() : "a player") + ".");
        return true;
    }
}
