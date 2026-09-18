package com.herasgarden.gardenessentials.command;

import com.herasgarden.gardencore.api.GardenPlatform;
import com.herasgarden.gardenessentials.location.LocationService;
import com.herasgarden.gardenessentials.social.TeleportRequestService;
import com.herasgarden.gardenessentials.ui.EssentialsMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UtilityCommand implements CommandExecutor, TabCompleter {
    private final GardenPlatform platform;
    private final LocationService locations;
    private final TeleportRequestService requests;
    private final Map<UUID, UUID> replyPartners = new ConcurrentHashMap<>();
    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();
    private final Set<UUID> afk = ConcurrentHashMap.newKeySet();

    public UtilityCommand(GardenPlatform platform, LocationService locations,
                          TeleportRequestService requests) {
        this.platform = platform;
        this.locations = locations;
        this.requests = requests;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "balance" -> balance(sender);
            case "pay" -> pay(sender, args);
            case "spawn" -> spawn(sender);
            case "setspawn" -> setSpawn(sender);
            case "warp" -> warp(sender, args);
            case "warps" -> warps(sender);
            case "setwarp" -> setWarp(sender, args);
            case "delwarp" -> delWarp(sender, args);
            case "tpa" -> tpa(sender, args);
            case "tpaccept" -> tpAccept(sender);
            case "tpdeny" -> tpDeny(sender);
            case "msg" -> message(sender, args);
            case "reply" -> reply(sender, args);
            case "back" -> back(sender);
            case "afk" -> afk(sender);
            case "heal" -> heal(sender, args);
            case "feed" -> feed(sender, args);
            case "fly" -> fly(sender, args);
            default -> false;
        };
    }

    private boolean balance(CommandSender sender) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.balance")) return true;
        long amount = platform.currency().balance(player.getUniqueId());
        EssentialsMessages.send(player, "Balance: " + platform.currency().symbol() + " " + amount);
        return true;
    }

    private boolean pay(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.pay")) return true;
        if (args.length < 2) {
            EssentialsMessages.send(player, "Use /pay <player> <amount>.");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            EssentialsMessages.send(player, "You cannot pay yourself.");
            return true;
        }
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException exception) {
            amount = -1L;
        }
        if (amount <= 0) {
            EssentialsMessages.send(player, "Payment must be a positive whole number of Obols.");
            return true;
        }
        if (!platform.currency().withdraw(player.getUniqueId(), amount)) {
            EssentialsMessages.send(player, "You do not have enough Obols.");
            return true;
        }
        if (!platform.currency().deposit(target.getUniqueId(), amount)) {
            platform.currency().deposit(player.getUniqueId(), amount);
            EssentialsMessages.send(player, "The payment could not be completed. Your Obols were returned.");
            return true;
        }
        EssentialsMessages.send(player, "Paid " + target.getName() + " " + platform.currency().symbol() + " " + amount + ".");
        if (target.isOnline() && target.getPlayer() != null) {
            EssentialsMessages.send(target.getPlayer(), player.getName() + " paid you "
                    + platform.currency().symbol() + " " + amount + ".");
        }
        return true;
    }

    private boolean spawn(CommandSender sender) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.spawn")) return true;
        try {
            Optional<Location> spawn = locations.spawn();
            if (spawn.isEmpty()) {
                EssentialsMessages.send(player, "Server spawn has not been set yet.");
                return true;
            }
            teleport(player, spawn.get());
        } catch (SQLException exception) {
            EssentialsMessages.send(player, "Spawn could not be loaded right now.");
        }
        return true;
    }

    private boolean setSpawn(CommandSender sender) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.setspawn")) return true;
        try {
            locations.setSpawn(player.getUniqueId(), player.getLocation());
            EssentialsMessages.send(player, "Server spawn set.");
        } catch (SQLException exception) {
            EssentialsMessages.send(player, "Server spawn could not be saved.");
        }
        return true;
    }

    private boolean warp(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.warp")) return true;
        if (args.length == 0) {
            return warps(sender);
        }
        try {
            Optional<Location> location = locations.warp(args[0]);
            if (location.isEmpty()) {
                EssentialsMessages.send(player, "That warp does not exist.");
                return true;
            }
            teleport(player, location.get());
        } catch (SQLException exception) {
            EssentialsMessages.send(player, "That warp could not be loaded.");
        }
        return true;
    }

    private boolean warps(CommandSender sender) {
        if (!sender.hasPermission("gardenessentials.warp")) {
            EssentialsMessages.send(sender, "You do not have permission to use warps.");
            return true;
        }
        try {
            List<String> names = locations.warps();
            EssentialsMessages.send(sender, names.isEmpty() ? "There are no server warps."
                    : "Warps: " + String.join(", ", names));
        } catch (SQLException exception) {
            EssentialsMessages.send(sender, "Warps could not be loaded.");
        }
        return true;
    }

    private boolean setWarp(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.warp.admin")) return true;
        if (args.length == 0) {
            EssentialsMessages.send(player, "Use /setwarp <name>.");
            return true;
        }
        try {
            locations.setWarp(player.getUniqueId(), args[0], player.getLocation());
            EssentialsMessages.send(player, "Warp " + args[0] + " set.");
        } catch (IllegalArgumentException exception) {
            EssentialsMessages.send(player, exception.getMessage());
        } catch (SQLException exception) {
            EssentialsMessages.send(player, "That warp could not be saved.");
        }
        return true;
    }

    private boolean delWarp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gardenessentials.warp.admin")) {
            EssentialsMessages.send(sender, "You do not have permission to remove warps.");
            return true;
        }
        if (args.length == 0) {
            EssentialsMessages.send(sender, "Use /delwarp <name>.");
            return true;
        }
        try {
            EssentialsMessages.send(sender, locations.deleteWarp(args[0])
                    ? "Deleted warp " + args[0] + "." : "That warp does not exist.");
        } catch (SQLException exception) {
            EssentialsMessages.send(sender, "That warp could not be deleted.");
        }
        return true;
    }

    private boolean tpa(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.tpa")) return true;
        if (args.length == 0) {
            EssentialsMessages.send(player, "Use /tpa <player>.");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            EssentialsMessages.send(player, "That player is not online.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            EssentialsMessages.send(player, "You are already here.");
            return true;
        }
        requests.request(player.getUniqueId(), target.getUniqueId());
        EssentialsMessages.send(player, "Teleport request sent to " + target.getName() + ".");
        target.sendMessage(EssentialsMessages.prefix()
                .append(Component.text(player.getName() + " wants to teleport to you. ", NamedTextColor.WHITE))
                .append(Component.text("[Accept]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/tpaccept")))
                .append(Component.space())
                .append(Component.text("[Deny]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/tpdeny"))));
        return true;
    }

    private boolean tpAccept(CommandSender sender) {
        Player target = player(sender);
        if (target == null || !allowed(target, "gardenessentials.tpa")) return true;
        Optional<UUID> requesterId = requests.take(target.getUniqueId());
        if (requesterId.isEmpty()) {
            EssentialsMessages.send(target, "You do not have a pending teleport request.");
            return true;
        }
        Player requester = Bukkit.getPlayer(requesterId.get());
        if (requester == null || !requester.isOnline()) {
            EssentialsMessages.send(target, "That player is no longer online.");
            return true;
        }
        teleport(requester, target.getLocation());
        EssentialsMessages.send(target, "Accepted " + requester.getName() + "'s teleport request.");
        EssentialsMessages.send(requester, target.getName() + " accepted your teleport request.");
        return true;
    }

    private boolean tpDeny(CommandSender sender) {
        Player target = player(sender);
        if (target == null || !allowed(target, "gardenessentials.tpa")) return true;
        EssentialsMessages.send(target, requests.deny(target.getUniqueId())
                ? "Teleport request denied." : "You do not have a pending teleport request.");
        return true;
    }

    private boolean message(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.msg")) return true;
        if (args.length < 2) {
            EssentialsMessages.send(player, "Use /msg <player> <message>.");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            EssentialsMessages.send(player, "That player is not online.");
            return true;
        }
        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sendPrivate(player, target, text);
        return true;
    }

    private boolean reply(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.msg")) return true;
        UUID partnerId = replyPartners.get(player.getUniqueId());
        if (partnerId == null || args.length == 0) {
            EssentialsMessages.send(player, partnerId == null
                    ? "You do not have anyone to reply to." : "Use /reply <message>.");
            return true;
        }
        Player target = Bukkit.getPlayer(partnerId);
        if (target == null || !target.isOnline()) {
            EssentialsMessages.send(player, "That player is no longer online.");
            return true;
        }
        sendPrivate(player, target, String.join(" ", args));
        return true;
    }

    private boolean back(CommandSender sender) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.back")) return true;
        Location previous = backLocations.get(player.getUniqueId());
        if (previous == null) {
            EssentialsMessages.send(player, "You do not have a previous location.");
            return true;
        }
        Location current = player.getLocation().clone();
        player.teleport(previous);
        backLocations.put(player.getUniqueId(), current);
        EssentialsMessages.send(player, "Returned to your previous location.");
        return true;
    }

    private boolean afk(CommandSender sender) {
        Player player = player(sender);
        if (player == null || !allowed(player, "gardenessentials.afk")) return true;
        boolean nowAfk;
        if (afk.remove(player.getUniqueId())) {
            nowAfk = false;
        } else {
            afk.add(player.getUniqueId());
            nowAfk = true;
        }
        Bukkit.broadcast(EssentialsMessages.prefix()
                .append(Component.text(player.getName() + (nowAfk ? " is now AFK." : " is no longer AFK."),
                        NamedTextColor.WHITE)));
        return true;
    }

    private boolean heal(CommandSender sender, String[] args) {
        Player target = adminTarget(sender, args, "gardenessentials.heal");
        if (target == null) return true;
        target.setHealth(target.getMaxHealth());
        target.setFireTicks(0);
        EssentialsMessages.send(sender, "Healed " + target.getName() + ".");
        return true;
    }

    private boolean feed(CommandSender sender, String[] args) {
        Player target = adminTarget(sender, args, "gardenessentials.feed");
        if (target == null) return true;
        target.setFoodLevel(20);
        target.setSaturation(20F);
        EssentialsMessages.send(sender, "Fed " + target.getName() + ".");
        return true;
    }

    private boolean fly(CommandSender sender, String[] args) {
        Player target = adminTarget(sender, args, "gardenessentials.fly");
        if (target == null) return true;
        boolean enabled = !target.getAllowFlight();
        target.setAllowFlight(enabled);
        if (!enabled && target.getGameMode() != GameMode.CREATIVE && target.getGameMode() != GameMode.SPECTATOR) {
            target.setFlying(false);
        }
        EssentialsMessages.send(sender, "Flight " + (enabled ? "enabled" : "disabled") + " for " + target.getName() + ".");
        return true;
    }

    private Player adminTarget(CommandSender sender, String[] args, String permission) {
        if (!sender.hasPermission(permission)) {
            EssentialsMessages.send(sender, "You do not have permission to do that.");
            return null;
        }
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) EssentialsMessages.send(sender, "That player is not online.");
            return target;
        }
        if (sender instanceof Player player) return player;
        EssentialsMessages.send(sender, "Specify a player.");
        return null;
    }

    private void teleport(Player player, Location destination) {
        backLocations.put(player.getUniqueId(), player.getLocation().clone());
        player.teleport(destination);
    }

    private void sendPrivate(Player from, Player to, String text) {
        from.sendMessage(Component.text("To " + to.getName() + ": ", NamedTextColor.GRAY)
                .append(Component.text(text, NamedTextColor.WHITE)));
        to.sendMessage(Component.text("From " + from.getName() + ": ", NamedTextColor.GRAY)
                .append(Component.text(text, NamedTextColor.WHITE)));
        replyPartners.put(from.getUniqueId(), to.getUniqueId());
        replyPartners.put(to.getUniqueId(), from.getUniqueId());
    }

    public void rememberDeath(Player player, Location location) {
        backLocations.put(player.getUniqueId(), location.clone());
    }

    public boolean isAfk(UUID playerId) {
        return afk.contains(playerId);
    }

    public void clearPlayer(UUID playerId) {
        requests.clearFor(playerId);
        afk.remove(playerId);
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player player) return player;
        EssentialsMessages.send(sender, "This command must be used in-game.");
        return null;
    }

    private boolean allowed(Player player, String permission) {
        if (player.hasPermission(permission)) return true;
        EssentialsMessages.send(player, "You do not have permission to do that.");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if ((name.equals("tpa") || name.equals("msg") || name.equals("heal")
                || name.equals("feed") || name.equals("fly") || name.equals("pay")) && args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        try {
            if ((name.equals("warp") || name.equals("delwarp")) && args.length == 1) {
                return match(args[0], locations.warps());
            }
        } catch (SQLException ignored) {
        }
        return List.of();
    }

    private List<String> match(String prefix, List<String> values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
