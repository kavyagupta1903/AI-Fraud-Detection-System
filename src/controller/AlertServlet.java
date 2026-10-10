package controller;

import exception.DatabaseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.FraudAlert;
import service.AdminMonitoringService;
import service.TransactionService;

import java.io.IOException;
import java.util.List;

/**
 * Controller managing alerts and notifications for suspicious activities.
 */
@WebServlet(name = "AlertServlet", urlPatterns = {"/alerts"})
public class AlertServlet extends HttpServlet {

    private final AdminMonitoringService adminService = new AdminMonitoringService();
    private final TransactionService transactionService = new TransactionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String role = (String) session.getAttribute("role");
        Long userId = (Long) session.getAttribute("userId");

        try {
            List<FraudAlert> alerts;
            if ("ADMIN".equals(role)) {
                alerts = adminService.getAllAlerts();
            } else {
                alerts = transactionService.getUserAlerts(userId);
            }

            req.setAttribute("alerts", alerts);
            req.getRequestDispatcher("/alerts.jsp").forward(req, resp);
        } catch (DatabaseException e) {
            req.setAttribute("errorMessage", "Error fetching alerts: " + e.getMessage());
            req.getRequestDispatcher("/alerts.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String alertIdStr = req.getParameter("alertId");
        if (alertIdStr != null) {
            try {
                Long alertId = Long.parseLong(alertIdStr.trim());
                adminService.markAlertAsRead(alertId);
            } catch (Exception ignored) {
            }
        }

        resp.sendRedirect(req.getContextPath() + "/alerts");
    }
}
