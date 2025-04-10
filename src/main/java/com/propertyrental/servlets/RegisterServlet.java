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
import org.json.JSONObject;

//@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Read the request body
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            sb.append(line);
        }

        // Parse the incoming JSON into a User object
        ObjectMapper objectMapper = new ObjectMapper();
        User user;
        try {
            user = objectMapper.readValue(sb.toString(), User.class);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Invalid JSON format.\"}");
            return;
        }

        // Retrieve fields from the User object
        String name = user.getName();
        String email = user.getEmail();
        String role = user.getRole();
        String password = user.getPassword();
        String phone = user.getPhone();
        String accountType = user.getAccountType(); // New field

        JSONObject jsonResponse = new JSONObject();

        // Validate that required fields are provided
        if (name == null || name.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                phone == null || phone.trim().isEmpty() ||
                accountType == null || accountType.trim().isEmpty()) {

            jsonResponse.put("error", "Missing required fields: name, email, password, phone, and account type are required.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        // Basic email format validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!email.matches(emailRegex)) {
            jsonResponse.put("error", "Invalid email format.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        // Validate minimum password length (e.g., at least 6 characters)
        if (password.length() < 6) {
            jsonResponse.put("error", "Password must be at least 6 characters long.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        // You can add additional validations (for phone, role, etc.) as required

        // Attempt to get the database connection
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                jsonResponse.put("error", "Database connection failed");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            // Prepare SQL insertion
            String insertQuery = "INSERT INTO users (name, email, password, phone, account_type) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = con.prepareStatement(insertQuery)) {
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.setString(3, password); // Note: Passwords should be hashed in production!
                stmt.setString(4, phone);
                stmt.setString(5, accountType);
                stmt.executeUpdate();
            }

            jsonResponse.put("message", "User registered successfully");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            jsonResponse.put("error", "Internal server error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(jsonResponse.toString());
    }
}