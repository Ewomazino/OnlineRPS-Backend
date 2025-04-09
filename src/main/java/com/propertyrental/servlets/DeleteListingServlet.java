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

@WebServlet("/delete-listing/*")
public class DeleteListingServlet extends HttpServlet {
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Extract listingId from URL (e.g., /delete-listing/123)
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Listing ID is missing.\"}");
            return;
        }
        String listingIdStr = pathInfo.substring(1);

        // Validate JWT token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing auth token.\"}");
            return;
        }
        String token = authHeader.split(" ")[1];
        String landlordId = JWTUtil.getUserIdFromToken(token);
        if (landlordId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Invalid or expired token.\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Verify that the listing belongs to the landlord
            String verifyQuery = "SELECT id FROM listings WHERE id = ? AND user_id = ?";
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery)) {
                verifyStmt.setInt(1, Integer.parseInt(listingIdStr));
                verifyStmt.setInt(2, Integer.parseInt(landlordId));
                ResultSet rs = verifyStmt.executeQuery();
                if (!rs.next()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"error\": \"Listing not found or you are not authorized to delete this listing.\"}");
                    return;
                }
            }

            // Delete the listing
            String deleteQuery = "DELETE FROM listings WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                stmt.setInt(1, Integer.parseInt(listingIdStr));
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message", "Listing deleted successfully.");
                    out.print(jsonResponse.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\": \"Failed to delete listing.\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}