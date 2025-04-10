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

@WebServlet("/featured-listings")
public class FeaturedListingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBConnection.getConnection()) {
            // Option 1: Use a "featured" column (if available)
            // String sql = "SELECT id, title, location, price, image_url FROM listings WHERE featured = TRUE";

            // Option 2: If there is no "featured" column, use a criteria (e.g., latest 3 listings)
            String sql = "SELECT id, title, location, price, image_url FROM listings ORDER BY created_at DESC LIMIT 3";

            PreparedStatement stmt = conn.prepareStatement(sql);
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
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"An error occurred while fetching featured listings: " + e.getMessage() + "\"}");
        }
    }
}