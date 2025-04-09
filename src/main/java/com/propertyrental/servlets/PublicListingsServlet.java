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

@WebServlet("/public-listings")
public class PublicListingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Set the content type and character encoding for the response.
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBConnection.getConnection()) {
            // SQL query to fetch public listings.
            // You can add additional criteria for public listings if needed.
            String sql = "SELECT id, title, location, price, image_url, description " +
                    "FROM listings " +
                    "ORDER BY created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            JSONArray listingsArray = new JSONArray();
            while (rs.next()) {
                JSONObject listing = new JSONObject();
                listing.put("id", rs.getInt("id"));
                listing.put("title", rs.getString("title"));
                listing.put("location", rs.getString("location"));
                listing.put("price", rs.getDouble("price"));
                listing.put("image_url", rs.getString("image_url"));
                listing.put("description", rs.getString("description"));
                listingsArray.put(listing);
            }

            out.print(listingsArray.toString());
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"An error occurred while fetching public listings: " + e.getMessage() + "\"}");
        }
    }
}