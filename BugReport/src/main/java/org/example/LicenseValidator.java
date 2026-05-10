package org.example;

import org.bukkit.World;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LicenseValidator {

    private static final String APP_NAME = "BugReport";
    private static final String API_URL = "https://authapi.rhynohost.com";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration PUBLIC_IP_TIMEOUT = Duration.ofSeconds(2);

    private final Main plugin;
    private final HttpClient httpClient;

    public LicenseValidator(Main plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public LicenseResult validate() {
        String licenseKey = trim(plugin.getConfig().getString("license.licenseKey", ""));
        String currentVersion = plugin.getDescription().getVersion();

        URI uri;
        try {
            uri = new URI(normalizeBaseUrl(API_URL) + "/api/license/validate");
        } catch (URISyntaxException exception) {
            return LicenseResult.unreachable("Invalid internal license API URL: " + exception.getMessage());
        }

        ValidateRequest requestPayload = buildRequest(licenseKey, currentVersion);
        String requestJson = toJson(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return LicenseResult.unreachable("License API returned HTTP " + response.statusCode() + ".");
            }
            return parseResponse(response.body());
        } catch (IOException exception) {
            return LicenseResult.unreachable("Network error: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return LicenseResult.unreachable("Validation request interrupted.");
        } catch (RuntimeException exception) {
            return LicenseResult.unreachable("Failed to parse validation response: " + exception.getMessage());
        }
    }

    private ValidateRequest buildRequest(String licenseKey, String currentVersion) {
        long startupTimeMs = Math.max(0L, System.currentTimeMillis() - plugin.getStartupStartedAtMillis());
        return new ValidateRequest(
                licenseKey,
                APP_NAME,
                currentVersion,
                resolveServerName(),
                resolvePublicIp(),
                System.getProperty("os.name", "unknown"),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory() / (1024L * 1024L),
                Locale.getDefault().toString(),
                startupTimeMs,
                collectWorlds()
        );
    }

    private List<ValidateRequest.WorldInfo> collectWorlds() {
        List<ValidateRequest.WorldInfo> worlds = new ArrayList<>();
        for (World world : plugin.getServer().getWorlds()) {
            worlds.add(new ValidateRequest.WorldInfo(
                    world.getName(),
                    world.getEnvironment().name(),
                    world.getPlayers().size()
            ));
        }
        return worlds;
    }

    private LicenseResult parseResponse(String json) {
        boolean isValid = readBoolean(json, "isValid", false);
        String message = readNullableString(json, "message", "");
        String mode = readNullableString(json, "mode", isValid ? "licensed" : "blocked");
        String blockReason = readNullableString(json, "blockReason", "");
        boolean isUpdateAvailable = readBoolean(json, "isUpdateAvailable", false);
        String latestVersion = readNullableString(json, "latestVersion", "");
        String downloadUrl = readNullableString(json, "downloadUrl", "");
        String expirationDate = readNullableString(json, "expirationDate", "");

        return LicenseResult.reachable(
                isValid,
                message,
                mode,
                blockReason,
                isUpdateAvailable,
                latestVersion,
                downloadUrl,
                expirationDate
        );
    }

    private boolean readBoolean(String json, String field, boolean fallback) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(true|false)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private String readNullableString(String json, String field, String fallback) {
        Pattern pattern = Pattern.compile(
                "\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(\\\"((?:\\\\\\\\.|[^\\\"\\\\\\\\])*)\\\"|null)"
        );
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        String rawValue = matcher.group(1);
        if ("null".equals(rawValue)) {
            return "";
        }
        return unescapeJson(matcher.group(2));
    }

    private String resolveServerName() {
        String serverIp = trim(plugin.getServer().getIp());
        if (!serverIp.isBlank()) {
            return serverIp;
        }

        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception exception) {
            return "unknown";
        }
    }

    private String resolvePublicIp() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ipify.org"))
                    .timeout(PUBLIC_IP_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return trim(response.body());
            }
        } catch (Exception ignored) {
            // Best effort only; fail open with blank IP.
        }

        String fallbackIp = trim(plugin.getServer().getIp());
        return fallbackIp;
    }

    private String toJson(ValidateRequest request) {
        StringBuilder worldsJson = new StringBuilder("[");
        List<ValidateRequest.WorldInfo> worlds = request.worlds();
        for (int i = 0; i < worlds.size(); i++) {
            ValidateRequest.WorldInfo world = worlds.get(i);
            if (i > 0) {
                worldsJson.append(',');
            }
            worldsJson.append('{')
                    .append("\"worldName\":\"").append(escapeJson(world.worldName())).append("\",")
                    .append("\"worldType\":\"").append(escapeJson(world.worldType())).append("\",")
                    .append("\"playerCount\":").append(world.playerCount())
                    .append('}');
        }
        worldsJson.append(']');

        return "{"
                + "\"licenseKey\":\"" + escapeJson(request.licenseKey()) + "\","
                + "\"appName\":\"" + escapeJson(request.appName()) + "\","
                + "\"currentVersion\":\"" + escapeJson(request.currentVersion()) + "\","
                + "\"serverName\":\"" + escapeJson(request.serverName()) + "\","
                + "\"publicIp\":\"" + escapeJson(request.publicIp()) + "\","
                + "\"osName\":\"" + escapeJson(request.osName()) + "\","
                + "\"cpuCores\":" + request.cpuCores() + ","
                + "\"ramMb\":" + request.ramMb() + ","
                + "\"locale\":\"" + escapeJson(request.locale()) + "\","
                + "\"startupTimeMs\":" + request.startupTimeMs() + ","
                + "\"worlds\":" + worldsJson
                + "}";
    }

    private String normalizeBaseUrl(String apiUrl) {
        if (apiUrl.endsWith("/")) {
            return apiUrl.substring(0, apiUrl.length() - 1);
        }
        return apiUrl;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '\\' || i + 1 >= value.length()) {
                output.append(current);
                continue;
            }

            char next = value.charAt(++i);
            switch (next) {
                case '\"' -> output.append('\"');
                case '\\' -> output.append('\\');
                case '/' -> output.append('/');
                case 'b' -> output.append('\b');
                case 'f' -> output.append('\f');
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                case 't' -> output.append('\t');
                case 'u' -> {
                    if (i + 4 >= value.length()) {
                        throw new IllegalArgumentException("Invalid unicode escape sequence in JSON string.");
                    }
                    String hex = value.substring(i + 1, i + 5);
                    output.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                }
                default -> output.append(next);
            }
        }
        return output.toString();
    }
}
