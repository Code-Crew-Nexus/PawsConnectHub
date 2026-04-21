package com.pawsconnect.controller;

import com.google.gson.Gson;
import com.pawsconnect.dao.PostDAO;
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

@WebServlet("/community")
public class CommunityServlet extends HttpServlet {

    private final PostDAO postDAO = new PostDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try (PrintWriter out = res.getWriter()) {
            out.print(gson.toJson(postDAO.getAllPosts(false)));
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
            String caption = clean((String) data.get("caption"));
            String category = clean((String) data.get("category"));
            String location = clean((String) data.get("location"));
            String tags = clean((String) data.get("tags"));

            if (caption.isEmpty()) {
                sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Caption is required");
                return;
            }

            StringBuilder content = new StringBuilder(caption);
            if (!category.isEmpty()) content.append("\n\nCategory: ").append(category);
            if (!location.isEmpty()) content.append("\nLocation: ").append(location);
            if (!tags.isEmpty()) content.append("\nTags: ").append(tags);

            boolean success = postDAO.createPost(user.getId(), content.toString());

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put(success ? "message" : "error",
                    success ? "Post submitted for review!" : "Failed to save post");
            res.getWriter().print(gson.toJson(response));
        } catch (Exception e) {
            e.printStackTrace();
            sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void sendError(HttpServletResponse res, int code, String message) throws IOException {
        res.setStatus(code);
        res.getWriter().print("{\"error\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
