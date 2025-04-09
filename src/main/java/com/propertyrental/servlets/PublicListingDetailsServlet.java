package com.propertyrental.servlets;

import com.propertyrental.config.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/public-listing")
public class PublicListingDetailsServlet extends HttpServlet {

    // Optionally handle CORS preflight requests (public endpoint)
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // GET: Fetch public details for a listing with a given listingId.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Retrieve the listingId query parameter
        String listingIdStr = request.getParameter("listingId");
        if (listingIdStr == null || listingIdStr.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Missing listingId parameter\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Query the database to fetch listing details along with the property owner's name and email.
            String query = "SELECT l.id, l.title, l.description, l.location, l.price, l.amenities, l.image_url, " +
                    "       l.created_at, l.updated_at, u.name AS owner_name, u.email AS owner_email " +
                    "FROM listings l " +
                    "JOIN users u ON l.user_id = u.id " +
                    "WHERE l.id = ?";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(listingIdStr));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                JSONObject listing = new JSONObject();
                listing.put("id", rs.getInt("id"));
                listing.put("title", rs.getString("title"));
                listing.put("description", rs.getString("description"));
                listing.put("location", rs.getString("location"));
                listing.put("price", rs.getDouble("price"));
                listing.put("amenities", rs.getString("amenities"));
                listing.put("image_url", rs.getString("image_url"));
                listing.put("created_at", rs.getString("created_at"));
                listing.put("updated_at", rs.getString("updated_at"));

                // Build owner (landlord) JSON object.
                JSONObject owner = new JSONObject();
                owner.put("name", rs.getString("owner_name"));
                owner.put("email", rs.getString("owner_email"));
                listing.put("landlord", owner);

                out.print(listing.toString());
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Listing not found\"}");
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database error: " + e.getMessage() + "\"}");
        }
    }
}