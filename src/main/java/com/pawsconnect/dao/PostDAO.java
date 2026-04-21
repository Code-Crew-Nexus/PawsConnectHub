package com.pawsconnect.dao;

import com.pawsconnect.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDAO {

    public List<Map<String, Object>> getAllPosts(boolean adminView) {
        List<Map<String, Object>> posts = new ArrayList<>();
        String query = adminView
                ? "SELECT p.*, u.full_name, u.email FROM posts p JOIN users u ON p.user_id = u.id ORDER BY p.created_at DESC"
                : "SELECT p.*, u.full_name FROM posts p JOIN users u ON p.user_id = u.id WHERE p.status = 'approved' ORDER BY p.created_at DESC";

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
                posts.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    public boolean createPost(int userId, String content) {
        String query = "INSERT INTO posts (user_id, content, status) VALUES (?, ?, 'pending')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ps.setString(2, content);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updatePostStatus(int postId, String status) {
        String query = "UPDATE posts SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, status);
            ps.setInt(2, postId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── Admin: delete any post ────────────────────────────────────────────────
    public boolean deletePost(int postId) {
        String query = "DELETE FROM posts WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, postId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── Admin: update post content ────────────────────────────────────────────
    public boolean updatePost(int postId, String newContent) {
        String query = "UPDATE posts SET content = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, newContent);
            ps.setInt(2, postId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
