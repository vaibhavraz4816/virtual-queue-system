package com.queueapp.servlet;

import com.queueapp.dao.ShopDAO;
import com.queueapp.model.Shop;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/display")
public class DisplayServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int shopId;
        try {
            shopId = Integer.parseInt(request.getParameter("shopId"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/shops");
            return;
        }

        try {
            Shop shop = shopDAO.findById(shopId);
            if (shop == null) {
                response.sendRedirect(request.getContextPath() + "/shops");
                return;
            }
            request.setAttribute("shop", shop);
            request.getRequestDispatcher("/WEB-INF/jsp/display.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error while loading the display screen", e);
        }
    }
}
