package com.herasgarden.gardenessentials.storage;

import com.herasgarden.gardencore.api.storage.GardenStorage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class EssentialsSchema {
    private EssentialsSchema() {}

    public static void ensure(GardenStorage storage) throws SQLException {
        try (Connection connection = storage.connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ge_warps ("
                    + "name_key VARCHAR(32) PRIMARY KEY,"
                    + "display_name VARCHAR(32) NOT NULL,"
                    + "world_uuid VARCHAR(36) NOT NULL,"
                    + "world_name VARCHAR(128) NOT NULL,"
                    + "x DOUBLE NOT NULL,"
                    + "y DOUBLE NOT NULL,"
                    + "z DOUBLE NOT NULL,"
                    + "yaw FLOAT NOT NULL,"
                    + "pitch FLOAT NOT NULL,"
                    + "created_by VARCHAR(36) NOT NULL,"
                    + "created_at BIGINT NOT NULL)");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS ge_server_locations ("
                    + "location_key VARCHAR(32) PRIMARY KEY,"
                    + "world_uuid VARCHAR(36) NOT NULL,"
                    + "world_name VARCHAR(128) NOT NULL,"
                    + "x DOUBLE NOT NULL,"
                    + "y DOUBLE NOT NULL,"
                    + "z DOUBLE NOT NULL,"
                    + "yaw FLOAT NOT NULL,"
                    + "pitch FLOAT NOT NULL,"
                    + "updated_by VARCHAR(36) NOT NULL,"
                    + "updated_at BIGINT NOT NULL)");
        }
    }
}
