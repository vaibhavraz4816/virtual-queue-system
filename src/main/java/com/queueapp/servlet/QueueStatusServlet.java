package com.queueapp.servlet;

import com.google.gson.Gson;
import com.queueapp.dao.ShopDAO;
import com.queueapp.dao.TokenDAO;
import com.queueapp.dto.QueueStatusResponse;
import com.queueapp.model.Shop;
import com.queueapp.model.Token;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /api/queueStatus?shopId=5
 * Polled by the public "Now Serving" display screen and by the shop
 * owner's dashboard, so both views stay in sync even when the
 * background auto-skip job changes the queue on its own.
 * Intentionally exposes only token numbers, never customer names/phones.
 */
@WebServlet("/api/queueStatus")
public class QueueStatusServlet extends HttpServlet {

    private final ShopDAO shopDAO = new ShopDAO();
    private final TokenDAO tokenDAO = new TokenDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            int shopId = Integer.parseInt(request.getParameter("shopId"));
            Shop shop = shopDAO.findById(shopId);

            if (shop == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Shop not found")));
                return;
            }

            LocalDate today = LocalDate.now();
            Token current = tokenDAO.findCurrentCalled(shopId, today);
            List<Token> waiting = tokenDAO.findWaitingQueue(shopId, today);
            List<Integer> waitingNumbers = waiting.stream()
                    .map(Token::getTokenNumber)
                    .collect(Collectors.toList());

            QueueStatusResponse body = new QueueStatusResponse(
                    shop.getShopName(),
                    shop.isOpen(),
                    current != null ? current.getTokenNumber() : null,
                    waitingNumbers,
                    shop.getAvgServiceTimeMins()
            );

            response.getWriter().write(gson.toJson(body));

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Invalid shopId")));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Server error")));
        }
    }
}
