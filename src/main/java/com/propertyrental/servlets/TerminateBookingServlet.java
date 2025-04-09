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

@WebServlet("/terminate-booking/*")
public class TerminateBookingServlet extends HttpServlet {

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Set up response type and encoding
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extract bookingId from the URL path (e.g., /terminate-booking/123)
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Booking ID is missing.\"}");
            return;
        }
        String bookingIdStr = pathInfo.substring(1);

        // Validate the Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing auth token.\"}");
            return;
        }
        // Assume header format: "Bearer <token>"
        String token = authHeader.split(" ")[1];
        String landlordId = JWTUtil.getUserIdFromToken(token);
        if (landlordId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Invalid or expired token.\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Verify that the booking belongs to one of the landlord's listings and is currently approved
            String checkQuery = "SELECT b.id " +
                    "FROM bookings b " +
                    "JOIN listings l ON b.listing_id = l.id " +
                    "WHERE b.id = ? AND b.status = 'Approved' AND l.user_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setInt(1, Integer.parseInt(bookingIdStr));
                checkStmt.setInt(2, Integer.parseInt(landlordId));
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\": \"Approved booking not found for your listing.\"}");
                    return;
                }
            }

            // Update the booking's status to "Terminated"
            String updateQuery = "UPDATE bookings SET status = 'Terminated' WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setInt(1, Integer.parseInt(bookingIdStr));
                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message", "Booking terminated successfully.");
                    out.print(jsonResponse.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\": \"Booking not found or could not be terminated.\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}