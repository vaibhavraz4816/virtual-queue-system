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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();
    private final TokenDAO tokenDAO = new TokenDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("shopId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int shopId = (int) session.getAttribute("shopId");

        try {
            Shop shop = shopDAO.findById(shopId);
            LocalDate today = LocalDate.now();
            Token current = tokenDAO.findCurrentCalled(shopId, today);
            List<Token> waiting = tokenDAO.findWaitingQueue(shopId, today);
            int servedToday = tokenDAO.countByStatus(shopId, today, Token.SERVED);
            int skippedToday = tokenDAO.countByStatus(shopId, today, Token.SKIPPED);

            request.setAttribute("shop", shop);
            request.setAttribute("currentToken", current);
            request.setAttribute("waitingList", waiting);
            request.setAttribute("servedToday", servedToday);
            request.setAttribute("skippedToday", skippedToday);
            request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error while loading the dashboard", e);
        }
    }
}
