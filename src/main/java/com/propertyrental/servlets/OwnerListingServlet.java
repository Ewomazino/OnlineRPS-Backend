package com.propertyrental.servlets;


import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/owner-listings")
public class OwnerListingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String authToken = request.getHeader("Authorization").split(" ")[1];
        if (authToken == null || authToken.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing auth token\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String userId = JWTUtil.getUserIdFromToken(authToken);

            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            // Query to get all listings of the property owner
            String query = "SELECT * FROM listings WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, Integer.parseInt(userId));
                ResultSet rs = stmt.executeQuery();

                JSONArray listings = new JSONArray();
                while (rs.next()) {
                    JSONObject listing = new JSONObject();
                    listing.put("id", rs.getInt("id"));
                    listing.put("title", rs.getString("title"));
                    listing.put("location", rs.getString("location"));
                    listing.put("price", rs.getDouble("price"));
                    listing.put("image_url", rs.getString("image_url"));
                    listings.put(listing);
                }

                out.print(listings);
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}