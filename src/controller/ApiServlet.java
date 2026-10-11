package controller;

import config.AppConfig;
import engine.EngineFactory;
import engine.FraudDetectionEngine;
import engine.GeoLocationService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.CreditCardTransaction;
import model.Transaction;
import model.WireTransaction;
import service.AdminMonitoringService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise REST API Controller providing programmatic fraud evaluation
 * (/api/v1/evaluate) and real-time operational metrics streaming (/api/v1/metrics).
 */
@WebServlet(name = "ApiServlet", urlPatterns = {"/api/v1/evaluate", "/api/v1/metrics"})
public class ApiServlet extends HttpServlet {

    private final AdminMonitoringService adminService = new AdminMonitoringService();
    private final AppConfig appConfig = AppConfig.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        if ("/api/v1/metrics".equals(path)) {
            handleMetrics(resp);
        } else if ("/api/v1/evaluate".equals(path)) {
            // Also allow GET for quick browser/curl evaluation
            handleEvaluation(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Endpoint not found\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        if ("/api/v1/evaluate".equals(path)) {
            handleEvaluation(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\": \"Endpoint not found\"}");
        }
    }

    private void handleMetrics(HttpServletResponse resp) throws IOException {
        Map<String, Object> metrics = adminService.getSystemMetricsSnapshot();
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof Number || val instanceof Boolean) {
                json.append(val);
            } else {
                json.append("\"").append(escapeJson(String.valueOf(val))).append("\"");
            }
            first = false;
        }
        json.append("}");

        PrintWriter out = resp.getWriter();
        out.write(json.toString());
        out.flush();
    }

    private void handleEvaluation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, String> params = extractParameters(req);

        try {
            Long userId = Long.parseLong(params.getOrDefault("userId", "1"));
            double amountVal = Double.parseDouble(params.getOrDefault("amount", "100.0"));
            BigDecimal amount = BigDecimal.valueOf(amountVal);
            String currency = params.getOrDefault("currency", "USD");
            String txType = params.getOrDefault("type", "CREDIT_CARD").toUpperCase();
            String location = params.getOrDefault("location", "New York, USA");
            String ipAddress = params.getOrDefault("ipAddress", "127.0.0.1");
            String deviceFingerprint = params.getOrDefault("deviceFingerprint", "device-web-client");
            String category = params.getOrDefault("category", "RETAIL");
            String prevLocation = params.getOrDefault("prevLocation", "New York, USA");
            double hoursElapsed = Double.parseDouble(params.getOrDefault("hoursElapsed", "24.0"));

            Transaction tx;
            if ("WIRE".equals(txType)) {
                WireTransaction wire = new WireTransaction();
                wire.setRecipientBank(params.getOrDefault("recipientBank", "Chase Bank"));
                wire.setRecipientAccount(params.getOrDefault("recipientAccount", "ACCT-9999"));
                wire.setSwiftCode(params.getOrDefault("swiftCode", "CHASUS33"));
                wire.setWirePurpose(params.getOrDefault("wirePurpose", "Invoice Settlement"));
                tx = wire;
            } else {
                CreditCardTransaction cc = new CreditCardTransaction();
                cc.setCardNumberLast4("1111");
                cc.setCardNetwork("VISA");
                cc.setCvvVerified(true);
                cc.setBillingZip("10001");
                tx = cc;
            }

            String txId = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            tx.setTransactionId(txId);
            tx.setUserId(userId);
            tx.setAmount(amount);
            tx.setCurrency(currency);
            tx.setLocation(location);
            tx.setIpAddress(ipAddress);
            tx.setDeviceFingerprint(deviceFingerprint);
            tx.setCategory(category);
            tx.setStatus(Transaction.Status.REVIEW);

            // Prepare contextual metadata for travel velocity and statistical evaluation
            Map<String, Object> evalContext = new HashMap<>();
            long now = System.currentTimeMillis();
            long prevTime = now - (long)(hoursElapsed * 3600.0 * 1000.0);
            evalContext.put("prevLocation", prevLocation);
            evalContext.put("prevTimestamp", prevTime);
            evalContext.put("recentTransactionCount", (hoursElapsed < 2.0) ? 3 : 1);

            // Fetch active engine and evaluate
            String engineType = params.getOrDefault("engine", appConfig.getActiveEngine());
            FraudDetectionEngine engine = EngineFactory.getEngine(engineType);
            FraudDetectionEngine.FraudEvaluationResult result = engine.evaluate(tx, evalContext);

            // Haversine Impossible Travel analysis
            double travelDistanceKm = GeoLocationService.calculateDistanceKm(prevLocation, location);
            double velocityKmH = GeoLocationService.calculateSpeedKmH(prevLocation, location, hoursElapsed);
            boolean impossibleTravel = GeoLocationService.isSpeedImpossible(prevLocation, location, hoursElapsed);

            long elapsedMs = System.currentTimeMillis() - startTime;

            StringBuilder json = new StringBuilder("{");
            json.append("\"transactionId\":\"").append(txId).append("\",");
            json.append("\"riskScore\":").append(String.format(java.util.Locale.US, "%.3f", result.getRiskScore())).append(",");
            json.append("\"recommendedStatus\":\"").append(result.getStatus()).append("\",");
            json.append("\"engine\":\"").append(engine.getEngineName()).append("\",");
            json.append("\"reasons\":[");
            for (int i = 0; i < result.getTriggeredRules().size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escapeJson(result.getTriggeredRules().get(i))).append("\"");
            }
            json.append("],");
            json.append("\"executionTimeMs\":").append(elapsedMs).append(",");
            json.append("\"telemetry\":{");
            json.append("\"travelDistanceKm\":").append(String.format(java.util.Locale.US, "%.1f", travelDistanceKm)).append(",");
            json.append("\"velocityKmH\":").append(String.format(java.util.Locale.US, "%.1f", velocityKmH)).append(",");
            json.append("\"impossibleTravel\":").append(impossibleTravel).append(",");
            json.append("\"amount\":").append(String.format(java.util.Locale.US, "%.2f", amountVal)).append(",");
            json.append("\"flagThreshold\":").append(appConfig.getFlagThreshold()).append(",");
            json.append("\"blockThreshold\":").append(appConfig.getBlockThreshold());
            json.append("}}");

            PrintWriter out = resp.getWriter();
            out.write(json.toString());
            out.flush();

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            PrintWriter out = resp.getWriter();
            out.write("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            out.flush();
        }
    }

    private Map<String, String> extractParameters(HttpServletRequest req) throws IOException {
        Map<String, String> map = new HashMap<>();

        String contentType = req.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String body = sb.toString();
            Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([\\d.-]+|true|false))");
            Matcher m = p.matcher(body);
            while (m.find()) {
                String key = m.group(1);
                String strVal = m.group(2);
                String numVal = m.group(3);
                map.put(key, strVal != null ? strVal : numVal);
            }
        } else {
            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                if (entry.getValue() != null && entry.getValue().length > 0) {
                    map.put(entry.getKey(), entry.getValue()[0]);
                }
            }
        }

        if (req.getQueryString() != null) {
            for (String pair : req.getQueryString().split("&")) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    map.putIfAbsent(pair.substring(0, idx), pair.substring(idx + 1));
                }
            }
        }

        return map;
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
