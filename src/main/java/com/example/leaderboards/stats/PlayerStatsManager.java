package com.example.leaderboards.stats;

import com.example.leaderboards.LeaderboardsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerStatsManager {

    private final LeaderboardsPlugin plugin;
    private final Map<UUID, Map<LeaderboardType, Long>> playerStats;
    private final File statsFile;
    private FileConfiguration statsConfig;

    public PlayerStatsManager(LeaderboardsPlugin plugin) {
        this.plugin = plugin;
        this.playerStats = new ConcurrentHashMap<>();
        this.statsFile = new File(plugin.getDataFolder(), "playerstats.yml");
        loadStatsFromFile();
    }

    private void loadStatsFromFile() {
        if (!statsFile.exists()) {
            plugin.saveResource("playerstats.yml", false);
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);

        if (statsConfig.contains("players")) {
            for (String uuidString : statsConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    Map<LeaderboardType, Long> stats = new ConcurrentHashMap<>();
                    for (LeaderboardType type : LeaderboardType.values()) {
                        long value = statsConfig.getLong("players." + uuidString + "." + type.name().toLowerCase(), 0);
                        stats.put(type, value);
                    }
                    playerStats.put(uuid, stats);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID found in playerstats.yml: " + uuidString);
                }
            }
        }
    }

    public void saveAllStats() {
        for (Map.Entry<UUID, Map<LeaderboardType, Long>> entry : playerStats.entrySet()) {
            String uuidString = entry.getKey().toString();
            for (Map.Entry<LeaderboardType, Long> statEntry : entry.getValue().entrySet()) {
                statsConfig.set("players." + uuidString + "." + statEntry.getKey().name().toLowerCase(), statEntry.getValue());
            }
        }
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player stats to playerstats.yml: " + e.getMessage());
        }
    }

    public void loadPlayerStats(UUID playerUuid) {
        // If player is not in cache, load from file (if not already loaded)
        // This is mostly for initial load, subsequent updates are in-memory
        if (!playerStats.containsKey(playerUuid)) {
            Map<LeaderboardType, Long> stats = new ConcurrentHashMap<>();
            String uuidString = playerUuid.toString();
            for (LeaderboardType type : LeaderboardType.values()) {
                long value = statsConfig.getLong("players." + uuidString + "." + type.name().toLowerCase(), 0);
                stats.put(type, value);
            }
            playerStats.put(playerUuid, stats);
        }
    }

    public long getStat(UUID playerUuid, LeaderboardType type) {
        return playerStats.getOrDefault(playerUuid, Collections.emptyMap()).getOrDefault(type, 0L);
    }

    public void incrementStat(UUID playerUuid, LeaderboardType type, long amount) {
        playerStats.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                .merge(type, amount, (oldValue, newValue) -> oldValue + newValue);
        // Save immediately for critical stats, or rely on periodic saveAllStats
        // For this plugin, we'll rely on saveAllStats on disable and periodic updates for signs.
    }

    public Map<UUID, Long> getTopStats(LeaderboardType type) {
        // For money, we need to get current balance from Vault
        if (type == LeaderboardType.MONEY && plugin.getVaultHook().isEconomyHooked()) {
            Map<UUID, Long> moneyStats = new HashMap<>();
            for (UUID uuid : playerStats.keySet()) { // Iterate over all known players
                double balance = plugin.getVaultHook().getEconomy().getBalance(plugin.getServer().getOfflinePlayer(uuid));
                moneyStats.put(uuid, (long) balance); // Convert double to long for consistency
            }
            return moneyStats.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                    .limit(3)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else {
            return playerStats.entrySet().stream()
                    .filter(entry -> entry.getValue().containsKey(type))
                    .sorted(Map.Entry.<UUID, Map<LeaderboardType, Long>>comparingByValue(
                            (map1, map2) -> Long.compare(map1.getOrDefault(type, 0L), map2.getOrDefault(type, 0L))
                    ).reversed())
                    .limit(3)
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(type), (e1, e2) -> e1, LinkedHashMap::new));
        }
    }
}