package com.osbsoto.basicutilities.commands;

import com.osbsoto.basicutilities.BasicUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedCommand implements CommandExecutor {

    private final BasicUtilities plugin;

    public FeedCommand(BasicUtilities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("basicutilities.feed")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /feed <player>");
                return true;
            }
            feedPlayer(player);
            player.sendMessage(ChatColor.GREEN + "You have been fed!");
            return true;
        }

        if (!sender.hasPermission("basicutilities.feed.others")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to feed other players.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        feedPlayer(target);
        target.sendMessage(ChatColor.GREEN + "You have been fed by " + sender.getName() + "!");
        sender.sendMessage(ChatColor.GREEN + "Fed " + target.getName() + ".");
        return true;
    }

    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }
}
