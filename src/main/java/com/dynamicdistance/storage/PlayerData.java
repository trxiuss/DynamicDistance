package com.dynamicdistance.storage;

import com.dynamicdistance.util.TimeUtil;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private Integer customViewDistance;
    private Integer customSimDistance;
    private Long expiryTimestamp;
    private boolean statusBarEnabled = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Integer getCustomViewDistance() {
        if (isOverrideExpired()) {
            clearOverride();
            return null;
        }
        return customViewDistance;
    }

    public void setCustomViewDistance(Integer customViewDistance) {
        this.customViewDistance = customViewDistance;
    }

    public Integer getCustomSimDistance() {
        if (isOverrideExpired()) {
            clearOverride();
            return null;
        }
        return customSimDistance;
    }

    public void setCustomSimDistance(Integer customSimDistance) {
        this.customSimDistance = customSimDistance;
    }

    public Long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public void setExpiryTimestamp(Long expiryTimestamp) {
        this.expiryTimestamp = expiryTimestamp;
    }

    public boolean isStatusBarEnabled() {
        return statusBarEnabled;
    }

    public void setStatusBarEnabled(boolean statusBarEnabled) {
        this.statusBarEnabled = statusBarEnabled;
    }

    public boolean isOverrideExpired() {
        return expiryTimestamp != null && System.currentTimeMillis() > expiryTimestamp;
    }

    public void clearOverride() {
        this.customViewDistance = null;
        this.customSimDistance = null;
        this.expiryTimestamp = null;
    }

    public boolean hasOverride() {
        if (isOverrideExpired()) {
            clearOverride();
            return false;
        }
        return customViewDistance != null || customSimDistance != null;
    }

    public boolean isTimedOverride() {
        return hasOverride() && expiryTimestamp != null;
    }

    public String getFormattedRemainingTime() {
        if (expiryTimestamp == null) {
            return null;
        }
        long rem = expiryTimestamp - System.currentTimeMillis();
        return TimeUtil.formatRemainingTime(rem);
    }

    public boolean hasPersistedData() {
        return hasOverride() || statusBarEnabled;
    }
}