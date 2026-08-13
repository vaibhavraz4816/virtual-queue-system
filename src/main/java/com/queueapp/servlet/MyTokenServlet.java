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

@WebServlet("/myToken")
public class MyTokenServlet extends HttpServlet {

    private final TokenDAO tokenDAO = new TokenDAO();
    private final ShopDAO shopDAO = new ShopDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int tokenId;
        try {
            tokenId = Integer.parseInt(request.getParameter("tokenId"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/shops");
            return;
        }

        try {
            Token token = tokenDAO.findById(tokenId);
            if (token == null) {
                response.sendRedirect(request.getContextPath() + "/shops?error=notfound");
                return;
            }

            Shop shop = shopDAO.findById(token.getShopId());
            int peopleAhead = Token.WAITING.equals(token.getStatus())
                    ? tokenDAO.countPeopleAhead(token.getShopId(), token.getTokenNumber(), token.getQueueDate())
                    : 0;

            request.setAttribute("token", token);
            request.setAttribute("shop", shop);
            request.setAttribute("peopleAhead", peopleAhead);
            request.setAttribute("estimatedWait", peopleAhead * shop.getAvgServiceTimeMins());
            request.getRequestDispatcher("/WEB-INF/jsp/my_token.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error while loading your token", e);
        }
    }
}
