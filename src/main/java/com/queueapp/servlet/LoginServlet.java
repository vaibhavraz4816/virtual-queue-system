package com.queueapp.servlet;

import com.queueapp.dao.ShopDAO;
import com.queueapp.model.Shop;
import com.queueapp.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        try {
            Shop shop = shopDAO.findByUsername(username);
            if (shop == null || password == null || !PasswordUtil.verify(password, shop.getPasswordHash())) {
                request.setAttribute("error", "Invalid username or password.");
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                return;
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("shopId", shop.getShopId());
            session.setAttribute("shopName", shop.getShopName());
            session.setMaxInactiveInterval(60 * 60); // 1 hour

            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            throw new ServletException("Database error during login", e);
        }
    }
}
