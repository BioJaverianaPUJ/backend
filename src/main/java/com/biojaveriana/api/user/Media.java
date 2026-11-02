package com.biojaveriana.api.user;

import java.util.UUID;

public enum Media {
    PROFILE_PICTURE("profile-picture"),
    BANNER("banner");

    private final String fieldName;

    Media(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldKey(User user) {
        if (this == PROFILE_PICTURE) {
            return user.getProfilePictureKey();
        }
        return user.getBannerKey();
    }

    public void setKey(User user, String key) {
        if (this == PROFILE_PICTURE) {
            user.setProfilePictureKey(key);
        } else {
            user.setBannerKey(key);
        }
    }

    public String generateKey(User user, String contentType) {
        String ext = contentTypeToExtension(contentType);
        return String.format("users/%s/%s/%s%s", user.getId(), fieldName, UUID.randomUUID().toString(), ext);
    }

    private static String contentTypeToExtension(String contentType) {
        if (contentType == null) return "";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            default           -> "";
        };
    }
}
