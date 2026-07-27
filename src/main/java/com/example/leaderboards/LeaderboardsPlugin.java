package com.example.leaderboards;

import com.example.leaderboards.commands.SetLeaderCommand;
import com.example.leaderboards.listeners.PlayerStatsListener;
import com.example.leaderboards.stats.PlayerStatsManager;
import com.example.leaderboards.stats.LeaderboardType;
import com.example.leaderboards.util.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeaderboardsPlugin extends JavaPlugin {

    private PlayerStatsManager statsManager;
    private VaultHook vaultHook;
    private Map<Location, LeaderboardType> leaderboardSigns;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.statsManager = new PlayerStatsManager(this);
        this.vaultHook = new VaultHook(this);
        this.leaderboardSigns = new HashMap<>();

        if (vaultHook.setupEconomy()) {
            getLogger().info("Vault economy hooked successfully.");
        } else {
            getLogger().warning("Vault economy not found. Money leaderboards will not function.");
        }

        loadLeaderboardSigns();

        getServer().getPluginManager().registerEvents(new PlayerStatsListener(this, statsManager), this);
        getCommand("setleader").setExecutor(new SetLeaderCommand(this));

        // Schedule repeating task to update leaderboards
        Bukkit.getScheduler().runTaskTimer(this, this::updateAllLeaderboardSigns, 0L, 20L * 60); // Every 1 minute
    }

    @Override
    public void onDisable() {
        saveLeaderboardSigns();
        statsManager.saveAllStats();
    }

    public PlayerStatsManager getStatsManager() {
        return statsManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public Map<Location, LeaderboardType> getLeaderboardSigns() {
        return leaderboardSigns;
    }

    private void loadLeaderboardSigns() {
        FileConfiguration config = getConfig();
        if (config.contains("leaderboards.signs")) {
            for (String key : config.getConfigurationSection("leaderboards.signs").getKeys(false)) {
                String worldName = config.getString("leaderboards.signs." + key + ".world");
                double x = config.getDouble("leaderboards.signs." + key + ".x");
                double y = config.getDouble("leaderboards.signs." + key + ".y");
                double z = config.getDouble("leaderboards.signs." + key + ".z");
                String typeString = config.getString("leaderboards.signs." + key + ".type");

                if (worldName != null && typeString != null) {
                    LeaderboardType type = LeaderboardType.fromString(typeString);
                    if (type != null && Bukkit.getWorld(worldName) != null) {
                        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
                        leaderboardSigns.put(loc, type);
                    } else {
                        getLogger().warning("Could not load leaderboard sign at " + worldName + "," + x + "," + y + "," + z + " with type " + typeString);
                    }
                }
            }
        }
    }

    private void saveLeaderboardSigns() {
        FileConfiguration config = getConfig();
        config.set("leaderboards.signs", null); // Clear old signs
        int i = 0;
        for (Map.Entry<Location, LeaderboardType> entry : leaderboardSigns.entrySet()) {
            Location loc = entry.getKey();
            LeaderboardType type = entry.getValue();
            String path = "leaderboards.signs." + i;
            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".type", type.name().toLowerCase());
            i++;
        }
        saveConfig();
    }

    public void updateLeaderboardSign(Location loc, LeaderboardType type) {
        if (loc.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) loc.getBlock().getState();
            sign.setLine(0, ChatColor.GOLD + "--- " + type.getDisplayName() + " ---");
            sign.setLine(1, ""); // Clear line 1 for top player
            sign.setLine(2, ""); // Clear line 2 for value
            sign.setLine(3, ""); // Clear line 3 for rank 2

            Map<UUID, Long> topStats = statsManager.getTopStats(type);

            if (topStats.isEmpty()) {
                sign.setLine(1, ChatColor.GRAY + "No data yet!");
            } else {
                int line = 1;
                for (Map.Entry<UUID, Long> entry : topStats.entrySet()) {
                    if (line > 3) break; // Only show top 3 on a 4-line sign
                    String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (playerName == null) playerName = "Unknown";

                    String value = String.valueOf(entry.getValue());
                    if (type == LeaderboardType.MONEY && vaultHook.getEconomy() != null) {
                        value = vaultHook.getEconomy().format(entry.getValue().doubleValue());
                    }

                    if (line == 1) {
                        sign.setLine(line, ChatColor.AQUA + "1. " + playerName);
                        sign.setLine(line + 1, ChatColor.WHITE + "   " + value);
                        line += 2; // Move to line 3 for the next entry
                    } else { // For the second entry (rank 2)
                        sign.setLine(line, ChatColor.GREEN + "2. " + playerName + ": " + value);
                        line++;
                    }
                }
            }
            sign.update();
        } else {
            // If the block is no longer a sign, remove it from the list
            getLogger().warning("Leaderboard sign at " + loc.toString() + " is no longer a sign. Removing.");
            leaderboardSigns.remove(loc);
            saveLeaderboardSigns(); // Resave config without the invalid sign
        }
    }

    public void updateAllLeaderboardSigns() {
        for (Map.Entry<Location, LeaderboardType> entry : leaderboardSigns.entrySet()) {
            updateLeaderboardSign(entry.getKey(), entry.getValue());
        }
    }
}