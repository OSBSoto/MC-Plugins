package com.osbsoto.basicutilities.commands;

import com.osbsoto.basicutilities.BasicUtilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand implements CommandExecutor {

    private final BasicUtilities plugin;

    public GamemodeCommand(BasicUtilities plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("basicutilities.gamemode")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /gamemode <survival|creative|adventure|spectator> [player]");
            sender.sendMessage(ChatColor.RED + "Aliases: survival=0, creative=1, adventure=2, spectator=3");
            return true;
        }

        GameMode gameMode = parseGameMode(args[0]);
        if (gameMode == null) {
            sender.sendMessage(ChatColor.RED + "Invalid gamemode. Use: survival, creative, adventure, spectator (or 0-3).");
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Usage: /gamemode <mode> <player>");
                return true;
            }
            player.setGameMode(gameMode);
            player.sendMessage(ChatColor.GREEN + "Your gamemode has been set to " + formatGameMode(gameMode) + ".");
            return true;
        }

        if (!sender.hasPermission("basicutilities.gamemode.others")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to change gamemode for other players.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        target.setGameMode(gameMode);
        target.sendMessage(ChatColor.GREEN + "Your gamemode has been set to " + formatGameMode(gameMode) + " by " + sender.getName() + ".");
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s gamemode to " + formatGameMode(gameMode) + ".");
        return true;
    }

    private GameMode parseGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "survival", "s", "0" -> GameMode.SURVIVAL;
            case "creative", "c", "1" -> GameMode.CREATIVE;
            case "adventure", "a", "2" -> GameMode.ADVENTURE;
            case "spectator", "sp", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private String formatGameMode(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> "Survival";
            case CREATIVE -> "Creative";
            case ADVENTURE -> "Adventure";
            case SPECTATOR -> "Spectator";
        };
    }
}
