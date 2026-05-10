package org.example;

public record LicenseResult(
        boolean reachable,
        boolean isUnlicensed,
        boolean isValid,
        String message,
        boolean isUpdateAvailable,
        String latestVersion,
        String downloadUrl,
        String expirationDate
) {
    public static LicenseResult unlicensed() {
        return new LicenseResult(true, true, true, "No license key configured.", false, "", "", "");
    }

    public static LicenseResult unreachable(String message) {
        return new LicenseResult(false, false, true, message, false, "", "", "");
    }

    public static LicenseResult reachable(
            boolean isValid,
            String message,
            boolean isUpdateAvailable,
            String latestVersion,
            String downloadUrl,
            String expirationDate
    ) {
        return new LicenseResult(
                true,
                false,
                isValid,
                message == null ? "" : message,
                isUpdateAvailable,
                latestVersion == null ? "" : latestVersion,
                downloadUrl == null ? "" : downloadUrl,
                expirationDate == null ? "" : expirationDate
        );
    }
}
