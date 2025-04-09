package com.propertyrental.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

@WebFilter("/*") // Apply to all requests
public class CORSFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Allow credentials (cookies, authorization headers)
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

        // Allow cross-origin requests from React frontend (localhost:3000)
        httpResponse.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");

        // Allow specific methods (GET, POST, DELETE, OPTIONS)
        httpResponse.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, OPTIONS, DELETE");

        // Allow specific headers (Content-Type, Authorization)
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Cache preflight response for 1 hour
        httpResponse.setHeader("Access-Control-Max-Age", "3600");

        // If this is a preflight request, handle it and return before passing it to the next filter
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Continue with the request chain
        chain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if necessary
    }

    public void destroy() {
        // Cleanup if necessary
    }
}