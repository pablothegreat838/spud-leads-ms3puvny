package com.example.leaderboards.util;

import com.example.leaderboards.LeaderboardsPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private final LeaderboardsPlugin plugin;
    private Economy economy = null;
    private boolean economyHooked = false;

    public VaultHook(LeaderboardsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        economyHooked = (economy != null);
        return economyHooked;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isEconomyHooked() {
        return economyHooked;
    }
}