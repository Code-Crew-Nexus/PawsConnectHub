package com.pawsconnect.dao;

import com.pawsconnect.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlogDAO {

    /**
     * Get all blog submissions with optional status filter
     * @param status - filter by status (pending, approved, rejected) or null for all
     * @return List of blog submissions with user details
     */
    public List<Map<String, Object>> getBlogSubmissions(String status) {
        List<Map<String, Object>> submissions = new ArrayList<>();
        String query = status == null || status.isEmpty()
                ? "SELECT b.*, u.full_name, u.email FROM blog_submissions b JOIN users u ON b.user_id = u.id ORDER BY b.created_at DESC"
                : "SELECT b.*, u.full_name, u.email FROM blog_submissions b JOIN users u ON b.user_id = u.id WHERE b.status = ? ORDER BY b.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            if (status != null && !status.isEmpty()) {
                ps.setString(1, status);
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    submissions.add(row);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return submissions;
    }

    /**
     * Create a new blog submission
     */
    public boolean createBlogSubmission(int userId, String title, String content, String topic,
                                        String authorName, String location) {
        String query = "INSERT INTO blog_submissions (user_id, title, content, topic, author_name, location, status) " +
                       "VALUES (?, ?, ?, ?, ?, ?, 'pending')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setString(4, topic);
            ps.setString(5, authorName);
            ps.setString(6, location);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Update blog submission status (approve/reject/pending)
     */
    public boolean updateBlogSubmissionStatus(int submissionId, String newStatus) {
        String query = "UPDATE blog_submissions SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, newStatus);
            ps.setInt(2, submissionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a blog submission
     */
    public boolean updateBlogSubmission(int submissionId, String title, String content,
                                        String topic, String authorName, String location,
                                        String status) {
        String query = "UPDATE blog_submissions "
                + "SET title = ?, content = ?, topic = ?, author_name = ?, location = ?, status = ? "
                + "WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, title);
            ps.setString(2, content);
            ps.setString(3, topic);
            ps.setString(4, authorName);
            ps.setString(5, location);
            ps.setString(6, status);
            ps.setInt(7, submissionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean deleteBlogSubmission(int submissionId) {
        String query = "DELETE FROM blog_submissions WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, submissionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get pending blog submissions count
     */
    public int getPendingCount() {
        String query = "SELECT COUNT(*) FROM blog_submissions WHERE status = 'pending'";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get approved blog submissions (for public display)
     */
    public List<Map<String, Object>> getApprovedBlogs() {
        List<Map<String, Object>> blogs = new ArrayList<>();
        String query = "SELECT b.*, u.full_name FROM blog_submissions b JOIN users u ON b.user_id = u.id WHERE b.status = 'approved' ORDER BY b.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                blogs.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return blogs;
    }
}

