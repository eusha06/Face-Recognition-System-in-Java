package com.windsurfproject;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHandler {
    private Connection conn;
    private final String dbPath = "faces.db";
    private Map<Integer, String> userNames;

    public DatabaseHandler() {
        userNames = new HashMap<>();
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTable();
            loadUserNames();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "gender TEXT NOT NULL," +
                    "age INTEGER NOT NULL," +
                    "face_data BLOB NOT NULL)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void loadUserNames() throws SQLException {
        String sql = "SELECT id, name, gender, age FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String gender = rs.getString("gender");
                int age = rs.getInt("age");
                userNames.put(id, String.format("%s (%s, %d)", name, gender, age));
            }
        }
    }

    public int addUser(String name, String gender, int age, byte[] faceData) throws SQLException {
        String sql = "INSERT INTO users (name, gender, age, face_data) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, gender);
            pstmt.setInt(3, age);
            pstmt.setBytes(4, faceData);
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    userNames.put(id, String.format("%s (%s, %d)", name, gender, age));
                    return id;
                }
            }
        }
        throw new SQLException("Failed to get generated ID for new user");
    }

    public String getUserName(int id) {
        return userNames.getOrDefault(id, "Unknown");
    }

    public void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
