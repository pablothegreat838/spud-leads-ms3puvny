package com.example.leaderboards.stats;

public enum LeaderboardType {
    KILLS("Kills"),
    MONEY("Money"),
    BLOCK_PLACE("Blocks Placed"),
    CHATSENT("Messages Sent");

    private final String displayName;

    LeaderboardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LeaderboardType fromString(String text) {
        for (LeaderboardType type : LeaderboardType.values()) {
            if (type.name().equalsIgnoreCase(text) || type.getDisplayName().equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }
}