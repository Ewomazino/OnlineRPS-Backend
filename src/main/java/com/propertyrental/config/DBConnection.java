package com.propertyrental.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:postgresql://dpg-cvrnfo6r433s73avjojg-a:5432/onlineprs_database?user=onlineprs_database_user&password=pWa5iO5tZCTVCFxqwXrTuoEWvIfQl8tP&sslmode=require";
    private static final String USER = "onlineprs_database_user"; // Change if needed
    private static final String PASSWORD = "pWa5iO5tZCTVCFxqwXrTuoEWvIfQl8tP"; // Replace with your actual password

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