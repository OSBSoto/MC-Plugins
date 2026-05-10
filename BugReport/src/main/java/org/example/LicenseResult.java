package org.example;

public record LicenseResult(
        boolean reachable,
        boolean isValid,
        String message,
        boolean isUpdateAvailable,
        String latestVersion,
        String downloadUrl,
        String expirationDate
) {
    public static LicenseResult unreachable(String message) {
        return new LicenseResult(false, true, message, false, "", "", "");
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
                isValid,
                message == null ? "" : message,
                isUpdateAvailable,
                latestVersion == null ? "" : latestVersion,
                downloadUrl == null ? "" : downloadUrl,
                expirationDate == null ? "" : expirationDate
        );
    }
}

