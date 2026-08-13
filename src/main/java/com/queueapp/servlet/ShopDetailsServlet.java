package com.queueapp.servlet;

import com.queueapp.dao.ShopDAO;
import com.queueapp.dao.TokenDAO;
import com.queueapp.model.Shop;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

@WebServlet("/shop")
public class ShopDetailsServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();
    private final TokenDAO tokenDAO = new TokenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int shopId;
        try {
            shopId = Integer.parseInt(request.getParameter("id"));
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

            int waitingCount = tokenDAO.findWaitingQueue(shopId, LocalDate.now()).size();

            request.setAttribute("shop", shop);
            request.setAttribute("waitingCount", waitingCount);
            request.getRequestDispatcher("/WEB-INF/jsp/shop_details.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error while loading shop details", e);
        }
    }
}
