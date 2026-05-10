package org.example;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// DiscordSRV shades JDA into its own package; try relocated path first then canonical.
// JDA version used by DiscordSRV 1.26+ is JDA 4.x.

public final class DiscordReportSender {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private final Main plugin;

    public DiscordReportSender(Main plugin) {
        this.plugin = plugin;
    }

    public boolean send(BugReportPayload report) {
        Plugin discordSrv = plugin.getServer().getPluginManager().getPlugin("DiscordSRV");
        if (discordSrv == null || !discordSrv.isEnabled()) {
            return false;
        }

        String channelName = plugin.getConfig().getString("discordChannel", "admin");
        try {
            Class<?> discordSrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Object discordSrvPlugin = discordSrvClass.getMethod("getPlugin").invoke(null);
            Method channelResolver = discordSrvPlugin.getClass()
                    .getMethod("getDestinationTextChannelForGameChannelName", String.class);
            Object channel = channelResolver.invoke(discordSrvPlugin, channelName);
            if (channel == null) {
                plugin.getLogger().warning("DiscordSRV channel mapping not found for: " + channelName);
                return false;
            }

            YamlConfiguration template = loadTemplate();
            Map<String, String> placeholders = createPlaceholders(report, template);

            if (sendEmbed(channel, template, placeholders)) {
                return true;
            }

            String fallbackMessage = buildPlainTextFallback(placeholders);
            return sendPlainText(channel, fallbackMessage);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Failed to resolve DiscordSRV API: " + exception.getMessage());
            return false;
        }
    }

    private YamlConfiguration loadTemplate() {
        String templateFileName = plugin.getConfig().getString("embedTemplateFile", "discord-embed.yml");
        File templateFile = new File(plugin.getDataFolder(), templateFileName);
        if (!templateFile.exists()) {
            plugin.saveResource("discord-embed.yml", false);
            templateFile = new File(plugin.getDataFolder(), "discord-embed.yml");
        }
        return YamlConfiguration.loadConfiguration(templateFile);
    }

    private Map<String, String> createPlaceholders(BugReportPayload report, YamlConfiguration template) {
        Map<String, String> placeholders = new HashMap<>();
        String cleanUserMessage = ChatColor.stripColor(report.message()).replace('\n', ' ').replace('\r', ' ');
        String recentConsoleLine = report.recentConsoleLine();
        if (recentConsoleLine == null || recentConsoleLine.isBlank()) {
            recentConsoleLine = "(none)";
        }

        placeholders.put("timestamp", TIMESTAMP_FORMAT.format(report.timestamp()));
        placeholders.put("timestamp_epoch", String.valueOf(report.timestamp().getEpochSecond()));
        placeholders.put("username", report.username());
        placeholders.put("world", report.world());
        placeholders.put("x", String.format("%.2f", report.x()));
        placeholders.put("y", String.format("%.2f", report.y()));
        placeholders.put("z", String.format("%.2f", report.z()));
        placeholders.put("xyz", String.format("%.2f, %.2f, %.2f", report.x(), report.y(), report.z()));
        placeholders.put("message", cleanUserMessage);
        placeholders.put("console_line", recentConsoleLine.replace('\n', ' ').replace('\r', ' '));
        placeholders.put("mention", template.getString("embed.mention", ""));

        String linkedDiscordId = resolveLinkedDiscordId(report.playerUuid());
        boolean includeLinkedMention = plugin.getConfig().getBoolean("linkedDiscord.includeLinkedMention", true);
        String linkedPlaceholder = plugin.getConfig().getString("linkedDiscord.unlinkedPlaceholder", "(not linked)");
        if (!includeLinkedMention) {
            linkedPlaceholder = "";
        } else if (linkedDiscordId != null && !linkedDiscordId.isBlank()) {
            linkedPlaceholder = "<@" + linkedDiscordId + ">";
        }
        placeholders.put("linked_discord_mention", linkedPlaceholder == null ? "" : linkedPlaceholder);
        placeholders.put("linked_discord_id", linkedDiscordId == null ? "" : linkedDiscordId);

        // Recent chat history — join lines, truncate to 950 chars (Discord field limit is 1024)
        List<String> chatLines = report.recentChatLines();
        String chatHistory;
        if (chatLines == null || chatLines.isEmpty()) {
            chatHistory = "(none)";
        } else {
            String joined = String.join("\n", chatLines);
            chatHistory = joined.length() > 950 ? joined.substring(joined.length() - 950) : joined;
        }
        placeholders.put("chat_history", chatHistory);

        return placeholders;
    }

