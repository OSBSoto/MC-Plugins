package org.example;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BugReportPayload(
        Instant timestamp,
        UUID playerUuid,
        String username,
        String world,
        double x,
        double y,
        double z,
        String message,
        String recentConsoleLine,
        List<String> recentChatLines
) {
}

