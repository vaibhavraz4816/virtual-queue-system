package com.queueapp.servlet;

import com.queueapp.dao.ShopDAO;
import com.queueapp.dao.TokenDAO;
import com.queueapp.model.Shop;
import com.queueapp.model.Token;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/join")
public class JoinQueueServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();
    private final TokenDAO tokenDAO = new TokenDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int shopId;
        try {
            shopId = Integer.parseInt(request.getParameter("shopId"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/shops");
            return;
        }

        String customerName = trim(request.getParameter("customerName"));
        String customerPhone = trim(request.getParameter("customerPhone"));

        try {
            Shop shop = shopDAO.findById(shopId);
            if (shop == null || !shop.isOpen()) {
                response.sendRedirect(request.getContextPath() + "/shops?error=closed");
                return;
            }

            if (customerName == null || customerName.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/shop?id=" + shopId + "&error=name");
                return;
            }

            Token token = tokenDAO.create(shopId, customerName, customerPhone);
            response.sendRedirect(request.getContextPath() + "/myToken?tokenId=" + token.getTokenId());
        } catch (SQLException e) {
            throw new ServletException("Database error while joining the queue", e);
        }
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
