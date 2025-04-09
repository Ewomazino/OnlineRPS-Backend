package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/approve-booking/*")
public class ApproveBookingsServlet extends HttpServlet {
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String bookingId = request.getPathInfo().substring(1); // Extract bookingId from URL
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing auth token\"}");
            return;
        }

        // Remove "Bearer " prefix if present
        String authToken = authHeader.split(" ")[1];

        try (Connection conn = DBConnection.getConnection()) {
            // Extract landlord's user id from the token
            String landlordId = JWTUtil.getUserIdFromToken(authToken);
            if (landlordId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            // Step 1: Retrieve the tenant_id and listing_id from the booking record
            int tenantId;
            int listingId;
            String bookingQuery = "SELECT user_id, listing_id FROM bookings WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(bookingQuery)) {
                ps.setInt(1, Integer.parseInt(bookingId));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tenantId = rs.getInt("user_id");
                    listingId = rs.getInt("listing_id");
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\": \"Booking not found\"}");
                    return;
                }
            }

            // Step 2: Check if this tenant already has an approved booking
            String tenantCheckQuery = "SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status = 'Approved'";
            try (PreparedStatement tenantStmt = conn.prepareStatement(tenantCheckQuery)) {
                tenantStmt.setInt(1, tenantId);
                ResultSet rs = tenantStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print("{\"error\": \"Tenant already has an approved booking\"}");
                    return;
                }
            }

            // Step 3: Check if the listing already has an approved booking
            String listingCheckQuery = "SELECT COUNT(*) FROM bookings WHERE listing_id = ? AND status = 'Approved'";
            try (PreparedStatement listingStmt = conn.prepareStatement(listingCheckQuery)) {
                listingStmt.setInt(1, listingId);
                ResultSet rs = listingStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print("{\"error\": \"This listing already has an approved booking\"}");
                    return;
                }
            }

            // Step 4: Approve the booking (update status to 'Approved')
            String updateQuery = "UPDATE bookings SET status = 'Approved' WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                stmt.setInt(1, Integer.parseInt(bookingId));
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    out.print("{\"message\": \"Booking approved successfully\"}");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\": \"Booking not found or already approved\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}