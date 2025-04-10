package com.propertyrental.servlets;


import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/owner-dashboard")
public class OwnerDashboardServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = response.getWriter();

        String token = request.getHeader("Authorization").split(" ")[1];
        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing auth token\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String email = JWTUtil.getEmailFromToken(token);

            if (email == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\": \"Invalid or expired token\"}");
                return;
            }

            // Get landlord user_id
            String userQuery = "SELECT id FROM users WHERE email = ? AND account_type = 'property-owner'";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            userStmt.setString(1, email);
            ResultSet userRs = userStmt.executeQuery();

            if (!userRs.next()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                out.print("{\"error\": \"Unauthorized user or not a property owner\"}");
                return;
            }

            int ownerId = userRs.getInt("id");

            // Fetch listings for this user
            String listingsQuery = "SELECT * FROM listings WHERE user_id = ?";
            PreparedStatement listingStmt = conn.prepareStatement(listingsQuery);
            listingStmt.setInt(1, ownerId);
            ResultSet listingsRs = listingStmt.executeQuery();

            JSONArray listingsArr = new JSONArray();

            while (listingsRs.next()) {
                int listingId = listingsRs.getInt("id");

                JSONObject listing = new JSONObject();
                listing.put("id", listingId);
                listing.put("title", listingsRs.getString("title"));
                listing.put("location", listingsRs.getString("location"));
                listing.put("price", listingsRs.getDouble("price"));
                listing.put("image_url", listingsRs.getString("image_url"));

                // Fetch booking requests for this listing
                String bookingsQuery = "SELECT b.id, b.status, b.booking_date, u.email as tenant_email " +
                        "FROM bookings b JOIN users u ON b.user_id = u.id " +
                        "WHERE b.listing_id = ?";
                PreparedStatement bookingStmt = conn.prepareStatement(bookingsQuery);
                bookingStmt.setInt(1, listingId);
                ResultSet bookingsRs = bookingStmt.executeQuery();

                JSONArray bookingList = new JSONArray();
                while (bookingsRs.next()) {
                    JSONObject booking = new JSONObject();
                    booking.put("id", bookingsRs.getInt("id"));
                    booking.put("status", bookingsRs.getString("status"));
                    booking.put("bookingDate", bookingsRs.getString("booking_date"));
                    booking.put("tenantEmail", bookingsRs.getString("tenant_email"));

                    bookingList.put(booking);
                }

                listing.put("bookings", bookingList);
                listingsArr.put(listing);
            }

            out.print(listingsArr);
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Server error: " + e.getMessage() + "\"}");
        }
    }
}