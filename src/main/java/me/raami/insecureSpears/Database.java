/*
 * Copyright (C) 2026 ramenrrami
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * You are not allowed to remove the credits when using this plugin.
 */
package me.raami.insecureSpears;

import org.bukkit.Bukkit;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Database {

    private final InsecureSpears plugin;
    private Connection connection;

    public Database(InsecureSpears plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            boolean isProxy = plugin.getConfig().getBoolean("proxy", false);
            if (isProxy) {
                String host = plugin.getConfig().getString("mysql.host");
                int port = plugin.getConfig().getInt("mysql.port");
                String database = plugin.getConfig().getString("mysql.database");
                String username = plugin.getConfig().getString("mysql.username");
                String password = plugin.getConfig().getString("mysql.password");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
                connection = DriverManager.getConnection(url, username, password);
            } else {
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException ignored) {}
                File dbFile = new File(plugin.getDataFolder(), "spears.db");
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                connection = DriverManager.getConnection(url);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void createTable() {
        if (!checkConnection()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isProxy = plugin.getConfig().getBoolean("proxy", false);
            try (Statement statement = connection.createStatement()) {
                if (isProxy) {
                    statement.execute("CREATE TABLE IF NOT EXISTS insecurespears_items (spear_uuid VARCHAR(36) PRIMARY KEY, owner_name VARCHAR(16), owner_uuid VARCHAR(36), is_active BOOLEAN DEFAULT TRUE)");
                } else {
                    statement.execute("CREATE TABLE IF NOT EXISTS insecurespears_items (spear_uuid VARCHAR(36) PRIMARY KEY, owner_name VARCHAR(16), owner_uuid VARCHAR(36), is_active INTEGER DEFAULT 1)");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void insertSpearAsync(String spearUuid, String ownerName, String ownerUuid) {
        if (!checkConnection()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isProxy = plugin.getConfig().getBoolean("proxy", false);
            String sql = isProxy 
                ? "INSERT INTO insecurespears_items (spear_uuid, owner_name, owner_uuid, is_active) VALUES (?, ?, ?, TRUE) ON DUPLICATE KEY UPDATE owner_name = ?, owner_uuid = ?"
                : "INSERT OR REPLACE INTO insecurespears_items (spear_uuid, owner_name, owner_uuid, is_active) VALUES (?, ?, ?, 1)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, spearUuid);
                ps.setString(2, ownerName);
                ps.setString(3, ownerUuid);
                if (isProxy) {
                    ps.setString(4, ownerName);
                    ps.setString(5, ownerUuid);
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateOwnerAsync(String spearUuid, String ownerName, String ownerUuid) {
        if (!checkConnection()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE insecurespears_items SET owner_name = ?, owner_uuid = ? WHERE spear_uuid = ?")) {
                ps.setString(1, ownerName);
                ps.setString(2, ownerUuid);
                ps.setString(3, spearUuid);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void removeAllAsync(Runnable callback) {
        if (!checkConnection()) {
            if (callback != null) Bukkit.getScheduler().runTask(plugin, callback);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isProxy = plugin.getConfig().getBoolean("proxy", false);
            String sql = isProxy ? "UPDATE insecurespears_items SET is_active = FALSE" : "UPDATE insecurespears_items SET is_active = 0";
            try (Statement st = connection.createStatement()) {
                st.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, callback);
            }
        });
    }

    public void fetchRemovedSpears(Set<String> set) {
        if (!checkConnection()) return;
        boolean isProxy = plugin.getConfig().getBoolean("proxy", false);
        String sql = isProxy ? "SELECT spear_uuid FROM insecurespears_items WHERE is_active = FALSE" : "SELECT spear_uuid FROM insecurespears_items WHERE is_active = 0";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                set.add(rs.getString("spear_uuid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getActiveSpearsCountsAsync(java.util.function.Consumer<Map<String, Integer>> callback) {
        if (!checkConnection()) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new HashMap<>()));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Integer> counts = new HashMap<>();
            boolean isProxy = plugin.getConfig().getBoolean("proxy", false);
            String sql = isProxy ? "SELECT owner_name, COUNT(*) as amount FROM insecurespears_items WHERE is_active = TRUE GROUP BY owner_name" 
                                 : "SELECT owner_name, COUNT(*) as amount FROM insecurespears_items WHERE is_active = 1 GROUP BY owner_name";
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    String name = rs.getString("owner_name");
                    int amount = rs.getInt("amount");
                    if (name != null && !name.isEmpty() && amount > 0) {
                        counts.put(name, amount);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(counts));
        });
    }
}