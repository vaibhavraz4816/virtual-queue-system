package com.queueapp.servlet;

import com.queueapp.dao.ShopDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/updateSettings")
public class UpdateSettingsServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("shopId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int shopId = (int) session.getAttribute("shopId");
        boolean isOpen = "on".equals(request.getParameter("isOpen"));

        int avgServiceTime;
        try {
            avgServiceTime = Integer.parseInt(request.getParameter("avgServiceTime"));
            if (avgServiceTime <= 0 || avgServiceTime > 240) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard?error=settings");
            return;
        }

        try {
            shopDAO.updateSettings(shopId, avgServiceTime, isOpen);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            throw new ServletException("Database error while updating settings", e);
        }
    }
}
