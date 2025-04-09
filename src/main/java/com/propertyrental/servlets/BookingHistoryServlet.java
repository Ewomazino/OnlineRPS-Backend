package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//@WebServlet("/booking-history")
public class BookingHistoryServlet extends HttpServlet {

    // Handle pre-flight CORS requests
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // Handle GET request to fetch booking history
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String token = request.getHeader("Authorization").split(" ")[1]; // Token from Authorization header

        try (Connection conn = DBConnection.getConnection()) {
            // Extract user email from token
            String userEmail = JWTUtil.getEmailFromToken(token);
            if (userEmail == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            // Step 1: Fetch user_id using user email
            String userIdQuery = "SELECT id FROM users WHERE email = ?";
            String userId = null;
            try (PreparedStatement userStmt = conn.prepareStatement(userIdQuery)) {
                userStmt.setString(1, userEmail);
                ResultSet userRs = userStmt.executeQuery();
                if (userRs.next()) {
                    userId = userRs.getString("id");
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print("{\"error\": \"User not found\"}");
                    return;
                }
            }

            // Step 2: Query to get booking history for the logged-in user using the user_id.
            // We now include b.listing_id so that listing id is available.
            String query = "SELECT b.id, b.listing_id, b.booking_date, b.status, " +
                    "       l.title, l.location, l.price, l.image_url " +
                    "FROM bookings b " +
                    "JOIN listings l ON b.listing_id = l.id " +
                    "WHERE b.user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, Integer.parseInt(userId));
                ResultSet rs = stmt.executeQuery();

                JSONArray bookings = new JSONArray();
                while (rs.next()) {
                    JSONObject booking = new JSONObject();
                    booking.put("id", rs.getInt("id"));  // Booking id
                    booking.put("listing_id", rs.getInt("listing_id")); // Listing id

                    booking.put("booking_date", rs.getString("booking_date"));
                    booking.put("status", rs.getString("status"));

                    JSONObject listing = new JSONObject();
                    listing.put("title", rs.getString("title"));
                    listing.put("location", rs.getString("location"));
                    listing.put("price", rs.getDouble("price"));
                    listing.put("image_url", rs.getString("image_url"));

                    booking.put("listing", listing);
                    bookings.put(booking);
                }

                // If an 'id' parameter was passed to filter for a single listing, return the first object.
                String idParam = request.getParameter("id");
                if (idParam != null && !idParam.isEmpty() && bookings.length() > 0) {
                    out.print(bookings.get(0).toString());
                } else {
                    out.print(bookings.toString());
                }
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"An error occurred while fetching bookings: " + e.getMessage() + "\"}");
        }
    }

    // Handle DELETE request to cancel a booking.
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String authToken = request.getHeader("Authorization"); // Token from Authorization header
        if (authToken == null || authToken.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing auth token\"}");
            return;
        }

        String listingId = request.getParameter("listingId");
        if (listingId == null || listingId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Missing listingId\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Extract user email from token.
            String userEmail = JWTUtil.getEmailFromToken(authToken);
            if (userEmail == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            // Step 1: Fetch user_id using user email.
            String userIdQuery = "SELECT id FROM users WHERE email = ?";
            String userId = null;
            try (PreparedStatement userStmt = conn.prepareStatement(userIdQuery)) {
                userStmt.setString(1, userEmail);
                ResultSet userRs = userStmt.executeQuery();
                if (userRs.next()) {
                    userId = userRs.getString("id");
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    out.print("{\"error\": \"User not found\"}");
                    return;
                }
            }

            // Step 2: Delete the booking using user_id and listing_id.
            String deleteQuery = "DELETE FROM bookings WHERE user_id = ? AND listing_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                stmt.setInt(1, Integer.parseInt(userId));
                stmt.setInt(2, Integer.parseInt(listingId));
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    out.print("{\"message\": \"Booking cancelled successfully\"}");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    out.print("{\"error\": \"Booking not found or you cannot cancel this booking\"}");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}