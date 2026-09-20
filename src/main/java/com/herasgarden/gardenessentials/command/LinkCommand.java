package com.herasgarden.gardenessentials.command;

import com.herasgarden.gardenessentials.iris.MinecraftLinkService;
import com.herasgarden.gardenessentials.iris.MinecraftLinkService.LinkCode;
import com.herasgarden.gardenessentials.iris.MinecraftLinkService.LinkStatus;
import com.herasgarden.gardenessentials.ui.EssentialsMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LinkCommand implements CommandExecutor, TabCompleter {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("M/d/yyyy 'at' h:mm a", Locale.US);

    private final MinecraftLinkService links;

    public LinkCommand(MinecraftLinkService links) {
        this.links = links;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            EssentialsMessages.send(sender, "Minecraft account linking must be started in-game.");
            return true;
        }
        if (!player.hasPermission("gardenessentials.link")) {
            EssentialsMessages.send(player, "You do not have permission to link accounts.");
            return true;
        }

        try {
            if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
                return status(player);
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("unlink")) {
                return unlink(player);
            }

            Optional<LinkStatus> existing = links.status(player.getUniqueId());
            if (existing.isPresent()) {
                EssentialsMessages.send(player,
                        "Your Minecraft account is already linked to Discord account "
                                + existing.get().discordId() + ". Use /link unlink first to change it.");
                return true;
            }

            LinkCode code = links.issue(player);
            EssentialsMessages.send(player, "Your one-time Iris link code is " + code.code() + ".");
            EssentialsMessages.send(player,
                    "In Discord, run /minecraft-link connect and enter " + code.code()
                            + ". The code expires at " + formatTime(code.expiresAt()) + ".");
        } catch (SQLException exception) {
            EssentialsMessages.send(player, "Account linking could not be updated right now.");
        }
        return true;
    }

    private boolean status(Player player) throws SQLException {
        Optional<LinkStatus> status = links.status(player.getUniqueId());
        if (status.isEmpty()) {
            EssentialsMessages.send(player, "Your Minecraft account is not linked to Iris.");
            return true;
        }
        EssentialsMessages.send(player,
                "Linked to Discord account " + status.get().discordId()
                        + ". Linked at " + formatTime(status.get().linkedAt()) + ".");
        return true;
    }

    private String formatTime(long epochMillis) {
        return DISPLAY_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(DISPLAY_ZONE));
    }

    private boolean unlink(Player player) throws SQLException {
        EssentialsMessages.send(player, links.unlink(player.getUniqueId())
                ? "Your Minecraft account has been unlinked from Iris."
                : "Your Minecraft account was not linked.");
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("status", "unlink").stream()
                .filter(value -> value.startsWith(prefix))
                .toList();
    }
}
