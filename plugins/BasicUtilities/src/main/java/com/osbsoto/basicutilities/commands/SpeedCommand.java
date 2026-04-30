package com.osbsoto.basicutilities.commands;

import com.osbsoto.basicutilities.BasicUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpeedCommand implements CommandExecutor {

    private final BasicUtilities plugin;

    public SpeedCommand(BasicUtilities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("basicutilities.speed")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /speed <1-10> [fly|walk] [player]");
            return true;
        }

        int speedLevel;
        try {
            speedLevel = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Speed must be a number between 1 and 10.");
            return true;
        }

        if (speedLevel < 1 || speedLevel > 10) {
            sender.sendMessage(ChatColor.RED + "Speed must be between 1 and 10.");
            return true;
        }

        // Default to walk, detect "fly" keyword
        boolean isFly = false;
        Player target = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("fly")) {
                isFly = true;
            } else if (args[i].equalsIgnoreCase("walk")) {
                isFly = false;
            } else {
                // Treat as player name
                target = Bukkit.getPlayer(args[i]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[i]);
                    return true;
                }
            }
        }

        if (target == null) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /speed <1-10> [fly|walk] <player>");
                return true;
            }
            target = player;
        } else if (!sender.hasPermission("basicutilities.speed.others")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to change speed for other players.");
            return true;
        }

        // Spigot speed: 0.0f to 1.0f (default walk: 0.2, fly: 0.1)
        float speed = speedLevel / 10.0f;

        if (isFly) {
            target.setFlySpeed(speed);
            String msg = ChatColor.GREEN + "Fly speed set to " + speedLevel + " for " + target.getName() + ".";
            target.sendMessage(ChatColor.GREEN + "Your fly speed has been set to " + speedLevel + ".");
            if (!target.equals(sender)) {
                sender.sendMessage(msg);
            }
        } else {
            target.setWalkSpeed(speed);
            String msg = ChatColor.GREEN + "Walk speed set to " + speedLevel + " for " + target.getName() + ".";
            target.sendMessage(ChatColor.GREEN + "Your walk speed has been set to " + speedLevel + ".");
            if (!target.equals(sender)) {
                sender.sendMessage(msg);
            }
        }

        return true;
    }
}
