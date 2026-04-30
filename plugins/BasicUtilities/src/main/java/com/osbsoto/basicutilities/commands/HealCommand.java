package com.osbsoto.basicutilities.commands;

import com.osbsoto.basicutilities.BasicUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand implements CommandExecutor {

    private final BasicUtilities plugin;

    public HealCommand(BasicUtilities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("basicutilities.heal")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /heal <player>");
                return true;
            }
            healPlayer(player);
            player.sendMessage(ChatColor.GREEN + "You have been healed!");
            return true;
        }

        if (!sender.hasPermission("basicutilities.heal.others")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to heal other players.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        healPlayer(target);
        target.sendMessage(ChatColor.GREEN + "You have been healed by " + sender.getName() + "!");
        sender.sendMessage(ChatColor.GREEN + "Healed " + target.getName() + ".");
        return true;
    }

    private void healPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
    }
}
