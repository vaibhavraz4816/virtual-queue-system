package com.queueapp.servlet;

import com.queueapp.dao.ShopDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/shops")
public class ShopListServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("shops", shopDAO.findAllOpen());
            request.getRequestDispatcher("/WEB-INF/jsp/shops.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Database error while loading shops", e);
        }
    }
}
