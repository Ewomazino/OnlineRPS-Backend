package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/listings")
public class ListingsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        List<JSONObject> listings = new ArrayList<>();
        // Check if an 'id' parameter is provided for filtering
        String idParam = request.getParameter("id");

        try (Connection con = DBConnection.getConnection()) {
            // Base SQL query with a subquery to count approved bookings
            String sql = "SELECT l.*, " +
                    "       (SELECT COUNT(*) FROM bookings b WHERE b.listing_id = l.id AND b.status = 'Approved') AS approved_count " +
                    "FROM listings l";
            if (idParam != null && !idParam.isEmpty()) {
                sql += " WHERE l.id = ?";
            }
            PreparedStatement stmt = con.prepareStatement(sql);
            if (idParam != null && !idParam.isEmpty()) {
                stmt.setInt(1, Integer.parseInt(idParam));
            }
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject listing = new JSONObject();
                listing.put("id", rs.getInt("id"));
                listing.put("title", rs.getString("title"));
                listing.put("description", rs.getString("description"));
                listing.put("location", rs.getString("location"));
                listing.put("price", rs.getDouble("price"));
                listing.put("amenities", rs.getString("amenities"));
                listing.put("owner_email", rs.getString("owner_email"));
                listing.put("created_at", rs.getString("created_at"));
                listing.put("updated_at", rs.getString("updated_at"));
                // Include image_url field
                listing.put("image_url", rs.getString("image_url"));

                int approvedCount = rs.getInt("approved_count");
                listing.put("approved", approvedCount > 0);
                listings.add(listing);
            }

            if (idParam != null && !idParam.isEmpty() && listings.size() > 0) {
                out.print(listings.get(0).toString());
            } else {
                JSONArray listingsArray = new JSONArray(listings);
                out.print(listingsArray.toString());
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"An error occurred while fetching listings: " + e.getMessage() + "\"}");
        }
    }
    // Handle POST requests to create a new property listing (unchanged)
//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//
//        String title = request.getParameter("title");
//        String description = request.getParameter("description");
//        String location = request.getParameter("location");
//        double price = Double.parseDouble(request.getParameter("price"));
//        String amenities = request.getParameter("amenities");
//        String ownerEmail = request.getParameter("owner_email");
//
//        try (Connection con = DBConnection.getConnection()) {
//            String sql = "INSERT INTO listings (title, description, location, price, amenities, owner_email) VALUES (?, ?, ?, ?, ?, ?)";
//            PreparedStatement stmt = con.prepareStatement(sql);
//            stmt.setString(1, title);
//            stmt.setString(2, description);
//            stmt.setString(3, location);
//            stmt.setDouble(4, price);
//            stmt.setString(5, amenities);
//            stmt.setString(6, ownerEmail);
//            int rowsAffected = stmt.executeUpdate();
//
//            JSONObject jsonResponse = new JSONObject();
//            if (rowsAffected > 0) {
//                response.setStatus(HttpServletResponse.SC_CREATED);
//                jsonResponse.put("message", "Listing created successfully.");
//            } else {
//                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//                jsonResponse.put("error", "Failed to create listing.");
//            }
//            response.getWriter().write(jsonResponse.toString());
//        } catch (Exception e) {
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            response.getWriter().write("{\"error\": \"An error occurred while creating the listing: " + e.getMessage() + "\"}");
//        }
//    }
}