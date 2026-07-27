package com.example.leaderboards.listeners;

import com.example.leaderboards.LeaderboardsPlugin;
import com.example.leaderboards.stats.LeaderboardType;
import com.example.leaderboards.stats.PlayerStatsManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerStatsListener implements Listener {

    private final LeaderboardsPlugin plugin;
    private final PlayerStatsManager statsManager;

    public PlayerStatsListener(LeaderboardsPlugin plugin, PlayerStatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Ensure stats are loaded for the player
        statsManager.loadPlayerStats(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            statsManager.incrementStat(killer.getUniqueId(), LeaderboardType.KILLS, 1);
            plugin.updateAllLeaderboardSigns();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        statsManager.incrementStat(player.getUniqueId(), LeaderboardType.BLOCK_PLACE, 1);
        plugin.updateAllLeaderboardSigns();
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        statsManager.incrementStat(player.getUniqueId(), LeaderboardType.CHATSENT, 1);
        // Chat events are async, so schedule sync update for signs
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.updateAllLeaderboardSigns());
    }
}