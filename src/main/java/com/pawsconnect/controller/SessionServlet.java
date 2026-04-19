package com.pawsconnect.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pawsconnect.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/session")
public class SessionServlet extends HttpServlet {

    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        JsonObject jsonResponse = new JsonObject();

        if (session != null && session.getAttribute("user") != null) {
            User user = (User) session.getAttribute("user");

            jsonResponse.addProperty("loggedIn", true);

            JsonObject userObj = new JsonObject();
            userObj.addProperty("id",       user.getId());
            userObj.addProperty("fullName", user.getFullName());
            userObj.addProperty("email",    user.getEmail());
            userObj.addProperty("role",     user.getRole()); // ← ADDED: auth.js needs this

            jsonResponse.add("user", userObj);
        } else {
            jsonResponse.addProperty("loggedIn", false);
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
}