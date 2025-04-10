package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    // GET /profile: Return profile information for the logged-in user
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Validate and extract token; expecting format "Bearer <token>"
        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing or invalid auth token.\"}");
            return;
        }
        String token = authHeader.substring(7);
        String userId = JWTUtil.getUserIdFromToken(token);
        if(userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Invalid or expired token.\"}");
            return;
        }

        // Query the users table to get profile information
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT id, name, email, account_type, phone FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, Integer.parseInt(userId));
                ResultSet rs = stmt.executeQuery();
                if(rs.next()){
                    JSONObject profile = new JSONObject();
                    profile.put("id", rs.getInt("id"));
                    profile.put("name", rs.getString("name"));
                    profile.put("email", rs.getString("email"));
                    profile.put("account_type", rs.getString("account_type"));
                    profile.put("phone", rs.getString("phone"));

                    out.print(profile);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\": \"User not found.\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }

    // PUT /profile: Update profile information for the logged-in user
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Validate the JWT token
        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing or invalid auth token.\"}");
            return;
        }
        String token = authHeader.substring(7);
        String userId = JWTUtil.getUserIdFromToken(token);
        if(userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Invalid or expired token.\"}");
            return;
        }

        // Read the request body as a JSON object
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while((line = reader.readLine()) != null){
            sb.append(line);
        }
        JSONObject reqBody = new JSONObject(sb.toString());
        // For this example, we'll allow updating name and phone.
        String name = reqBody.optString("name", null);
        String phone = reqBody.optString("phone", null);

        if(name == null || phone == null){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Missing required profile details (name and phone).\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE users SET name = ?, phone = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, phone);
                stmt.setInt(3, Integer.parseInt(userId));

                int rowsAffected = stmt.executeUpdate();
                if(rowsAffected > 0){
                    response.setStatus(HttpServletResponse.SC_OK);
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message", "Profile updated successfully.");
                    out.print(jsonResponse);
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\": \"Failed to update profile.\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}