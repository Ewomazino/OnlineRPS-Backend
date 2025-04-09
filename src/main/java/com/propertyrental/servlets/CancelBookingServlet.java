package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/cancel-book") // Ensure this matches your frontend endpoint
public class CancelBookingServlet extends HttpServlet {

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Validate the Authorization header
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Missing or invalid token\"}");
            return;
        }

        // Retrieve the tenant's user ID from the token
        String tenantIdStr = JWTUtil.getUserIdFromToken(token.substring(7));
        if (tenantIdStr == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
            return;
        }

        int listingId;
        try {
            listingId = Integer.parseInt(request.getParameter("listingId"));
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Invalid listing ID\"}");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Update SQL: use user_id instead of tenant_email
            String sql = "DELETE FROM bookings WHERE listing_id = ? AND user_id = ? AND status = 'pending'";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setInt(1, listingId);
            stmt.setInt(2, Integer.parseInt(tenantIdStr));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                response.getWriter().write("{\"message\": \"Booking cancelled successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\": \"No pending booking found to cancel\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }
}