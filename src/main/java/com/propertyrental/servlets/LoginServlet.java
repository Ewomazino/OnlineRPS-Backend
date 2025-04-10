package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONObject;

//@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Retrieve parameters from the request
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        JSONObject jsonResponse = new JSONObject();

        // Basic validation for required parameters
        if (email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            jsonResponse.put("error", "Email and password are required.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        // Validate the email format (basic regex)
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            jsonResponse.put("error", "Invalid email format.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(jsonResponse.toString());
            return;
        }

        // Attempt to create a database connection
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                jsonResponse.put("error", "Database connection not available.");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(jsonResponse.toString());
                return;
            }

            // Prepare and execute the query to authenticate user
            String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Retrieve fields from the user record
                String userId = rs.getString("id");
                String role = rs.getString("role");
                String accountType = rs.getString("account_type");

                // Generate a JWT token including email, userId, and role.
                String token = JWTUtil.generateToken(email, userId, role);

                jsonResponse.put("message", "Login successful");
                jsonResponse.put("token", token);
                jsonResponse.put("accountType", accountType);
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                jsonResponse.put("error", "Invalid email or password");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (Exception e) {
            jsonResponse.put("error", "Internal server error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        response.getWriter().write(jsonResponse.toString());
    }
}