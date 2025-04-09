package com.propertyrental.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    // Handle CORS pre-flight requests (optional, if not handled by a global filter)
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // POST request to log out the user.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set response type and encoding.
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // In a typical stateless JWT architecture, there's no server-side token destruction.
        // Optionally: You could log the logout event or add the token to a blacklist.

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("message", "Logout successful");

        // Return success response.
        response.setStatus(HttpServletResponse.SC_OK);
        out.print(jsonResponse.toString());
    }
}