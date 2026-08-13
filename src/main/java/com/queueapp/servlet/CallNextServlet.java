package com.queueapp.servlet;

import com.queueapp.dao.TokenDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/callNext")
public class CallNextServlet extends HttpServlet {

    private final TokenDAO tokenDAO = new TokenDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("shopId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        int shopId = (int) session.getAttribute("shopId");
        try {
            tokenDAO.callNext(shopId);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            throw new ServletException("Database error while calling the next token", e);
        }
    }
}
