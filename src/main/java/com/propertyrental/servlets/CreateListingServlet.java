package com.propertyrental.servlets;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.propertyrental.config.DBConnection;
import com.propertyrental.config.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@WebServlet("/create-listing")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,   // 2MB threshold
        maxFileSize = 1024 * 1024 * 10,          // 10MB per file
        maxRequestSize = 1024 * 1024 * 50        // 50MB total request size
)
public class CreateListingServlet extends HttpServlet {

    private Cloudinary cloudinary;

    @Override
    public void init() throws ServletException {
        // Initialize Cloudinary with your credentials.
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dia4uo7jz",   // Replace with your Cloudinary cloud name
                "api_key", "522619549394986",         // Replace with your Cloudinary API key
                "api_secret", "Kpot5rJ7FdpvslxMNAnCFDKkLuk"    // Replace with your Cloudinary API secret
        ));
        System.out.println("Initialized Cloudinary with config: " + cloudinary.config.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Set response content type and encoding.
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Validate the Authorization header and obtain the token.
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty() || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Missing or invalid auth token.\"}");
            return;
        }
        String token = authHeader.substring(7);
        String ownerId = JWTUtil.getUserIdFromToken(token);
        if (ownerId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Invalid or expired token.\"}");
            return;
        }

        // Retrieve listing details from request parameters.
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String location = request.getParameter("location");
        String priceStr = request.getParameter("price");
        String amenities = request.getParameter("amenities");

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

        // Process multiple image file uploads using Cloudinary (expecting input field name "images")
        Collection<Part> parts = request.getParts();
        List<String> imageUrls = new ArrayList<>();

        // Log received parts for debugging.
        for (Part part : parts) {
            System.out.println("Received part: name = " + part.getName() + ", size = " + part.getSize());
        }

        // Process each part named "images"
        for (Part part : parts) {
            if ("images".equals(part.getName()) && part.getSize() > 0) {
                try (InputStream inputStream = part.getInputStream()) {
                    // Use IOUtils to convert InputStream to byte array (works on Java 8)
                    byte[] fileBytes = IOUtils.toByteArray(inputStream);
                    // Upload the file bytes to Cloudinary with "resource_type" set to "auto"
                    Map uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap("resource_type", "auto"));
                    String secureUrl = (String) uploadResult.get("secure_url");
                    System.out.println("Uploaded image URL: " + secureUrl);
                    if (secureUrl != null) {
                        imageUrls.add(secureUrl);
                    }
                } catch (Exception e) {
                    System.err.println("Error uploading file to Cloudinary: " + e.getMessage());
                }
            }
        }
        // Join the URLs into a comma-separated string
        String imagesStr = String.join(",", imageUrls);
        System.out.println("Final image URLs: " + imagesStr);

        // Insert the new listing record into the database
        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO listings (title, description, location, price, amenities, image_url, user_id, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, title);
                stmt.setString(2, description);
                stmt.setString(3, location);
                stmt.setDouble(4, price);
                stmt.setString(5, amenities);
                stmt.setString(6, imagesStr);
                stmt.setInt(7, Integer.parseInt(ownerId));

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("message", "Listing created successfully.");
                    jsonResponse.put("image_url", imagesStr);
                    out.print(jsonResponse);
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JSONObject jsonResponse = new JSONObject();
                    jsonResponse.put("error", "Failed to create listing.");
                    out.print(jsonResponse);
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"An error occurred while creating the listing: " + e.getMessage() + "\"}");
        }
    }
}