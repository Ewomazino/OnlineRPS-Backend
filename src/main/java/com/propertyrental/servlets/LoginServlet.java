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

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        JSONObject jsonResponse = new JSONObject();

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String userId = rs.getString("id");   // Fetch user_id from DB
                String role = rs.getString("role");         // Fetch role from DB

                // Generate the JWT token with email, user_id, and role
                String token = JWTUtil.generateToken(email, userId, role);

                String accountType = rs.getString("account_type"); // 👈 fetch account type from DB (if needed)

                jsonResponse.put("message", "Login successful");
                jsonResponse.put("token", token);  // Include the token in the response
                jsonResponse.put("accountType", accountType); // Include account type in the response if needed
                response.setStatus(HttpServletResponse.SC_OK); // 200 OK
            } else {
                jsonResponse.put("error", "Invalid email or password");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            }
        } catch (Exception e) {
            jsonResponse.put("error", "Internal server error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
        }

        response.getWriter().write(jsonResponse.toString());
    }
}