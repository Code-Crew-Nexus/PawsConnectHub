package com.pawsconnect.dao;

import com.pawsconnect.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListingDAO {

    // â”€â”€ Get all listings (admin sees all statuses; public sees only approved) â”€
    public List<Map<String, Object>> getAllListings(boolean adminView) {
        List<Map<String, Object>> listings = new ArrayList<>();
        String query = adminView
                ? "SELECT m.*, u.full_name, u.email " +
                  "FROM marketplace_items m " +
                  "JOIN users u ON m.user_id = u.id " +
                  "ORDER BY m.created_at DESC"
                : "SELECT m.*, u.full_name " +
                  "FROM marketplace_items m " +
                  "JOIN users u ON m.user_id = u.id " +
                  "WHERE m.status = 'approved' " +
                  "ORDER BY m.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                listings.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return listings;
    }

    // â”€â”€ Admin: update listing status (approved / rejected / pending) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public boolean updateListingStatus(int listingId, String status) {
        String query = "UPDATE marketplace_items SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, status);
            ps.setInt(2, listingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // â”€â”€ Admin: delete any listing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public boolean updateListingDetails(int listingId, String title, String description,
                                        double price, String status) {
        String query = "UPDATE marketplace_items SET title = ?, description = ?, price = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, title);
            ps.setString(2, description);
            ps.setDouble(3, price);
            ps.setString(4, status);
            ps.setInt(5, listingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean deleteListing(int listingId) {
        String query = "DELETE FROM marketplace_items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, listingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // â”€â”€ User: create a listing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public boolean createListing(int userId, String title,
                                 String description, double price, String imageUrl) {
        String query = "INSERT INTO marketplace_items " +
                "(user_id, title, description, price, image_url, status) " +
                "VALUES (?, ?, ?, ?, ?, 'pending')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setDouble(4, price);
            ps.setString(5, imageUrl);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}