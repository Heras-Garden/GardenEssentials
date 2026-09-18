package com.herasgarden.gardenessentials.iris;

import com.herasgarden.gardencore.api.GardenPlatform;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class MinecraftLinkService {
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final GardenPlatform platform;
    private final SecureRandom random = new SecureRandom();
    private final long expiryMillis;

    public MinecraftLinkService(GardenPlatform platform, long expiryMinutes) {
        this.platform = platform;
        this.expiryMillis = Math.max(1L, expiryMinutes) * 60_000L;
    }

    public LinkCode issue(Player player) throws SQLException {
        long now = System.currentTimeMillis();
        long expiresAt = now + expiryMillis;
        String code = randomCode();
        String hash = hash(code);

        try (Connection connection = platform.storage().connection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement invalidate = connection.prepareStatement(
                        "UPDATE gc_minecraft_link_codes SET consumed_at = ? "
                                + "WHERE player_uuid = ? AND consumed_at IS NULL")) {
                    invalidate.setLong(1, now);
                    invalidate.setString(2, player.getUniqueId().toString());
                    invalidate.executeUpdate();
                }

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO gc_minecraft_link_codes "
                                + "(code_hash, player_uuid, player_name, created_at, expires_at, consumed_at) "
                                + "VALUES (?, ?, ?, ?, ?, NULL)")) {
                    insert.setString(1, hash);
                    insert.setString(2, player.getUniqueId().toString());
                    insert.setString(3, player.getName());
                    insert.setLong(4, now);
                    insert.setLong(5, expiresAt);
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }

        return new LinkCode(code, expiresAt);
    }

    public Optional<LinkStatus> status(UUID playerId) throws SQLException {
        try (Connection connection = platform.storage().connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT discord_id, player_name, linked_at "
                             + "FROM gc_minecraft_links WHERE player_uuid = ? LIMIT 1")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(new LinkStatus(
                        result.getString("discord_id"),
                        result.getString("player_name"),
                        result.getLong("linked_at")
                ));
            }
        }
    }

    public boolean isLinked(UUID playerId) throws SQLException {
        try (Connection connection = platform.storage().connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM gc_minecraft_links WHERE player_uuid = ? LIMIT 1")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean unlink(UUID playerId) throws SQLException {
        try (Connection connection = platform.storage().connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM gc_minecraft_links WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            return statement.executeUpdate() > 0;
        }
    }

    private String randomCode() {
        StringBuilder value = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            value.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        }
        return value.toString();
    }

    public static String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    code.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record LinkCode(String code, long expiresAt) {
    }

    public record LinkStatus(String discordId, String playerName, long linkedAt) {
    }
}
