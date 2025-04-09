package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.sql.*;

@WebServlet("/my-bookings")
public class MyBookingsServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        // Extract token and retrieve tenant's user ID from it
        String token = request.getHeader("Authorization").split(" ")[1];
        String tenantId = JWTUtil.getUserIdFromToken(token);
        if (tenantId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid or expired token.\"}");
            return;
        }

        JSONArray bookings = new JSONArray();

        try (Connection con = DBConnection.getConnection()) {
            // Use tenant's user_id instead of tenant_email
            String sql = "SELECT listing_id, status, booking_date FROM bookings WHERE user_id = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setInt(1, Integer.parseInt(tenantId));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject booking = new JSONObject();
                booking.put("listingId", rs.getInt("listing_id"));
                booking.put("status", rs.getString("status"));
                booking.put("booking_date", rs.getString("booking_date")); // Ensure your DB stores this as a valid date string
                bookings.put(booking);
            }

            response.getWriter().write(bookings.toString());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Failed to fetch bookings.\"}");
        }
    }
}