package com.queueapp.servlet;

import com.queueapp.dao.ShopDAO;
import com.queueapp.model.Shop;
import com.queueapp.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String shopName = trim(request.getParameter("shopName"));
        String category = trim(request.getParameter("category"));
        String username = trim(request.getParameter("username"));
        String password = request.getParameter("password");
        String avgServiceTimeStr = trim(request.getParameter("avgServiceTime"));

        if (isBlank(shopName) || isBlank(username) || isBlank(password) || isBlank(avgServiceTimeStr)) {
            fail(request, response, "Please fill in all required fields.");
            return;
        }

        int avgServiceTime;
        try {
            avgServiceTime = Integer.parseInt(avgServiceTimeStr);
            if (avgServiceTime <= 0 || avgServiceTime > 240) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            fail(request, response, "Average service time must be a number between 1 and 240 minutes.");
            return;
        }

        if (password.length() < 6) {
            fail(request, response, "Password must be at least 6 characters.");
            return;
        }

        try {
            if (shopDAO.findByUsername(username) != null) {
                fail(request, response, "That username is already taken. Please choose another.");
                return;
            }

            if (isBlank(category)) {
                category = "General";
            }

            Shop shop = new Shop(shopName, category, username, PasswordUtil.hash(password), avgServiceTime);
            shopDAO.create(shop);

            response.sendRedirect(request.getContextPath() + "/login.jsp?registered=true");
        } catch (SQLException e) {
            throw new ServletException("Database error during registration", e);
        }
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
