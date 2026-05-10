package org.example;

import java.util.List;

public record ValidateRequest(
        String licenseKey,
        String appName,
        String currentVersion,
        String serverName,
        String publicIp,
        String osName,
        int cpuCores,
        long ramMb,
        String locale,
        long startupTimeMs,
        List<WorldInfo> worlds
) {
    public record WorldInfo(
            String worldName,
            String worldType,
            int playerCount
    ) {
    }
}

