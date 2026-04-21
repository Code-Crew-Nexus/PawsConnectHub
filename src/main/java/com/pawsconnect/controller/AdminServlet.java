package com.pawsconnect.controller;

import com.google.gson.Gson;
import com.pawsconnect.dao.BlogDAO;
import com.pawsconnect.dao.ListingDAO;
import com.pawsconnect.dao.PostDAO;
import com.pawsconnect.dao.UserDAO;
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

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    private UserDAO    userDAO    = new UserDAO();
    private ListingDAO listingDAO = new ListingDAO();
    private PostDAO    postDAO    = new PostDAO();
    private BlogDAO    blogDAO    = new BlogDAO();
    private Gson       gson       = new Gson();

    // â”€â”€ Guard: called at top of every doGet/doPost â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private User requireAdmin(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in.");
            return null;
        }
        User user = (User) session.getAttribute("user");
        if (user == null || !user.isAdmin()) {
            sendError(res, HttpServletResponse.SC_FORBIDDEN, "Admin access required.");
            return null;
        }
        return user;
    }

    // â”€â”€ GET â€” read operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // /admin?action=users            â†’ list all users
    // /admin?action=listings         â†’ list all listings (all statuses)
    // /admin?action=posts            â†’ list all posts
    // /admin?action=blog-submissions â†’ list pending blog submissions
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        if (requireAdmin(req, res) == null) return;

        String action = req.getParameter("action");
        if (action == null) action = "";

        Object data;
        switch (action) {
            case "users":               data = userDAO.getAllUsers();                    break;
            case "listings":            data = listingDAO.getAllListings(true);          break;
            case "posts":               data = postDAO.getAllPosts(true);                break;
            case "blog-submissions":    data = blogDAO.getBlogSubmissions(null);         break;
            case "blog-pending":        data = blogDAO.getBlogSubmissions("pending");    break;
            default:
                sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                return;
        }

        out.print(gson.toJson(data));
        out.flush();
    }

    // â”€â”€ POST â€” write operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // action=updateUserRole   â†’ params: userId, role
    // action=deleteUser       â†’ params: userId
    // action=updateListing    â†’ params: listingId, status  (approve/reject/pending)
    // action=deleteListing    â†’ params: listingId
    // action=deletePost       â†’ params: postId
    // action=updatePost       â†’ params: postId, content
    // action=approveBlog      â†’ params: submissionId
    // action=rejectBlog       â†’ params: submissionId
    // action=deleteBlog       â†’ params: submissionId
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        PrintWriter out = res.getWriter();

        if (requireAdmin(req, res) == null) return;

        String action = req.getParameter("action");
        if (action == null) {
            sendError(res, HttpServletResponse.SC_BAD_REQUEST, "action parameter missing.");
            return;
        }

        boolean ok = false;

        switch (action) {

            // â”€â”€ USER CRUD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            case "updateUserRole": {
                int    userId  = Integer.parseInt(req.getParameter("userId"));
                String newRole = req.getParameter("role");
                if (!newRole.equals("user") && !newRole.equals("admin")) {
                    sendError(res, 400, "role must be 'user' or 'admin'.");
                    return;
                }
                ok = userDAO.updateUserRole(userId, newRole);
                break;
            }
            case "updateUser": {
                int userId = Integer.parseInt(req.getParameter("userId"));
                String fullName = req.getParameter("fullName");
                String email = req.getParameter("email");
                String role = req.getParameter("role");
                if (fullName == null || fullName.trim().isEmpty()
                        || email == null || email.trim().isEmpty()) {
                    sendError(res, 400, "Name and email are required.");
                    return;
                }
                if (!"user".equals(role) && !"admin".equals(role)) {
                    sendError(res, 400, "role must be 'user' or 'admin'.");
                    return;
                }
                ok = userDAO.updateUserDetails(userId, fullName.trim(), email.trim(), role);
                break;
            }
            case "deleteUser": {
                int userId = Integer.parseInt(req.getParameter("userId"));
                ok = userDAO.deleteUser(userId);
                break;
            }

            // â”€â”€ LISTING CRUD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            case "updateListing": {
                int    listingId = Integer.parseInt(req.getParameter("listingId"));
                String status    = req.getParameter("status"); // approved / rejected / pending
                ok = listingDAO.updateListingStatus(listingId, status);
                break;
            }
            case "updateListingDetails": {
                int listingId = Integer.parseInt(req.getParameter("listingId"));
                String title = req.getParameter("title");
                String description = req.getParameter("description");
                String status = req.getParameter("status");
                double price = Double.parseDouble(req.getParameter("price"));
                if (title == null || title.trim().isEmpty() || price <= 0) {
                    sendError(res, 400, "Title and valid price are required.");
                    return;
                }
                if (!"pending".equals(status) && !"approved".equals(status) && !"rejected".equals(status)) {
                    sendError(res, 400, "Invalid listing status.");
                    return;
                }
                ok = listingDAO.updateListingDetails(listingId, title.trim(),
                        description == null ? "" : description.trim(), price, status);
                break;
            }
            case "deleteListing": {
                int listingId = Integer.parseInt(req.getParameter("listingId"));
                ok = listingDAO.deleteListing(listingId);
                break;
            }

            // â”€â”€ POST CRUD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            case "deletePost": {
                int postId = Integer.parseInt(req.getParameter("postId"));
                ok = postDAO.deletePost(postId);
                break;
            }
            case "updatePost": {
                int    postId     = Integer.parseInt(req.getParameter("postId"));
                String newContent = req.getParameter("content");
                ok = postDAO.updatePost(postId, newContent);
                break;
            }
            case "updatePostStatus": {
                int    postId = Integer.parseInt(req.getParameter("postId"));
                String status = req.getParameter("status");
                ok = postDAO.updatePostStatus(postId, status);
                break;
            }

            // â”€â”€ BLOG SUBMISSION CRUD â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            case "approveBlog": {
                int submissionId = Integer.parseInt(req.getParameter("submissionId"));
                ok = blogDAO.updateBlogSubmissionStatus(submissionId, "approved");
                break;
            }
            case "rejectBlog": {
                int submissionId = Integer.parseInt(req.getParameter("submissionId"));
                ok = blogDAO.updateBlogSubmissionStatus(submissionId, "rejected");
                break;
            }
            case "deleteBlog": {
                int submissionId = Integer.parseInt(req.getParameter("submissionId"));
                ok = blogDAO.deleteBlogSubmission(submissionId);
                break;
            }
            case "updateBlog": {
                int submissionId = Integer.parseInt(req.getParameter("submissionId"));
                String title = req.getParameter("title");
                String content = req.getParameter("content");
                String topic = req.getParameter("topic");
                String authorName = req.getParameter("authorName");
                String location = req.getParameter("location");
                String status = req.getParameter("status");
                if (title == null || title.trim().isEmpty()
                        || content == null || content.trim().isEmpty()) {
                    sendError(res, 400, "Title and content are required.");
                    return;
                }
                if (!"pending".equals(status) && !"approved".equals(status) && !"rejected".equals(status)) {
                    sendError(res, 400, "Invalid blog status.");
                    return;
                }
                ok = blogDAO.updateBlogSubmission(submissionId, title.trim(), content.trim(),
                        topic == null ? "" : topic.trim(),
                        authorName == null ? "" : authorName.trim(),
                        location == null ? "" : location.trim(), status);
                break;
            }

            default:
                sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", ok);
        out.print(gson.toJson(result));
        out.flush();
    }

    private void sendError(HttpServletResponse res, int code, String message) throws IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.setStatus(code);
        res.getWriter().print("{\"error\":\"" + message + "\"}");
        res.getWriter().flush();
    }
}
