package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/owner-booking-requests")
public class BookingsRequestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Expecting an Authorization header: "Bearer <token>"
        String authToken = request.getHeader("Authorization").split(" ")[1];
        if (authToken == null || authToken.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing auth token\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Get landlord's user id from the token (landlord's id is stored in JWT)
            String landlordId = JWTUtil.getUserIdFromToken(authToken);
            if (landlordId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            // SQL query to fetch booking requests for listings owned by this landlord.
            // Note: b.user_id refers to tenant; we join with users u to get tenant details.
            String query = "SELECT b.id AS booking_id, b.booking_date, b.status, b.listing_id, " +
                    "       l.title AS listing_title, l.location AS listing_location, l.price AS listing_price, " +
                    "       l.amenities AS listing_amenities, l.image_url AS listing_image_url, " +
                    "       u.name AS tenant_name, u.email AS tenant_email, u.phone AS tenant_phone " +
                    "FROM bookings b " +
                    "JOIN listings l ON b.listing_id = l.id " +
                    "JOIN users u ON b.user_id = u.id " +
                    "WHERE l.user_id = ? AND LOWER(b.status) IN ('pending', 'approved')";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, Integer.parseInt(landlordId));
                ResultSet rs = stmt.executeQuery();

                JSONArray bookingRequests = new JSONArray();
                while (rs.next()) {
                    JSONObject booking = new JSONObject();
                    booking.put("booking_id", rs.getInt("booking_id"));
                    booking.put("listing_id", rs.getInt("listing_id"));
                    booking.put("listing_title", rs.getString("listing_title"));
                    booking.put("listing_location", rs.getString("listing_location"));
                    booking.put("listing_price", rs.getDouble("listing_price"));
                    booking.put("listing_amenities", rs.getString("listing_amenities"));
                    booking.put("listing_image_url", rs.getString("listing_image_url"));
                    booking.put("tenant_name", rs.getString("tenant_name"));
                    booking.put("tenant_email", rs.getString("tenant_email"));
                    booking.put("tenant_phone", rs.getString("tenant_phone"));
                    booking.put("booking_date", rs.getString("booking_date"));
                    booking.put("status", rs.getString("status"));
                    bookingRequests.put(booking);
                }
                out.print(bookingRequests.toString());
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}