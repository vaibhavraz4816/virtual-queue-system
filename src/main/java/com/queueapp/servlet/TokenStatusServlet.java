package com.queueapp.servlet;

import com.google.gson.Gson;
import com.queueapp.dao.ShopDAO;
import com.queueapp.dao.TokenDAO;
import com.queueapp.dto.TokenStatusResponse;
import com.queueapp.model.Shop;
import com.queueapp.model.Token;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;

/**
 * GET /api/tokenStatus?tokenId=123
 * Polled every few seconds by my_token.jsp to show live position + wait time.
 */
@WebServlet("/api/tokenStatus")
public class TokenStatusServlet extends HttpServlet {

    private final TokenDAO tokenDAO = new TokenDAO();
    private final ShopDAO shopDAO = new ShopDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            int tokenId = Integer.parseInt(request.getParameter("tokenId"));
            Token token = tokenDAO.findById(tokenId);

            if (token == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writeError(response, "Token not found");
                return;
            }

            Shop shop = shopDAO.findById(token.getShopId());
            int peopleAhead = Token.WAITING.equals(token.getStatus())
                    ? tokenDAO.countPeopleAhead(token.getShopId(), token.getTokenNumber(), token.getQueueDate())
                    : 0;
            int estimatedWait = peopleAhead * shop.getAvgServiceTimeMins();

            TokenStatusResponse body = new TokenStatusResponse(
                    token.getTokenNumber(), token.getStatus(), peopleAhead, estimatedWait, shop.getShopName());

            response.getWriter().write(gson.toJson(body));

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(response, "Invalid tokenId");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeError(response, "Server error");
        }
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.getWriter().write(gson.toJson(Collections.singletonMap("error", message)));
    }
}
