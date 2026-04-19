package com.pawsconnect.controller;

import com.google.gson.Gson;
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
    private Gson       gson       = new Gson();

    // ── Guard: called at top of every doGet/doPost ────────────────────────────
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

    // ── GET — read operations ─────────────────────────────────────────────────
    // /admin?action=users            → list all users
    // /admin?action=listings         → list all listings (all statuses)
    // /admin?action=posts            → list all posts
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
            case "users":    data = userDAO.getAllUsers();          break;
            case "listings": data = listingDAO.getAllListings(true); break;
            case "posts":    data = postDAO.getAllPosts(true);       break;
            default:
                sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
                return;
        }

        out.print(gson.toJson(data));
        out.flush();
    }

    // ── POST — write operations ───────────────────────────────────────────────
    // action=updateUserRole   → params: userId, role
    // action=deleteUser       → params: userId
    // action=updateListing    → params: listingId, status  (approve/reject/pending)
    // action=deleteListing    → params: listingId
    // action=deletePost       → params: postId
    // action=updatePost       → params: postId, content
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

            // ── USER CRUD ─────────────────────────────────────────────────────
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
            case "deleteUser": {
                int userId = Integer.parseInt(req.getParameter("userId"));
                ok = userDAO.deleteUser(userId);
                break;
            }

            // ── LISTING CRUD ──────────────────────────────────────────────────
            case "updateListing": {
                int    listingId = Integer.parseInt(req.getParameter("listingId"));
                String status    = req.getParameter("status"); // approved / rejected / pending
                ok = listingDAO.updateListingStatus(listingId, status);
                break;
            }
            case "deleteListing": {
                int listingId = Integer.parseInt(req.getParameter("listingId"));
                ok = listingDAO.deleteListing(listingId);
                break;
            }

            // ── POST CRUD ─────────────────────────────────────────────────────
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