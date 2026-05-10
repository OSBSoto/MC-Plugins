package org.example;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BugCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final DiscordReportSender reportSender;
    private final Map<UUID, Long> cooldownExpiryByPlayer = new ConcurrentHashMap<>();

    public BugCommand(Main plugin, DiscordReportSender reportSender) {
        this.plugin = plugin;
        this.reportSender = reportSender;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "Only OPs can reload BugReport.");
                return true;
            }

            plugin.reloadRuntimeConfiguration();
            sender.sendMessage(ChatColor.GREEN + "BugReport configuration reloaded.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
            sendVersionInfo(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Console usage: bugreport reload | bug version");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <message>");
            return true;
        }

        String message = String.join(" ", args).trim();
        if (message.isBlank()) {
            player.sendMessage(ChatColor.RED + "Please include a message.");
            return true;
        }

        if (isOnCooldown(player)) {
            return true;
        }

        String recentConsole = null;
        ConsoleLineBuffer consoleLineBuffer = plugin.getConsoleLineBuffer();
        if (plugin.getConfig().getBoolean("includeRecentConsoleLine", true) && consoleLineBuffer != null) {
            recentConsole = consoleLineBuffer.getLastLine();
        }

        List<String> recentChat = plugin.getChatHistoryBuffer() != null
                ? plugin.getChatHistoryBuffer().snapshot()
                : List.of();

        BugReportPayload payload = new BugReportPayload(
                Instant.now(),
                player.getUniqueId(),
                player.getName(),
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                message,
                recentConsole,
                recentChat
        );

        applyCooldown(player);

        String initializingMessage = plugin.getConfig().getString("playerMessages.initializing", "Initializing report");
        player.sendMessage(ChatColor.YELLOW + initializingMessage);

        // Keep Discord network work off the main server thread.
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean sent = reportSender.send(payload);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (sent) {
                    String successMessage = plugin.getConfig().getString("playerMessages.success", "bug reported");
                    player.sendMessage(ChatColor.GREEN + successMessage);
                } else {
                    String failureMessage = plugin.getConfig().getString(
                            "playerMessages.failure",
                            "Could not send report to Discord right now."
                    );
                    player.sendMessage(ChatColor.RED + failureMessage);
                }
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> suggestions = new java.util.ArrayList<>();
            if ("version".startsWith(partial)) {
                suggestions.add("version");
            }
            if (sender.isOp() && "reload".startsWith(partial)) {
                suggestions.add("reload");
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    private void sendVersionInfo(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String currentVersion = version;
        LicenseResult result = plugin.getLicenseResult();

        sender.sendMessage(ChatColor.GOLD + "=== BugReport " + ChatColor.YELLOW + "v" + version + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.GRAY + "Validation currentVersion: " + ChatColor.WHITE + currentVersion);

        if (result == null) {
            sender.sendMessage(ChatColor.GRAY + "License: " + ChatColor.YELLOW + "Not checked yet");
        } else if (!result.reachable()) {
            sender.sendMessage(ChatColor.GRAY + "License: " + ChatColor.YELLOW + "Server unreachable");
        } else if (result.isUnlicensedMode()) {
            sender.sendMessage(ChatColor.GRAY + "License mode: " + ChatColor.WHITE + "unlicensed");
        } else if (result.isValid()) {
            sender.sendMessage(ChatColor.GRAY + "License mode: " + ChatColor.GREEN + result.mode());
            if (result.expirationDate() != null && !result.expirationDate().isBlank()) {
                sender.sendMessage(ChatColor.GRAY + "Expires: " + ChatColor.WHITE + result.expirationDate());
            }
            if (result.isUpdateAvailable()) {
                sender.sendMessage(ChatColor.GRAY + "Update: " + ChatColor.AQUA + "v" + result.latestVersion() + " available — " + result.downloadUrl());
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "License mode: " + ChatColor.RED + result.mode());
            String reason = result.blockReason();
            if (reason == null || reason.isBlank()) {
                reason = result.message();
            }
            if (reason != null && !reason.isBlank()) {
                sender.sendMessage(ChatColor.RED + reason);
            }
        }
    }

    private boolean isOnCooldown(Player player) {
        if (!plugin.getConfig().getBoolean("cooldown.enabled", true)) {
            return false;
        }

        Long expiry = cooldownExpiryByPlayer.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (expiry == null || expiry <= now) {
            return false;
        }

        long secondsLeft = Math.max(1L, (expiry - now + 999L) / 1000L);
        String cooldownMessage = plugin.getConfig().getString(
                "cooldown.message",
                "Please wait {seconds_left}s before creating another bug report."
        );
        player.sendMessage(ChatColor.RED + cooldownMessage.replace("{seconds_left}", String.valueOf(secondsLeft)));
        return true;
    }

    private void applyCooldown(Player player) {
        if (!plugin.getConfig().getBoolean("cooldown.enabled", true)) {
            return;
        }

        int cooldownSeconds = Math.max(0, plugin.getConfig().getInt("cooldown.seconds", 45));
        if (cooldownSeconds == 0) {
            return;
        }

        long expiry = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldownExpiryByPlayer.put(player.getUniqueId(), expiry);
    }
}

