package com.herasgarden.gardenessentials.command;

import com.herasgarden.gardencore.api.GardenPlatform;
import com.herasgarden.gardencore.api.integration.IntegrationEventType;
import com.herasgarden.gardenessentials.ui.EssentialsMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ReportCommand implements CommandExecutor, TabCompleter {
    private final GardenPlatform platform;

    public ReportCommand(GardenPlatform platform) {
        this.platform = platform;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            EssentialsMessages.send(sender, "Reports must be submitted in-game.");
            return true;
        }
        if (!player.hasPermission("gardenessentials.report")) {
            EssentialsMessages.send(player, "You do not have permission to submit reports.");
            return true;
        }
        if (args.length < 2) {
            help(player);
            return true;
        }

        try {
            if (args[0].equalsIgnoreCase("bug")) {
                String description = clean(String.join(" ", Arrays.copyOfRange(args, 1, args.length)), 1800);
                if (description.length() < 5) {
                    EssentialsMessages.send(player, "Please describe what went wrong.");
                    return true;
                }
                platform.integrations().publish(
                        IntegrationEventType.BUG_REPORT_SUBMITTED,
                        "player",
                        player.getUniqueId().toString(),
                        payloadBase(player)
                                + ",\"description\":\"" + json(description) + "\"}"
                );
                EssentialsMessages.send(player, "Bug report sent to the team through Iris. Thank you.");
                return true;
            }

            if (args[0].equalsIgnoreCase("player")) {
                if (args.length < 3) {
                    EssentialsMessages.send(player, "Use /report player <player> <reason>.");
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String reason = clean(String.join(" ", Arrays.copyOfRange(args, 2, args.length)), 1800);
                if (reason.length() < 3) {
                    EssentialsMessages.send(player, "Please include a reason for the report.");
                    return true;
                }
                platform.integrations().publish(
                        IntegrationEventType.PLAYER_REPORT_SUBMITTED,
                        "player",
                        target.getUniqueId().toString(),
                        payloadBase(player)
                                + ",\"reportedPlayerUuid\":\"" + target.getUniqueId() + "\""
                                + ",\"reportedPlayerName\":\"" + json(target.getName() == null ? args[1] : target.getName()) + "\""
                                + ",\"reason\":\"" + json(reason) + "\"}"
                );
                EssentialsMessages.send(player, "Player report sent privately to the staff team through Iris.");
                return true;
            }

            help(player);
        } catch (SQLException exception) {
            EssentialsMessages.send(player, "Your report could not be saved right now. Please try again shortly.");
        }
        return true;
    }

    private String payloadBase(Player player) {
        return "{\"reporterUuid\":\"" + player.getUniqueId() + "\""
                + ",\"reporterName\":\"" + json(player.getName()) + "\""
                + ",\"world\":\"" + json(player.getWorld().getName()) + "\""
                + ",\"x\":" + player.getLocation().getBlockX()
                + ",\"y\":" + player.getLocation().getBlockY()
                + ",\"z\":" + player.getLocation().getBlockZ();
    }

    private void help(Player player) {
        EssentialsMessages.send(player, "Use /report bug <description> or /report player <player> <reason>.");
    }

    private String clean(String value, int max) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private String json(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("bug", "player").stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
