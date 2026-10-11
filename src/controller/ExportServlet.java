package controller;

import exception.DatabaseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.AuditLog;
import model.Transaction;
import service.AdminMonitoringService;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Enterprise Compliance Export Controller generating RFC 4180-compliant CSV reports
 * for bank auditors, regulatory filings, and forensic investigation.
 */
@WebServlet(name = "ExportServlet", urlPatterns = {"/export/transactions", "/export/audit"})
public class ExportServlet extends HttpServlet {

    private final AdminMonitoringService adminService = new AdminMonitoringService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrative privileges required for compliance export.");
            return;
        }

        String path = req.getServletPath();
        resp.setContentType("text/csv; charset=UTF-8");

        try {
            if ("/export/transactions".equals(path)) {
                exportTransactions(resp);
            } else if ("/export/audit".equals(path)) {
                exportAuditTrail(resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Export endpoint not recognized.");
            }
        } catch (DatabaseException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database failure during export: " + e.getMessage());
        }
    }

    private void exportTransactions(HttpServletResponse resp) throws IOException, DatabaseException {
        resp.setHeader("Content-Disposition", "attachment; filename=\"fraud_transactions_export_" + System.currentTimeMillis() + ".csv\"");
        PrintWriter out = resp.getWriter();

        // CSV Header
        out.println("Transaction ID,User ID,Amount,Currency,Type,Location,IP Address,Device Fingerprint,Risk Score,Status,Decision Engine,Rule Breakdown,Timestamp");

        List<Transaction> list = adminService.getAllTransactions();
        for (Transaction tx : list) {
            out.printf("%s,%d,%.2f,%s,%s,%s,%s,%s,%.4f,%s,%s,%s,%s%n",
                    csvEscape(tx.getTransactionId()),
                    tx.getUserId(),
                    tx.getAmount() != null ? tx.getAmount().doubleValue() : 0.0,
                    csvEscape(tx.getCurrency()),
                    csvEscape(tx.getTransactionType() != null ? tx.getTransactionType().name() : ""),
                    csvEscape(tx.getLocation()),
                    csvEscape(tx.getIpAddress()),
                    csvEscape(tx.getDeviceFingerprint()),
                    tx.getRiskScore(),
                    tx.getStatus() != null ? tx.getStatus().name() : "",
                    csvEscape(tx.getDecisionEngine()),
                    csvEscape(tx.getEvaluationNotes()),
                    tx.getTimestamp() != null ? tx.getTimestamp().toString() : ""
            );
        }
        out.flush();
    }

    private void exportAuditTrail(HttpServletResponse resp) throws IOException, DatabaseException {
        resp.setHeader("Content-Disposition", "attachment; filename=\"compliance_audit_trail_" + System.currentTimeMillis() + ".csv\"");
        PrintWriter out = resp.getWriter();

        // CSV Header
        out.println("Audit ID,Action Type,Performed By,Target Entity/ID,Compliance Details,Timestamp");

        List<AuditLog> list = adminService.getRecentAuditLogs(500);
        for (AuditLog log : list) {
            out.printf("%d,%s,%s,%s,%s,%s%n",
                    log.getAuditId() != null ? log.getAuditId() : 0,
                    csvEscape(log.getActionType()),
                    csvEscape(log.getPerformedBy()),
                    csvEscape(log.getTargetId()),
                    csvEscape(log.getDetails()),
                    log.getTimestamp() != null ? log.getTimestamp().toString() : ""
            );
        }
        out.flush();
    }

    private String csvEscape(String text) {
        if (text == null) return "\"\"";
        String escaped = text.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
