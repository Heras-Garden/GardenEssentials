package com.herasgarden.gardenessentials.location;

import com.herasgarden.gardencore.api.storage.GardenStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class LocationService {
    private final GardenStorage storage;

    public LocationService(GardenStorage storage) {
        this.storage = storage;
    }

    public void setSpawn(UUID editor, Location location) throws SQLException {
        upsertServerLocation("spawn", editor, location);
    }

    public Optional<Location> spawn() throws SQLException {
        try (Connection connection = storage.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM ge_server_locations WHERE location_key = 'spawn'");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? Optional.ofNullable(read(result)) : Optional.empty();
        }
    }

    public void setWarp(UUID editor, String name, Location location) throws SQLException {
        String key = key(name);
        if (key.isBlank()) throw new IllegalArgumentException("Warp names cannot be empty.");
        long now = System.currentTimeMillis();
        try (Connection connection = storage.connection()) {
            int changed;
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE ge_warps SET display_name = ?, world_uuid = ?, world_name = ?, x = ?, y = ?, z = ?, "
                            + "yaw = ?, pitch = ?, created_by = ?, created_at = ? WHERE name_key = ?")) {
                update.setString(1, cleanName(name));
                bindLocation(update, 2, location);
                update.setString(9, editor.toString());
                update.setLong(10, now);
                update.setString(11, key);
                changed = update.executeUpdate();
            }
            if (changed == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO ge_warps "
                                + "(name_key, display_name, world_uuid, world_name, x, y, z, yaw, pitch, created_by, created_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    insert.setString(1, key);
                    insert.setString(2, cleanName(name));
                    bindLocation(insert, 3, location);
                    insert.setString(10, editor.toString());
                    insert.setLong(11, now);
                    insert.executeUpdate();
                }
            }
        }
    }

    public Optional<Location> warp(String name) throws SQLException {
        try (Connection connection = storage.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM ge_warps WHERE name_key = ?")) {
            statement.setString(1, key(name));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.ofNullable(read(result)) : Optional.empty();
            }
        }
    }

    public List<String> warps() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection connection = storage.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT display_name FROM ge_warps ORDER BY display_name");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) names.add(result.getString("display_name"));
        }
        return names;
    }

    public boolean deleteWarp(String name) throws SQLException {
        try (Connection connection = storage.connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM ge_warps WHERE name_key = ?")) {
            statement.setString(1, key(name));
            return statement.executeUpdate() > 0;
        }
    }

    private void upsertServerLocation(String key, UUID editor, Location location) throws SQLException {
        long now = System.currentTimeMillis();
        try (Connection connection = storage.connection()) {
            int changed;
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE ge_server_locations SET world_uuid = ?, world_name = ?, x = ?, y = ?, z = ?, "
                            + "yaw = ?, pitch = ?, updated_by = ?, updated_at = ? WHERE location_key = ?")) {
                bindLocation(update, 1, location);
                update.setString(8, editor.toString());
                update.setLong(9, now);
                update.setString(10, key);
                changed = update.executeUpdate();
            }
            if (changed == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO ge_server_locations "
                                + "(location_key, world_uuid, world_name, x, y, z, yaw, pitch, updated_by, updated_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    insert.setString(1, key);
                    bindLocation(insert, 2, location);
                    insert.setString(9, editor.toString());
                    insert.setLong(10, now);
                    insert.executeUpdate();
                }
            }
        }
    }

    private void bindLocation(PreparedStatement statement, int start, Location location) throws SQLException {
        World world = location.getWorld();
        if (world == null) throw new IllegalArgumentException("Location world is unavailable.");
        statement.setString(start, world.getUID().toString());
        statement.setString(start + 1, world.getName());
        statement.setDouble(start + 2, location.getX());
        statement.setDouble(start + 3, location.getY());
        statement.setDouble(start + 4, location.getZ());
        statement.setFloat(start + 5, location.getYaw());
        statement.setFloat(start + 6, location.getPitch());
    }

    private Location read(ResultSet result) throws SQLException {
        UUID worldId = UUID.fromString(result.getString("world_uuid"));
        World world = Bukkit.getWorld(worldId);
        if (world == null) world = Bukkit.getWorld(result.getString("world_name"));
        if (world == null) return null;
        return new Location(world,
                result.getDouble("x"),
                result.getDouble("y"),
                result.getDouble("z"),
                result.getFloat("yaw"),
                result.getFloat("pitch"));
    }

    private String key(String value) {
        if (value == null) return "";
        String clean = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private String cleanName(String value) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.length() > 32) clean = clean.substring(0, 32);
        return clean;
    }
}
