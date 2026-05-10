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

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Console usage: bugreport reload");
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
        // Only suggest "reload" as first argument and only to OPs.
        // Return empty list in all other cases to suppress default player-name suggestions.
        if (args.length == 1 && sender.isOp()) {
            String partial = args[0].toLowerCase();
            if ("reload".startsWith(partial)) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
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

