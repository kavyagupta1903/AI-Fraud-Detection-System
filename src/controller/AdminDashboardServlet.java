package controller;

import exception.DatabaseException;
import exception.ThresholdOutOfBoundsException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Transaction;
import service.AdminMonitoringService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller providing enterprise administration dashboard, dynamic threshold/weight tuning,
 * and fraud investigation actions.
 */
@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/admin/dashboard", "/admin/actions"})
public class AdminDashboardServlet extends HttpServlet {

    private final AdminMonitoringService adminService = new AdminMonitoringService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            resp.sendRedirect(req.getContextPath() + "/login?msg=admin_required");
            return;
        }

        try {
            String statusFilter = req.getParameter("status");
            if (statusFilter == null || statusFilter.isBlank()) {
                statusFilter = "ALL";
            }

            Map<String, Object> metrics = adminService.getSystemMetricsSnapshot();
            List<Transaction> transactions = adminService.getTransactionsFiltered(statusFilter);

            req.setAttribute("metrics", metrics);
            req.setAttribute("transactions", transactions);
            req.setAttribute("currentFilter", statusFilter);

            req.getRequestDispatcher("/admin-dashboard.jsp").forward(req, resp);
        } catch (DatabaseException e) {
            req.setAttribute("errorMessage", "Error loading admin dashboard: " + e.getMessage());
            req.getRequestDispatcher("/admin-dashboard.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String adminUser = (String) session.getAttribute("username");
        String action = req.getParameter("action");

        try {
            if ("update_thresholds".equals(action)) {
                double flag = Double.parseDouble(req.getParameter("flagThreshold"));
                double block = Double.parseDouble(req.getParameter("blockThreshold"));
                adminService.updateRiskThresholds(flag, block, adminUser);
                session.setAttribute("flashSuccess", "Risk thresholds updated successfully.");

            } else if ("update_weights".equals(action)) {
                double wAmount = Double.parseDouble(req.getParameter("weightAmount"));
                double wLoc = Double.parseDouble(req.getParameter("weightLocation"));
                double wDev = Double.parseDouble(req.getParameter("weightDevice"));
                double wVel = Double.parseDouble(req.getParameter("weightVelocity"));
                adminService.updateAlgorithmWeights(wAmount, wLoc, wDev, wVel, adminUser);
                session.setAttribute("flashSuccess", "Algorithm feature weights updated successfully.");

            } else if ("switch_engine".equals(action)) {
                String engineName = req.getParameter("engineName");
                adminService.setActiveEngine(engineName, adminUser);
                session.setAttribute("flashSuccess", "Active detection engine switched to: " + engineName);

            } else if ("approve".equals(action)) {
                String txId = req.getParameter("transactionId");
                boolean done = adminService.reviewApproveTransaction(txId, adminUser);
                if (done) {
                    session.setAttribute("flashSuccess", "Transaction " + txId + " approved. False-positive metric updated.");
                }

            } else if ("block".equals(action)) {
                String txId = req.getParameter("transactionId");
                String reason = req.getParameter("reason");
                boolean done = adminService.reviewBlockTransaction(txId, adminUser, reason);
                if (done) {
                    session.setAttribute("flashSuccess", "Transaction " + txId + " marked as BLOCKED.");
                }

            } else if ("investigate".equals(action)) {
                String txId = req.getParameter("transactionId");
                boolean done = adminService.reviewInvestigateTransaction(txId, adminUser);
                if (done) {
                    session.setAttribute("flashSuccess", "Transaction " + txId + " flagged for active investigation.");
                }
            } else if ("recalculate".equals(action)) {
                // Triggers cache and engine re-tuning
                config.AppConfig.getInstance().refreshFromDatabase();
                session.setAttribute("flashSuccess", "Algorithm parameters & baseline cache recalculated.");
            }

        } catch (ThresholdOutOfBoundsException tobe) {
            session.setAttribute("flashError", "Threshold Validation Error: " + tobe.getMessage());
        } catch (NumberFormatException nfe) {
            session.setAttribute("flashError", "Numeric format error in configuration inputs.");
        } catch (Exception e) {
            session.setAttribute("flashError", "Action failed: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }
}
