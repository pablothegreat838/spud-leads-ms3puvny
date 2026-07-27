package com.example.leaderboards.commands;

import com.example.leaderboards.LeaderboardsPlugin;
import com.example.leaderboards.stats.LeaderboardType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLeaderCommand implements CommandExecutor {

    private final LeaderboardsPlugin plugin;

    public SetLeaderCommand(LeaderboardsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /setleader <type>");
            player.sendMessage(ChatColor.YELLOW + "Available types: kills, money, block-place, chatsent");
            return true;
        }

        LeaderboardType type = LeaderboardType.fromString(args[0]);
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Invalid leaderboard type: " + args[0]);
            player.sendMessage(ChatColor.YELLOW + "Available types: kills, money, block-place, chatsent");
            return true;
        }

        if (type == LeaderboardType.MONEY && !plugin.getVaultHook().isEconomyHooked()) {
            player.sendMessage(ChatColor.RED + "Money leaderboards require Vault and an economy plugin to be installed.");
            return true;
        }

        Block targetBlock = player.getTargetBlock(null, 5); // Get block player is looking at within 5 blocks
        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
            player.sendMessage(ChatColor.RED + "You must be looking at a sign to set a leaderboard.");
            return true;
        }

        Location signLoc = targetBlock.getLocation();
        plugin.getLeaderboardSigns().put(signLoc, type);
        plugin.saveConfig(); // Save the new sign location to config

        player.sendMessage(ChatColor.GREEN + "Leaderboard sign for " + type.getDisplayName() + " set successfully!");
        plugin.updateLeaderboardSign(signLoc, type); // Immediately update the sign

        return true;
    }
}