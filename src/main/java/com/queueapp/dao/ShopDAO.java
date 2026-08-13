package com.queueapp.dao;

import com.queueapp.model.Shop;
import com.queueapp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShopDAO {

    public int create(Shop shop) throws SQLException {
        String sql = "INSERT INTO shops (shop_name, category, username, password_hash, avg_service_time_mins, is_open) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, shop.getShopName());
            ps.setString(2, shop.getCategory());
            ps.setString(3, shop.getUsername());
            ps.setString(4, shop.getPasswordHash());
            ps.setInt(5, shop.getAvgServiceTimeMins());
            ps.setBoolean(6, shop.isOpen());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    shop.setShopId(id);
                    return id;
                }
            }
            return -1;
        }
    }

    public Shop findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM shops WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Shop findById(int shopId) throws SQLException {
        String sql = "SELECT * FROM shops WHERE shop_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Shop> findAllOpen() throws SQLException {
        String sql = "SELECT * FROM shops WHERE is_open = TRUE ORDER BY shop_name ASC";
        List<Shop> shops = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                shops.add(mapRow(rs));
            }
        }
        return shops;
    }

    public void updateSettings(int shopId, int avgServiceTimeMins, boolean isOpen) throws SQLException {
        String sql = "UPDATE shops SET avg_service_time_mins = ?, is_open = ? WHERE shop_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, avgServiceTimeMins);
            ps.setBoolean(2, isOpen);
            ps.setInt(3, shopId);
            ps.executeUpdate();
        }
    }

    private Shop mapRow(ResultSet rs) throws SQLException {
        Shop shop = new Shop();
        shop.setShopId(rs.getInt("shop_id"));
        shop.setShopName(rs.getString("shop_name"));
        shop.setCategory(rs.getString("category"));
        shop.setUsername(rs.getString("username"));
        shop.setPasswordHash(rs.getString("password_hash"));
        shop.setAvgServiceTimeMins(rs.getInt("avg_service_time_mins"));
        shop.setOpen(rs.getBoolean("is_open"));
        shop.setCreatedAt(rs.getTimestamp("created_at"));
        return shop;
    }
}
