package controller;

import exception.DatabaseException;
import exception.FraudDetectionException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.CreditCardTransaction;
import model.FraudAlert;
import model.Transaction;
import model.User;
import model.WireTransaction;
import service.TransactionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Controller handling user payments and historical ledger view.
 */
@WebServlet(name = "TransactionServlet", urlPatterns = {"/transactions", "/transactions/new", "/user/dashboard"})
public class TransactionServlet extends HttpServlet {

    private final TransactionService transactionService = new TransactionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String path = req.getServletPath();
        Long userId = (Long) session.getAttribute("userId");

        try {
            if ("/transactions/new".equals(path)) {
                req.getRequestDispatcher("/new-transaction.jsp").forward(req, resp);
                return;
            }

            // Default to user dashboard with historical transactions
            List<Transaction> transactions = transactionService.getUserTransactions(userId);
            List<FraudAlert> alerts = transactionService.getUserAlerts(userId);

            req.setAttribute("transactions", transactions);
            req.setAttribute("alerts", alerts);
            req.getRequestDispatcher("/user-dashboard.jsp").forward(req, resp);
        } catch (DatabaseException e) {
            req.setAttribute("errorMessage", "Failed to retrieve ledger data: " + e.getMessage());
            req.getRequestDispatcher("/user-dashboard.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        String paymentType = req.getParameter("paymentType");
        String amountStr = req.getParameter("amount");
        String currency = req.getParameter("currency");
        String location = req.getParameter("location");
        String deviceFp = req.getParameter("deviceFingerprint");
        String category = req.getParameter("category");
        boolean isAsync = "true".equalsIgnoreCase(req.getParameter("asyncEvaluation"));

        // Fallback IP resolution
        String ipAddress = req.getParameter("ipAddress");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = req.getRemoteAddr();
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            Transaction tx;

            if ("WIRE_TRANSFER".equalsIgnoreCase(paymentType)) {
                String recipientBank = req.getParameter("recipientBank");
                String recipientAccount = req.getParameter("recipientAccount");
                String swiftCode = req.getParameter("swiftCode");
                String wirePurpose = req.getParameter("wirePurpose");

                tx = new WireTransaction(null, userId, amount, currency, location, ipAddress,
                        deviceFp, category, recipientBank, recipientAccount, swiftCode, wirePurpose);
            } else {
                // Default CREDIT_CARD
                String cardNumber = req.getParameter("cardNumber");
                String cardLast4 = "0000";
                if (cardNumber != null && cardNumber.replaceAll("\\s+", "").length() >= 4) {
                    String clean = cardNumber.replaceAll("\\s+", "");
                    cardLast4 = clean.substring(clean.length() - 4);
                } else if (req.getParameter("cardNumberLast4") != null) {
                    cardLast4 = req.getParameter("cardNumberLast4").trim();
                }

                String cardNetwork = req.getParameter("cardNetwork");
                boolean cvvVerified = "true".equalsIgnoreCase(req.getParameter("cvvVerified"));
                String billingZip = req.getParameter("billingZip");

                tx = new CreditCardTransaction(null, userId, amount, currency, location, ipAddress,
                        deviceFp, category, cardLast4, cardNetwork, cvvVerified, billingZip);
            }

            if (isAsync) {
                // Asynchronous Producer-Consumer pipeline
                boolean enqueued = transactionService.submitTransactionAsync(tx);
                if (enqueued) {
                    req.setAttribute("successMessage", "Transaction " + tx.getTransactionId() +
                            " successfully enqueued for asynchronous worker evaluation.");
                } else {
                    req.setAttribute("errorMessage", "Evaluation pipeline queue is currently at capacity. Please retry shortly.");
                }
                req.setAttribute("evaluatedTx", tx);
            } else {
                // Synchronous immediate evaluation
                Transaction evaluated = transactionService.processTransactionSync(tx);
                req.setAttribute("evaluatedTx", evaluated);

                if (evaluated.getStatus() == Transaction.Status.APPROVED) {
                    req.setAttribute("successMessage", "Payment Approved! Risk Score: " +
                            String.format("%.2f", evaluated.getRiskScore()));
                } else if (evaluated.getStatus() == Transaction.Status.REVIEW) {
                    req.setAttribute("warningMessage", "Payment Under Review: Unusual pattern detected. (Risk Score: " +
                            String.format("%.2f", evaluated.getRiskScore()) + ")");
                } else {
                    req.setAttribute("errorMessage", "Payment BLOCKED due to critical fraud probability! (Risk Score: " +
                            String.format("%.2f", evaluated.getRiskScore()) + ")");
                }
            }

            req.getRequestDispatcher("/new-transaction.jsp").forward(req, resp);

        } catch (NumberFormatException nfe) {
            req.setAttribute("errorMessage", "Invalid payment amount format.");
            req.getRequestDispatcher("/new-transaction.jsp").forward(req, resp);
        } catch (FraudDetectionException | DatabaseException ex) {
            req.setAttribute("errorMessage", "Transaction processing error: " + ex.getMessage());
            req.getRequestDispatcher("/new-transaction.jsp").forward(req, resp);
        }
    }
}
