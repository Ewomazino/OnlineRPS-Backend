package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.model.User;  // Import User class
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;


public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            sb.append(line);
        }

        // Parse the incoming JSON into the User object
        ObjectMapper objectMapper = new ObjectMapper();
        User user = objectMapper.readValue(sb.toString(), User.class);

        String name = user.getName();
        String email = user.getEmail();
        String role = user.getRole();
        String password = user.getPassword();
        String phone = user.getPhone();
        String accountType = user.getAccountType(); // New field

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                response.getWriter().write("{\"error\": \"Database connection failed\"}");
                return;
            }

            String insertQuery = "INSERT INTO users (name, email, password, phone, account_type) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = con.prepareStatement(insertQuery);
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, password); // Should be hashed
            stmt.setString(4, phone);
            stmt.setString(5, accountType);
            stmt.executeUpdate();

            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"User registered successfully\"}");
        } catch (Exception e) {
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}