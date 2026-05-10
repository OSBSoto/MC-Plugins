package org.example;

public record LicenseResult(
        boolean reachable,
        boolean isValid,
        String message,
        String mode,
        String blockReason,
        boolean isUpdateAvailable,
        String latestVersion,
        String downloadUrl,
        String expirationDate
) {
    public static LicenseResult unreachable(String message) {
        return new LicenseResult(false, true, message, "unreachable", "", false, "", "", "");
    }

    public static LicenseResult reachable(
            boolean isValid,
            String message,
            String mode,
            String blockReason,
            boolean isUpdateAvailable,
            String latestVersion,
            String downloadUrl,
            String expirationDate
    ) {
        return new LicenseResult(
                true,
                isValid,
                message == null ? "" : message,
                mode == null ? "" : mode,
                blockReason == null ? "" : blockReason,
                isUpdateAvailable,
                latestVersion == null ? "" : latestVersion,
                downloadUrl == null ? "" : downloadUrl,
                expirationDate == null ? "" : expirationDate
        );
    }

    public boolean isBlockedMode() {
        return "blocked".equalsIgnoreCase(mode);
    }

    public boolean isUnlicensedMode() {
        return "unlicensed".equalsIgnoreCase(mode);
    }
}
