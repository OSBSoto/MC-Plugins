package com.osbsoto.teleportsystem.commands;

import com.osbsoto.teleportsystem.TeleportSystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final TeleportSystem plugin;

    public TpaCommand(TeleportSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("teleportsystem.tpa")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /tpa <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot send a teleport request to yourself.");
            return true;
        }

        plugin.getTeleportManager().createTpaRequest(player.getUniqueId(), target.getUniqueId());

        int expireTime = plugin.getTpaExpireTime();
        player.sendMessage(ChatColor.GREEN + "Teleport request sent to " + target.getName() + ". It will expire in " + expireTime + " seconds.");
        target.sendMessage(ChatColor.YELLOW + player.getName() + " has requested to teleport to you.");
        target.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GREEN + "/tpaccept" + ChatColor.YELLOW + " to accept or " + ChatColor.RED + "/tpdeny" + ChatColor.YELLOW + " to deny.");
        return true;
    }
}
