package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LicenseValidator {

    private static final String APP_NAME = "BugReport";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern STRING_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\\\"])*)\"");
    private static final Pattern BOOLEAN_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*(true|false)");

    private final Main plugin;
    private final HttpClient httpClient;

    public LicenseValidator(Main plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public LicenseResult validate() {
        String apiUrl = trim(plugin.getConfig().getString("license.apiUrl", ""));
        String licenseKey = trim(plugin.getConfig().getString("license.licenseKey", ""));
        String currentVersion = trim(plugin.getConfig().getString("license.currentVersion", ""));

        if (apiUrl.isBlank() || licenseKey.isBlank() || currentVersion.isBlank()) {
            return LicenseResult.unreachable("Missing one or more license config values (apiUrl/licenseKey/currentVersion).");
        }

        URI uri;
        try {
            uri = new URI(normalizeBaseUrl(apiUrl) + "/api/license/validate");
        } catch (URISyntaxException exception) {
            return LicenseResult.unreachable("Invalid license API URL in config: " + exception.getMessage());
        }

        String requestJson = "{"
                + "\"licenseKey\":\"" + escapeJson(licenseKey) + "\","
                + "\"appName\":\"" + escapeJson(APP_NAME) + "\","
                + "\"currentVersion\":\"" + escapeJson(currentVersion) + "\""
                + "}";

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

    private LicenseResult parseResponse(String json) {
        boolean isValid = readBoolean(json, "isValid", false);
        String message = readString(json, "message", "");
        boolean isUpdateAvailable = readBoolean(json, "isUpdateAvailable", false);
        String latestVersion = readString(json, "latestVersion", "");
        String downloadUrl = readString(json, "downloadUrl", "");
        String expirationDate = readString(json, "expirationDate", "");

        return LicenseResult.reachable(
                isValid,
                message,
                isUpdateAvailable,
                latestVersion,
                downloadUrl,
                expirationDate
        );
    }

    private boolean readBoolean(String json, String field, boolean fallback) {
        Pattern pattern = Pattern.compile(String.format(BOOLEAN_PATTERN_TEMPLATE.pattern(), Pattern.quote(field)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private String readString(String json, String field, String fallback) {
        Pattern pattern = Pattern.compile(String.format(STRING_PATTERN_TEMPLATE.pattern(), Pattern.quote(field)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return fallback;
        }
        return unescapeJson(matcher.group(1));
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
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '\\' || i + 1 >= value.length()) {
                output.append(current);
                continue;
            }

            char next = value.charAt(++i);
            switch (next) {
                case '"' -> output.append('"');
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

