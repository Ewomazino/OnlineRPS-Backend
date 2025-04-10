package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/listing-details")
public class ListingDetailsServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        String listingId = request.getParameter("listingId");

        if (listingId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Missing listingId\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT l.title, l.location, l.price, l.description, l.image_url, " +
                    "u.name, u.email " +
                    "FROM listings l " +
                    "JOIN users u ON l.user_id = u.id " +
                    "WHERE l.id = ? AND u.account_type = 'property-owner'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(listingId));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                JSONObject listing = new JSONObject();
                listing.put("title", rs.getString("title"));
                listing.put("location", rs.getString("location"));
                listing.put("price", rs.getDouble("price"));
                listing.put("description", rs.getString("description"));

                // Retrieve and log the image_url field.
                String imageUrlStr = rs.getString("image_url");
                System.out.println("Fetched image_url from DB: " + imageUrlStr);

                // If the image_url field contains comma-separated URLs, convert them into a JSON array.
                if (imageUrlStr != null && !imageUrlStr.trim().isEmpty()) {
                    String[] urlArray = imageUrlStr.split(",");
                    JSONArray imageUrls = new JSONArray();
                    for (String url : urlArray) {
                        imageUrls.put(url.trim());
                    }
                    listing.put("image_url", imageUrls);
                } else {
                    // If no images are found, provide an empty array.
                    listing.put("image_url", new JSONArray());
                }

                // Build the landlord JSON object.
                JSONObject landlord = new JSONObject();
                landlord.put("name", rs.getString("name"));
                landlord.put("email", rs.getString("email"));
                listing.put("landlord", landlord);

                out.print(listing);
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Listing not found or not owned by a property-owner\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Server error: " + e.getMessage() + "\"}");
        }
    }
}