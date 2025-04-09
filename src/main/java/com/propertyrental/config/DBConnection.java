package com.propertyrental.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/rentalsystem";
    private static final String USER = "postgres"; // Change if needed
    private static final String PASSWORD = "admin"; // Replace with your actual password

    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver"); // Load the PostgreSQL driver
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}