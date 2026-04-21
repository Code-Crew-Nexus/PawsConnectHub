package com.pawsconnect.controller;

import com.google.gson.Gson;
import com.pawsconnect.dao.ListingDAO;
import com.pawsconnect.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/marketplace")
public class MarketplaceServlet extends HttpServlet {

    private final ListingDAO listingDAO = new ListingDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try (PrintWriter out = res.getWriter()) {
            out.print(gson.toJson(listingDAO.getAllListings(false)));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return;
        }

        try {
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            Map<String, Object> data = gson.fromJson(body.toString(), Map.class);
            String animalType = clean((String) data.get("animalType"));
            String breed = clean((String) data.get("breed"));
            String age = clean((String) data.get("age"));
            String health = clean((String) data.get("health"));
            String description = clean((String) data.get("description"));
            String city = clean((String) data.get("city"));
            String phone = clean((String) data.get("phone"));
            double price = numberValue(data.get("price"));

            String title = !breed.isEmpty() && !animalType.isEmpty()
                    ? breed + " " + animalType
                    : (!breed.isEmpty() ? breed : animalType);

            if (title.trim().isEmpty()) {
                sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Animal type or breed is required");
                return;
            }
            if (price <= 0) {
                sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Price is required");
                return;
            }

            StringBuilder details = new StringBuilder(description);
            appendDetail(details, "Age", age);
            appendDetail(details, "Health", health);
            appendDetail(details, "City", city);
            appendDetail(details, "WhatsApp", phone);

            boolean success = listingDAO.createListing(user.getId(), title.trim(), details.toString(), price, null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put(success ? "message" : "error",
                    success ? "Listing submitted for review!" : "Failed to save listing");
            res.getWriter().print(gson.toJson(response));
        } catch (Exception e) {
            e.printStackTrace();
            sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private void appendDetail(StringBuilder details, String label, String value) {
        if (value == null || value.isEmpty()) return;
        if (details.length() > 0) details.append("\n");
        details.append(label).append(": ").append(value);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private double numberValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void sendError(HttpServletResponse res, int code, String message) throws IOException {
        res.setStatus(code);
        res.getWriter().print("{\"error\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
