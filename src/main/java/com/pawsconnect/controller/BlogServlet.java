package com.pawsconnect.controller;

import com.google.gson.Gson;
import com.pawsconnect.dao.BlogDAO;
import com.pawsconnect.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/blog")
public class BlogServlet extends HttpServlet {

    private BlogDAO blogDAO = new BlogDAO();
    private Gson gson = new Gson();

    /**
     * GET: Retrieve blog submissions
     * /blog?action=approved → get all approved blogs for public display
     * /blog?action=pending  → get pending submissions (user's own or admin)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        String action = req.getParameter("action");
        if (action == null) action = "approved";

        Object data;
        switch (action) {
            case "approved":
                data = blogDAO.getApprovedBlogs();
                break;
            case "pending":
                // Only show pending blogs to admins or the original poster
                HttpSession session = req.getSession(false);
                if (session == null) {
                    sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
                    return;
                }
                User user = (User) session.getAttribute("user");
                if (user != null && user.isAdmin()) {
                    data = blogDAO.getBlogSubmissions("pending");
                } else {
                    data = new java.util.ArrayList<>();
                }
                break;
            default:
                sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                return;
        }

        out.print(gson.toJson(data));
        out.flush();
    }

    /**
     * POST: Create a new blog submission
     * Body: JSON with title, content, topic, author_name, location
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        // Check authentication
        HttpSession session = req.getSession(false);
        if (session == null) {
            sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user == null) {
            sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "User not found in session");
            return;
        }

        try {
            // Parse JSON body
            StringBuilder sb = new StringBuilder();
            String line;
            java.io.BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Map<String, Object> data = gson.fromJson(sb.toString(), Map.class);

            String title = (String) data.get("title");
            String content = (String) data.get("body");
            String topic = (String) data.get("topic");
            String authorName = (String) data.get("author");
            String location = (String) data.get("location");

            // Validate
            if (title == null || title.trim().isEmpty()) {
                sendError(res, 400, "Title is required");
                return;
            }
            if (content == null || content.trim().isEmpty()) {
                sendError(res, 400, "Content is required");
                return;
            }
            if (authorName == null || authorName.trim().isEmpty()) {
                sendError(res, 400, "Author name is required");
                return;
            }

            // Create blog submission
            boolean success = blogDAO.createBlogSubmission(
                    user.getId(),
                    title.trim(),
                    content.trim(),
                    topic != null ? topic.trim() : "",
                    authorName.trim(),
                    location != null ? location.trim() : ""
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            if (success) {
                response.put("message", "Blog submitted for review!");
            } else {
                response.put("error", "Failed to save submission");
            }

            out.print(gson.toJson(response));
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
            sendError(res, 500, "Server error: " + e.getMessage());
        }
    }

    private void sendError(HttpServletResponse res, int code, String message) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.setStatus(code);
        res.getWriter().print("{\"error\":\"" + message + "\"}");
        res.getWriter().flush();
    }
}