    private String resolveLinkedDiscordId(java.util.UUID playerUuid) {
        if (!plugin.getConfig().getBoolean("linkedDiscord.enabled", true)) {
            return null;
        }

        try {
            Class<?> discordSrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Object discordSrvPlugin = discordSrvClass.getMethod("getPlugin").invoke(null);
            if (discordSrvPlugin == null) {
                return null;
            }

            Method getAccountLinkManager = discordSrvPlugin.getClass().getMethod("getAccountLinkManager");
            Object accountLinkManager = getAccountLinkManager.invoke(discordSrvPlugin);
            if (accountLinkManager == null) {
                return null;
            }

            try {
                Method getDiscordId = accountLinkManager.getClass().getMethod("getDiscordId", java.util.UUID.class);
                Object result = getDiscordId.invoke(accountLinkManager, playerUuid);
                return result != null ? String.valueOf(result) : null;
            } catch (NoSuchMethodException ignored) {
                // Fallback to common alternative method names across DiscordSRV versions.
            }

            for (Method method : accountLinkManager.getClass().getMethods()) {
                if (!method.getName().toLowerCase().contains("discord") || method.getParameterCount() != 1) {
                    continue;
                }

                Class<?> parameterType = method.getParameterTypes()[0];
                if (parameterType != java.util.UUID.class) {
                    continue;
                }

                Object result = method.invoke(accountLinkManager, playerUuid);
                if (result != null) {
                    return String.valueOf(result);
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Linking lookup is optional; keep sending report even if reflection fails.
        }

        return null;
    }

    /**
     * Candidate class paths for JDA's EmbedBuilder.
     * DiscordSRV relocates JDA into its own shaded package; try that first.
     */
    private static final String[] EMBED_BUILDER_CANDIDATES = {
            "github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder",
            "net.dv8tion.jda.api.EmbedBuilder"
    };

    private static final String[] MESSAGE_EMBED_CANDIDATES = {
            "github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed",
            "net.dv8tion.jda.api.entities.MessageEmbed"
    };

    private static Class<?> resolveClass(String[] candidates) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String candidate : candidates) {
            try {
                return Class.forName(candidate);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        throw last;
    }

    private boolean sendEmbed(Object channel, YamlConfiguration template, Map<String, String> placeholders) {
        ConfigurationSection embed = template.getConfigurationSection("embed");
        if (embed == null || !embed.getBoolean("enabled", true)) {
            return false;
        }

        try {
            String title = applyPlaceholders(embed.getString("title", "New Bug Report"), placeholders);
            String description = applyPlaceholders(embed.getString("description", "{message}"), placeholders);
            String footer = applyPlaceholders(embed.getString("footer", ""), placeholders);
            String mention = applyPlaceholders(embed.getString("mention", ""), placeholders);
            String thumbnail = applyPlaceholders(embed.getString("thumbnailUrl", ""), placeholders);
            int color = parseColor(embed.getString("color", "#E74C3C"));

            Class<?> embedBuilderClass = resolveClass(EMBED_BUILDER_CANDIDATES);
            Object builder = embedBuilderClass.getConstructor().newInstance();

            // JDA 4 EmbedBuilder uses setColor(int) - safest over reflection
            builder.getClass().getMethod("setTitle", String.class).invoke(builder, title);
            builder.getClass().getMethod("setDescription", CharSequence.class).invoke(builder, description);
            builder.getClass().getMethod("setColor", int.class).invoke(builder, color);
            if (!footer.isBlank()) {
                builder.getClass().getMethod("setFooter", String.class).invoke(builder, footer);
            }
            if (!thumbnail.isBlank()) {
                builder.getClass().getMethod("setThumbnail", String.class).invoke(builder, thumbnail);
            }

            List<Map<?, ?>> fields = template.getMapList("embed.fields");
            for (Map<?, ?> rawField : fields) {
                Object rawName = rawField.get("name");
                Object rawValue = rawField.get("value");
                Object rawInline = rawField.get("inline");

                String name = applyPlaceholders(rawName != null ? String.valueOf(rawName) : "Field", placeholders);
                String value = applyPlaceholders(rawValue != null ? String.valueOf(rawValue) : "-", placeholders);
                boolean inline = rawInline != null && Boolean.parseBoolean(String.valueOf(rawInline));
                builder.getClass().getMethod("addField", String.class, String.class, boolean.class)
                        .invoke(builder, name, value, inline);
            }

            Object messageEmbed = builder.getClass().getMethod("build").invoke(builder);

            // JDA 4: TextChannel.sendMessage(MessageEmbed) — not sendMessageEmbeds (that's JDA 5)
            Class<?> messageEmbedClass = resolveClass(MESSAGE_EMBED_CANDIDATES);
            Method sendMessageMethod = channel.getClass().getMethod("sendMessage", messageEmbedClass);
            Object action = sendMessageMethod.invoke(channel, messageEmbed);

            if (!mention.isBlank()) {
                // Prepend mention as message content using sendMessage(CharSequence)
                Method sendMentionMethod = channel.getClass().getMethod("sendMessage", CharSequence.class);
                Object mentionAction = sendMentionMethod.invoke(channel, mention);
                mentionAction.getClass().getMethod("queue").invoke(mentionAction);
            }

            action.getClass().getMethod("queue").invoke(action);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Embed send failed (" + exception.getClass().getName()
                    + "): " + exception.getMessage() + " — falling back to plain text.");
            return false;
        }
    }

    private boolean sendPlainText(Object channel, String message) {
        try {
            Class<?> discordUtilClass = Class.forName("github.scarsz.discordsrv.util.DiscordUtil");
            Method queueMessage = findQueueMessageMethod(discordUtilClass);
            if (queueMessage == null) {
                plugin.getLogger().warning("Could not find DiscordUtil.queueMessage(channel, message)");
                return false;
            }

            queueMessage.invoke(null, channel, message);
            return true;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Fallback plain text send failed: " + exception.getMessage());
            return false;
        }
    }

    private Method findQueueMessageMethod(Class<?> discordUtilClass) {
        for (Method method : discordUtilClass.getMethods()) {
            if (!method.getName().equals("queueMessage") || method.getParameterCount() != 2) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes[1] == String.class) {
                return method;
            }
        }
        return null;
    }


    private int parseColor(String rawColor) {
        String normalized = rawColor.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException ignored) {
            return 0xE74C3C;
        }
    }

    private String applyPlaceholders(String template, Map<String, String> placeholders) {
        String value = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return value;
    }

    private String buildPlainTextFallback(Map<String, String> placeholders) {
        return """
                :beetle: **New Bug Report**
                **Time:** %s
                **Player:** %s
                **World:** %s
                **XYZ:** %s
                **Message:** %s
                **Last console line:** %s
                """.formatted(
                placeholders.getOrDefault("timestamp", "-"),
                placeholders.getOrDefault("username", "-"),
                placeholders.getOrDefault("world", "-"),
                placeholders.getOrDefault("xyz", "-"),
                placeholders.getOrDefault("message", "-"),
                placeholders.getOrDefault("console_line", "-")
        );
    }
}

