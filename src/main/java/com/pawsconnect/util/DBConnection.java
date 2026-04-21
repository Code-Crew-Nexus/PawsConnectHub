package com.pawsconnect.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/pawsconnect" +
                    "?useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&serverTimezone=UTC";

    private static final String USER = "root";
    private static final String PASSWORD = "zaheerist25";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            ensureUserRoleColumn();
            ensureBlogSubmissionsTable();
            ensurePostsStatusColumn();
            ensureMarketplaceStatusColumn();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found in classpath", e);
        }
    }

    private static void ensureUserRoleColumn() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, "users", "role")) {
                if (!columns.next()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(
                                "ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user' AFTER password_hash"
                        );
                    }
                }
            }
        } catch (SQLException ignored) {
            // Database may not be available during startup; callers still handle connection errors.
        }
    }

    private static void ensureBlogSubmissionsTable() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS blog_submissions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "user_id INT NOT NULL,"
                + "title VARCHAR(200) NOT NULL,"
                + "content TEXT NOT NULL,"
                + "topic VARCHAR(50),"
                + "author_name VARCHAR(100),"
                + "location VARCHAR(100),"
                + "image_url VARCHAR(255),"
                + "status VARCHAR(20) DEFAULT 'pending',"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                + ")";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableSql);
        } catch (SQLException ignored) {
            // Database may not be available during startup; callers still handle connection errors.
        }
    }

    private static void ensureMarketplaceStatusColumn() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, "marketplace_items", "status")) {
                if (!columns.next()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(
                                "ALTER TABLE marketplace_items "
                                        + "ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'pending' AFTER image_url"
                        );
                    }
                }
            }
        } catch (SQLException ignored) {
            // Database may not be available during startup; callers still handle connection errors.
        }
    }

    private static void ensurePostsStatusColumn() {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet columns = metaData.getColumns(null, null, "posts", "status")) {
                if (!columns.next()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(
                                "ALTER TABLE posts "
                                        + "ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'approved' AFTER image_url"
                        );
                    }
                }
            }
        } catch (SQLException ignored) {
            // Database may not be available during startup; callers still handle connection errors.
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
