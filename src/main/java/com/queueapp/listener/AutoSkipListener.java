package com.queueapp.listener;

import com.queueapp.dao.TokenDAO;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts a background thread when the app boots that periodically checks
 * every shop for a "CALLED" token whose grace period has expired, and
 * automatically skips it so the queue keeps moving even if the shop
 * owner doesn't notice a no-show. This is the piece that makes the
 * queue self-healing instead of needing manual intervention.
 */
@WebListener
public class AutoSkipListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AutoSkipListener.class.getName());
    private static final int CHECK_INTERVAL_SECONDS = 30;

    private ScheduledExecutorService scheduler;
    private final TokenDAO tokenDAO = new TokenDAO();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "auto-skip-worker");
            thread.setDaemon(true);
            return thread;
        });

        scheduler.scheduleAtFixedRate(
                this::checkAndSkipExpiredTokens,
                CHECK_INTERVAL_SECONDS,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        LOGGER.info("AutoSkipListener started - checking for no-shows every " + CHECK_INTERVAL_SECONDS + "s");
    }

    private void checkAndSkipExpiredTokens() {
        try {
            List<Integer> shopIds = tokenDAO.findShopsWithExpiredCalledTokens();
            for (Integer shopId : shopIds) {
                tokenDAO.skipCurrent(shopId);
                LOGGER.info("Auto-skipped a no-show token for shop_id=" + shopId);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Auto-skip check failed - will retry on the next cycle", e);
        } catch (RuntimeException e) {
            // Never let an unexpected error kill the scheduled task permanently.
            LOGGER.log(Level.SEVERE, "Unexpected error in auto-skip worker", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler == null) {
            return;
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
