package com.osbsoto.basicutilities.commands;

import com.osbsoto.basicutilities.BasicUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GodCommand implements CommandExecutor {

    private final BasicUtilities plugin;

    public GodCommand(BasicUtilities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("basicutilities.god")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /god <player>");
                return true;
            }
            plugin.toggleGodMode(player.getUniqueId());
            boolean isGod = plugin.isGodMode(player.getUniqueId());
            String stateStr = isGod ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
            player.sendMessage(ChatColor.YELLOW + "God mode " + stateStr + ChatColor.YELLOW + ".");
            return true;
        }

        if (!sender.hasPermission("basicutilities.god.others")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to toggle god mode for other players.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        plugin.toggleGodMode(target.getUniqueId());
        boolean isGod = plugin.isGodMode(target.getUniqueId());
        String stateStr = isGod ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        target.sendMessage(ChatColor.YELLOW + "God mode has been " + stateStr + ChatColor.YELLOW + " for you by " + sender.getName() + ".");
        sender.sendMessage(ChatColor.YELLOW + "God mode " + stateStr + ChatColor.YELLOW + " for " + target.getName() + ".");
        return true;
    }
}
