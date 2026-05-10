package org.example;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class Main extends JavaPlugin {

    // 24 hours in server ticks (20 ticks/s)
    private static final long TICKS_PER_24H = 20L * 60 * 60 * 24;

    private final long startupStartedAtMillis = System.currentTimeMillis();
    private ConsoleLineBuffer consoleLineBuffer;
    private ChatHistoryBuffer chatHistoryBuffer;
    private LicenseResult licenseResult;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureConfigDefaultsMerged();
        ensureEmbedTemplateDefaultsMerged();

        LicenseResult licenseResult = new LicenseValidator(this).validate();
        this.licenseResult = licenseResult;

        if (!licenseResult.reachable()) {
            getLogger().warning("License server unreachable. Continuing startup (fail-open). " + licenseResult.message());
        } else if (licenseResult.isBlockedMode() || !licenseResult.isValid()) {
            String blockReason = licenseResult.blockReason().isBlank() ? licenseResult.message() : licenseResult.blockReason();
            logInvalidLicenseAndDisable(blockReason);
            return;
        } else if (licenseResult.isUnlicensedMode()) {
            scheduleUnlicensedWarning();
        }

        if (licenseResult.isUpdateAvailable()) {
            notifyUpdateAvailable(licenseResult);
        }

        reloadRuntimeConfiguration();

        DiscordReportSender reportSender = new DiscordReportSender(this);
        BugCommand executor = new BugCommand(this, reportSender);

        int chatHistorySize = getConfig().getInt("chatHistorySize", 10);
        chatHistoryBuffer = new ChatHistoryBuffer(chatHistorySize);
        getServer().getPluginManager().registerEvents(chatHistoryBuffer, this);

        PluginCommand bugCommand = getCommand("bug");
        if (bugCommand == null) {
            getLogger().severe("Command /bug is missing from plugin.yml");
            return;
        }
        bugCommand.setExecutor(executor);
        bugCommand.setTabCompleter(executor);

        PluginCommand bugReportCommand = getCommand("bugreport");
        if (bugReportCommand == null) {
            getLogger().severe("Command /bugreport is missing from plugin.yml");
            return;
        }
        bugReportCommand.setExecutor(executor);
        bugReportCommand.setTabCompleter(executor);
    }

    private void scheduleUnlicensedWarning() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, () ->
            getLogger().warning("[BugReport] AuthAPI mode is UNLICENSED. Plugin will continue running."),
        0L, TICKS_PER_24H);
    }

    private void logInvalidLicenseAndDisable(String message) {
        String reason = (message == null || message.isBlank()) ? "License is invalid." : message;
        getLogger().severe("========================================");
        getLogger().severe("BUGREPORT LICENSE VALIDATION FAILED");
        getLogger().severe(reason);
        getLogger().severe("Plugin is shutting down.");
        getLogger().severe("========================================");
        getServer().getPluginManager().disablePlugin(this);
    }

    private void notifyUpdateAvailable(LicenseResult result) {
        String latestVersion = (result.latestVersion() == null || result.latestVersion().isBlank())
                ? "(unknown)"
                : result.latestVersion();
        String downloadUrl = (result.downloadUrl() == null) ? "" : result.downloadUrl();

        getLogger().warning("A new BugReport version is available: " + latestVersion);
        if (!downloadUrl.isBlank()) {
            getLogger().warning("Download: " + downloadUrl);
        }

        Component message = Component.text("[BugReport] Update available: ", NamedTextColor.GOLD)
                .append(Component.text(latestVersion, NamedTextColor.YELLOW));

        if (!downloadUrl.isBlank()) {
            message = message
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Open", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.openUrl(downloadUrl)))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Copy URL", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.suggestCommand(downloadUrl)));
        }

        final Component finalMessage = message;
        getServer().getScheduler().runTask(this, () -> {
            for (Player onlinePlayer : getServer().getOnlinePlayers()) {
                if (onlinePlayer.isOp()) {
                    onlinePlayer.sendMessage(finalMessage);
                }
            }
        });
    }

    public void reloadRuntimeConfiguration() {
        ensureConfigDefaultsMerged();
        reloadConfig();
        ensureEmbedTemplateDefaultsMerged();

        boolean includeRecentConsoleLine = getConfig().getBoolean("includeRecentConsoleLine", true);
        if (includeRecentConsoleLine && consoleLineBuffer == null) {
            consoleLineBuffer = new ConsoleLineBuffer(getServer().getLogger());
            consoleLineBuffer.attach();
        }

        if (!includeRecentConsoleLine && consoleLineBuffer != null) {
            consoleLineBuffer.detach();
            consoleLineBuffer = null;
        }
    }

    public ConsoleLineBuffer getConsoleLineBuffer() {
        return consoleLineBuffer;
    }

    public ChatHistoryBuffer getChatHistoryBuffer() {
        return chatHistoryBuffer;
    }

    public LicenseResult getLicenseResult() {
        return licenseResult;
    }

    public long getStartupStartedAtMillis() {
        return startupStartedAtMillis;
    }

    private void ensureDefaultEmbedTemplate() {
        File embedTemplate = new File(getDataFolder(), "discord-embed.yml");
        if (!embedTemplate.exists()) {
            saveResource("discord-embed.yml", false);
        }
    }

    private void ensureConfigDefaultsMerged() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private void ensureEmbedTemplateDefaultsMerged() {
        ensureDefaultEmbedTemplate();

        String templateFileName = getConfig().getString("embedTemplateFile", "discord-embed.yml");
        File templateFile = new File(getDataFolder(), templateFileName);
        if (!templateFile.exists()) {
            if ("discord-embed.yml".equals(templateFileName)) {
                saveResource("discord-embed.yml", false);
            } else {
                copyDefaultTemplateTo(templateFile);
            }
        }

        try (InputStream stream = getResource("discord-embed.yml")) {
            if (stream == null) {
                return;
            }

            YamlConfiguration current = YamlConfiguration.loadConfiguration(templateFile);
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            );

            if (mergeMissingKeys(current, defaults)) {
                current.save(templateFile);
            }
        } catch (Exception exception) {
            getLogger().warning("Failed to merge defaults into embed template: " + exception.getMessage());
        }
    }

    private void copyDefaultTemplateTo(File targetFile) {
        try {
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (InputStream stream = getResource("discord-embed.yml")) {
                if (stream == null) {
                    return;
                }
                java.nio.file.Files.copy(
                        stream,
                        targetFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception exception) {
            getLogger().warning("Failed to create embed template file: " + exception.getMessage());
        }
    }

    private boolean mergeMissingKeys(ConfigurationSection target, ConfigurationSection defaults) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
                changed = true;
                continue;
            }

            if (defaults.isConfigurationSection(key) && target.isConfigurationSection(key)) {
                ConfigurationSection targetSection = Objects.requireNonNull(target.getConfigurationSection(key));
                ConfigurationSection defaultSection = Objects.requireNonNull(defaults.getConfigurationSection(key));
                changed |= mergeMissingKeys(targetSection, defaultSection);
            }
        }
        return changed;
    }

    @Override
    public void onDisable() {
        if (consoleLineBuffer != null) {
            consoleLineBuffer.detach();
        }
    }
}