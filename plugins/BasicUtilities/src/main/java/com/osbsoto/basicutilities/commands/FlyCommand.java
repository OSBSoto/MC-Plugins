package com.osbsoto.basicutilities.commands;

import com.osbsoto.basicutilities.BasicUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    private final BasicUtilities plugin;

    public FlyCommand(BasicUtilities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("basicutilities.fly")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /fly <player>");
                return true;
            }
            toggleFly(player, sender);
            return true;
        }

        if (!sender.hasPermission("basicutilities.fly.others")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to toggle fly for other players.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        if (!newState) {
            target.setFlying(false);
        }

        String stateStr = newState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        target.sendMessage(ChatColor.YELLOW + "Fly mode has been " + stateStr + ChatColor.YELLOW + " for you by " + sender.getName() + ".");
        sender.sendMessage(ChatColor.YELLOW + "Fly mode " + stateStr + ChatColor.YELLOW + " for " + target.getName() + ".");
        return true;
    }

    private void toggleFly(Player player, CommandSender sender) {
        boolean newState = !player.getAllowFlight();
        player.setAllowFlight(newState);
        if (!newState) {
            player.setFlying(false);
        }
        String stateStr = newState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        player.sendMessage(ChatColor.YELLOW + "Fly mode " + stateStr + ChatColor.YELLOW + ".");
    }
}
