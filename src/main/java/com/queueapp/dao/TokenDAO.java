package com.queueapp.dao;

import com.queueapp.model.Token;
import com.queueapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything to do with the life cycle of a token:
 * WAITING -> CALLED -> SERVED
 *                 \-> SKIPPED (no-show, manual or automatic)
 *
 * Token numbering and the "call next" transition both need to be
 * race-condition-safe if two customers join at once, or if a shop
 * owner double-clicks "Call Next". This class uses a JVM-level lock
 * (fine for a single Tomcat instance, which is the assumption for
 * this project) plus a real DB transaction underneath it. Swapping
 * the lock for a `SELECT ... FOR UPDATE` is the natural upgrade if
 * this ever ran across multiple app server instances.
 */
public class TokenDAO {

    private static final Object QUEUE_LOCK = new Object();
    private static final int GRACE_PERIOD_MINUTES = 5;

    // ------------------------------------------------------------------
    // Joining the queue
    // ------------------------------------------------------------------

    public Token create(int shopId, String customerName, String customerPhone) throws SQLException {
        synchronized (QUEUE_LOCK) {
            LocalDate today = LocalDate.now();
            try (Connection conn = DBConnection.getConnection()) {
                int nextNumber = getNextTokenNumber(conn, shopId, today);

                String sql = "INSERT INTO tokens (shop_id, token_number, customer_name, customer_phone, status, queue_date) " +
                             "VALUES (?, ?, ?, ?, 'WAITING', ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, shopId);
                    ps.setInt(2, nextNumber);
                    ps.setString(3, customerName);
                    ps.setString(4, customerPhone);
                    ps.setDate(5, Date.valueOf(today));
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            return findById(keys.getInt(1));
                        }
                    }
                }
                throw new SQLException("Failed to create token - no ID returned");
            }
        }
    }

    private int getNextTokenNumber(Connection conn, int shopId, LocalDate date) throws SQLException {
        String sql = "SELECT COALESCE(MAX(token_number), 0) + 1 AS next_number " +
                     "FROM tokens WHERE shop_id = ? AND queue_date = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("next_number");
            }
        }
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    public Token findById(int tokenId) throws SQLException {
        String sql = "SELECT * FROM tokens WHERE token_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tokenId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** How many people are still ahead of this token in today's queue. */
    public int countPeopleAhead(int shopId, int tokenNumber, Date queueDate) throws SQLException {
        String sql = "SELECT COUNT(*) AS ahead FROM tokens " +
                     "WHERE shop_id = ? AND queue_date = ? AND token_number < ? " +
                     "AND status IN ('WAITING', 'CALLED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setDate(2, queueDate);
            ps.setInt(3, tokenNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("ahead");
            }
        }
    }

    public List<Token> findWaitingQueue(int shopId, LocalDate date) throws SQLException {
        String sql = "SELECT * FROM tokens WHERE shop_id = ? AND queue_date = ? " +
                     "AND status = 'WAITING' ORDER BY token_number ASC";
        List<Token> tokens = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tokens.add(mapRow(rs));
                }
            }
        }
        return tokens;
    }

    public Token findCurrentCalled(int shopId, LocalDate date) throws SQLException {
        String sql = "SELECT * FROM tokens WHERE shop_id = ? AND queue_date = ? " +
                     "AND status = 'CALLED' LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public int countByStatus(int shopId, LocalDate date, String status) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM tokens WHERE shop_id = ? AND queue_date = ? AND status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        }
    }

    /** Used by the background auto-skip job to find shops with an overdue CALLED token. */
    public List<Integer> findShopsWithExpiredCalledTokens() throws SQLException {
        String sql = "SELECT DISTINCT shop_id FROM tokens " +
                     "WHERE status = 'CALLED' AND grace_deadline IS NOT NULL AND grace_deadline < NOW()";
        List<Integer> shopIds = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                shopIds.add(rs.getInt("shop_id"));
            }
        }
        return shopIds;
    }

    // ------------------------------------------------------------------
    // State transitions (owner actions + the background scheduler)
    // ------------------------------------------------------------------

    /**
     * Shop owner clicks "Call Next": whoever is currently CALLED is marked
     * SERVED (the owner is done with them), then the next WAITING token
     * becomes CALLED with a fresh grace period. Returns the newly-called
     * token, or null if the queue is empty.
     */
    public Token callNext(int shopId) throws SQLException {
        synchronized (QUEUE_LOCK) {
            LocalDate today = LocalDate.now();
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    markCurrentCalledAs(conn, shopId, today, Token.SERVED, true);
                    Token next = advanceToNextWaiting(conn, shopId, today);
                    conn.commit();
                    return next;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }
    }

    /**
     * Shop owner clicks "Skip" (manual no-show), or the background
     * scheduler calls this for a token whose grace period has expired.
     * Marks the current CALLED token as SKIPPED, then advances the queue.
     */
    public Token skipCurrent(int shopId) throws SQLException {
        synchronized (QUEUE_LOCK) {
            LocalDate today = LocalDate.now();
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    markCurrentCalledAs(conn, shopId, today, Token.SKIPPED, false);
                    Token next = advanceToNextWaiting(conn, shopId, today);
                    conn.commit();
                    return next;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }
    }

    public void cancel(int tokenId) throws SQLException {
        String sql = "UPDATE tokens SET status = 'CANCELLED' WHERE token_id = ? AND status = 'WAITING'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tokenId);
            ps.executeUpdate();
        }
    }

    private void markCurrentCalledAs(Connection conn, int shopId, LocalDate date,
                                      String newStatus, boolean stampServedAt) throws SQLException {
        String sql = stampServedAt
                ? "UPDATE tokens SET status = ?, served_at = NOW() WHERE shop_id = ? AND queue_date = ? AND status = 'CALLED'"
                : "UPDATE tokens SET status = ? WHERE shop_id = ? AND queue_date = ? AND status = 'CALLED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, shopId);
            ps.setDate(3, Date.valueOf(date));
            ps.executeUpdate();
        }
    }

    private Token advanceToNextWaiting(Connection conn, int shopId, LocalDate date) throws SQLException {
        Token next = null;
        String findSql = "SELECT * FROM tokens WHERE shop_id = ? AND queue_date = ? " +
                          "AND status = 'WAITING' ORDER BY token_number ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(findSql)) {
            ps.setInt(1, shopId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    next = mapRow(rs);
                }
            }
        }

        if (next != null) {
            String updateSql = "UPDATE tokens SET status = 'CALLED', called_at = NOW(), " +
                                "grace_deadline = DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE token_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setInt(1, GRACE_PERIOD_MINUTES);
                ps.setInt(2, next.getTokenId());
                ps.executeUpdate();
            }
            next.setStatus(Token.CALLED);
        }
        return next;
    }

    // ------------------------------------------------------------------

    private Token mapRow(ResultSet rs) throws SQLException {
        Token token = new Token();
        token.setTokenId(rs.getInt("token_id"));
        token.setShopId(rs.getInt("shop_id"));
        token.setTokenNumber(rs.getInt("token_number"));
        token.setCustomerName(rs.getString("customer_name"));
        token.setCustomerPhone(rs.getString("customer_phone"));
        token.setStatus(rs.getString("status"));
        token.setQueueDate(rs.getDate("queue_date"));
        token.setJoinedAt(rs.getTimestamp("joined_at"));
        token.setCalledAt(rs.getTimestamp("called_at"));
        token.setGraceDeadline(rs.getTimestamp("grace_deadline"));
        token.setServedAt(rs.getTimestamp("served_at"));
        return token;
    }
}
