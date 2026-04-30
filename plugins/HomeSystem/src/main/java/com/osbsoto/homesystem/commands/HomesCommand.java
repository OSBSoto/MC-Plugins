package com.osbsoto.homesystem.commands;

import com.osbsoto.homesystem.HomeSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class HomesCommand implements CommandExecutor {

    private final HomeSystem plugin;

    public HomesCommand(HomeSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("homesystem.homes")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Set<String> homeNames = plugin.getHomeManager().getHomeNames(player.getUniqueId());

        if (homeNames.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no homes set. Use /sethome to set one.");
            return true;
        }

        int max = player.hasPermission("homesystem.multiplehomes") ? -1 : plugin.getMaxHomes();
        String limitStr = max < 0 ? "unlimited" : String.valueOf(max);

        player.sendMessage(ChatColor.GOLD + "--- Your Homes (" + homeNames.size() + "/" + limitStr + ") ---");
        for (String name : homeNames) {
            player.sendMessage(ChatColor.YELLOW + "  - " + name);
        }
        return true;
    }
}
