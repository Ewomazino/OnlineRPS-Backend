package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/edit-listing/*")
public class EditListingServlet extends HttpServlet {
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set response content type and encoding
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Extract listing ID from URL (e.g., /edit-listing/123)
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Listing ID is missing.\"}");
            return;
        }
        String listingIdStr = pathInfo.substring(1);

        // Validate JWT token from the Authorization header ("Bearer <token>")
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

        // Read the JSON from the request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        JSONObject reqBody = new JSONObject(sb.toString());

        // Extract listing details from JSON with fallback values
        String title = reqBody.optString("title", null);
        String description = reqBody.optString("description", null);
        String location = reqBody.optString("location", null);
        String priceStr = reqBody.optString("price", null);
        String amenities = reqBody.optString("amenities", "");
        String imageUrl = reqBody.optString("image_url", "");

        if (title == null || description == null || location == null || priceStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Missing required listing details.\"}");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException nfe) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid price format.\"}");
            return;
        }

        // Update the listing in the database if it belongs to the current landlord
        try (Connection conn = DBConnection.getConnection()) {
            // Verify listing ownership
            String verifyQuery = "SELECT id FROM listings WHERE id = ? AND user_id = ?";
            try (PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery)) {
                verifyStmt.setInt(1, Integer.parseInt(listingIdStr));
                verifyStmt.setInt(2, Integer.parseInt(landlordId));
                ResultSet rs = verifyStmt.executeQuery();
                if (!rs.next()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"error\": \"Listing not found or you are not authorized to edit this listing.\"}");
                    return;
                }
            }

            // Update the listing and set updated_at to NOW()
            String updateQuery = "UPDATE listings SET title = ?, description = ?, location = ?, price = ?, amenities = ?, image_url = ?, updated_at = NOW() WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                stmt.setString(1, title);
                stmt.setString(2, description);
                stmt.setString(3, location);
                stmt.setDouble(4, price);
                stmt.setString(5, amenities);
                stmt.setString(6, imageUrl);
                stmt.setInt(7, Integer.parseInt(listingIdStr));

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message", "Listing updated successfully.");
                    out.print(jsonResponse.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"error\": \"Failed to update listing.\"}");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}