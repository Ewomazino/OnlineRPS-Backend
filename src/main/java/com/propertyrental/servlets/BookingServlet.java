package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

import java.io.*;
import java.sql.*;

@WebServlet("/book")
public class BookingServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Read the request body to get the listingId
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            sb.append(line);
        }
        JSONObject reqBody = new JSONObject(sb.toString());
        int listingId = reqBody.getInt("listingId");

        // Get token from the Authorization header (expected format: "Bearer <token>")
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing auth token\"}");
            return;
        }
        String authToken = authHeader.split(" ")[1];

        try (Connection con = DBConnection.getConnection()) {
            // Extract tenant ID from the token
            String tenantId = JWTUtil.getUserIdFromToken(authToken);
            if (tenantId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            // Check if the tenant already has an approved booking
            String checkQuery = "SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status = 'Approved'";
            try (PreparedStatement checkStmt = con.prepareStatement(checkQuery)) {
                checkStmt.setInt(1, Integer.parseInt(tenantId));
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print("{\"error\": \"You already have an approved booking. You cannot make any further bookings.\"}");
                    return;
                }
            }

            // If no approved booking exists, insert the new booking with status 'pending'
            String insertQuery = "INSERT INTO bookings (listing_id, user_id, status) VALUES (?, ?, 'pending')";
            try (PreparedStatement insertStmt = con.prepareStatement(insertQuery)) {
                insertStmt.setInt(1, listingId);
                insertStmt.setInt(2, Integer.parseInt(tenantId));
                int rowsAffected = insertStmt.executeUpdate();
                if (rowsAffected > 0) {
                    out.print("{\"message\": \"Booking request sent successfully\"}");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\": \"Failed to create booking request.\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}