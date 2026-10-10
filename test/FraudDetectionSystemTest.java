import config.AppConfig;
import config.DBConnectionPool;
import dao.TransactionDAO;
import dao.UserDAO;
import engine.EngineFactory;
import engine.FraudDetectionEngine;
import engine.MLAnomalyEngine;
import engine.RuleBasedEngine;
import exception.DatabaseException;
import exception.FraudDetectionException;
import exception.ThresholdOutOfBoundsException;
import model.CreditCardTransaction;
import model.FraudAlert;
import model.Transaction;
import model.User;
import model.WireTransaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import service.AdminMonitoringService;
import service.TransactionService;
import thread.TransactionQueue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FraudDetectionSystemTest {

    private static DBConnectionPool pool;
    private static TransactionService transactionService;
    private static AdminMonitoringService adminService;

    @BeforeAll
    static void setUpAll() {
        pool = DBConnectionPool.getInstance();
        transactionService = new TransactionService();
        adminService = new AdminMonitoringService();
    }

    @Test
    @Order(1)
    @DisplayName("Verify OOP Inheritance, Polymorphism and Base Risk Factors")
    void testOOPPolymorphism() {
        Transaction cc = new CreditCardTransaction("TEST-CC-1", 2L, new BigDecimal("120.00"), "USD",
                "New York, US", "127.0.0.1", "fp-1", "RETAIL", "4242", "VISA", true, "10001");

        Transaction wt = new WireTransaction("TEST-WT-1", 2L, new BigDecimal("25000.00"), "USD",
                "Lagos, NG", "102.89.23.44", "fp-unknown", "BUSINESS",
                "Apex Bank", "ACC-999", "APEXNG22", "Commercial payment");

        // Verify abstraction methods
        assertTrue(cc.getTransactionDetails().contains("4242"));
        assertTrue(wt.getTransactionDetails().contains("Apex Bank"));

        // Polymorphic baseline risk calculations
        double ccRisk = cc.calculateBaseRiskFactor();
        double wtRisk = wt.calculateBaseRiskFactor();

        assertTrue(ccRisk < wtRisk, "Wire transfers with high risk location should have higher baseline risk than verified card");
    }

    @Test
    @Order(2)
    @DisplayName("Verify EngineFactory and Core Engines (Rule-Based and ML Anomaly)")
    void testEnginesAndFactory() throws FraudDetectionException {
        FraudDetectionEngine ruleEngine = EngineFactory.getEngine("RULE_BASED");
        FraudDetectionEngine mlEngine = EngineFactory.getEngine("ML_ANOMALY");
        FraudDetectionEngine hybridEngine = EngineFactory.getEngine("HYBRID");

        assertNotNull(ruleEngine);
        assertNotNull(mlEngine);
        assertNotNull(hybridEngine);
        assertInstanceOf(RuleBasedEngine.class, ruleEngine);
        assertInstanceOf(MLAnomalyEngine.class, mlEngine);

        // Low risk transaction
        Transaction safeTx = new CreditCardTransaction("TEST-CC-SAFE", 2L, new BigDecimal("50.00"), "USD",
                "New York, US", "127.0.0.1", "fp-1", "RETAIL", "4242", "VISA", true, "10001");

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("recentTransactionCount", 0);

        FraudDetectionEngine.FraudEvaluationResult safeResult = mlEngine.evaluate(safeTx, ctx);
        assertEquals(Transaction.Status.APPROVED, safeResult.getStatus());
        assertTrue(safeResult.getRiskScore() < 0.70);

        // High risk wire transaction
        Transaction fraudTx = new WireTransaction("TEST-WT-FRAUD", 2L, new BigDecimal("50000.00"), "USD",
                "Lagos, NG", "102.89.23.44", "dev-fp-unknown-888", "BUSINESS",
                "Foreign Bank", "ACC-12345", "SWIFT999", "Offshore Transfer");

        FraudDetectionEngine.FraudEvaluationResult fraudResult = mlEngine.evaluate(fraudTx, ctx);
        assertTrue(fraudResult.getRiskScore() >= 0.70);
        assertTrue(fraudResult.getStatus() == Transaction.Status.REVIEW || fraudResult.getStatus() == Transaction.Status.BLOCKED);
        assertFalse(fraudResult.getTriggeredRules().isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Verify Pure JDBC GenericDAO CRUD Operations")
    void testGenericDAOOperations() throws DatabaseException {
        UserDAO userDAO = new UserDAO(pool);

        // Retrieve seeded user
        Optional<User> adminOpt = userDAO.findByUsername("admin");
        assertTrue(adminOpt.isPresent());
        User admin = adminOpt.get();
        assertEquals(User.Role.ADMIN, admin.getRole());

        // Find all
        List<User> allUsers = userDAO.findAll();
        assertFalse(allUsers.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("Verify Custom Exception Hierarchy and Boundary Validation")
    void testExceptionHierarchy() {
        // ThresholdOutOfBoundsException extends FraudDetectionException extends Exception
        assertThrows(ThresholdOutOfBoundsException.class, () -> {
            adminService.updateRiskThresholds(0.95, 0.70, "ADMIN"); // flag >= block must fail
        });

        assertThrows(ThresholdOutOfBoundsException.class, () -> {
            adminService.updateRiskThresholds(-0.1, 0.90, "ADMIN"); // negative value must fail
        });

        assertThrows(ThresholdOutOfBoundsException.class, () -> {
            adminService.updateRiskThresholds(0.50, 1.50, "ADMIN"); // > 1.0 must fail
        });
    }

    @Test
    @Order(5)
    @DisplayName("Verify Multithreaded Asynchronous Producer-Consumer Pipeline")
    void testAsyncProducerConsumerPipeline() throws Exception {
        TransactionQueue queue = TransactionQueue.getInstance();
        queue.startWorkers(2);

        String txId = "TEST-ASYNC-" + System.currentTimeMillis();
        Transaction tx = new CreditCardTransaction(txId, 2L, new BigDecimal("199.99"), "USD",
                "Chicago, US", "192.168.1.50", "fp-chicago", "ELECTRONICS", "1111", "VISA", true, "60601");

        boolean enqueued = queue.enqueue(tx);
        assertTrue(enqueued, "Transaction should be successfully enqueued");

        // Wait briefly for worker consumer to process
        TransactionDAO txDAO = new TransactionDAO(pool);
        boolean processed = false;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(200);
            Optional<Transaction> fetched = txDAO.findById(txId);
            if (fetched.isPresent() && fetched.get().getDecisionEngine() != null) {
                processed = true;
                assertEquals(Transaction.Status.APPROVED, fetched.get().getStatus());
                break;
            }
        }
        assertTrue(processed, "Asynchronous worker should have evaluated and persisted the transaction");
    }

    @Test
    @Order(6)
    @DisplayName("Verify PriorityQueue Ordering for FraudAlerts")
    void testPriorityAlertTriage() {
        PriorityBlockingQueue<FraudAlert> priorityQueue = new PriorityBlockingQueue<>();

        FraudAlert lowAlert = new FraudAlert(1L, "TX-1", 2L, "Low deviation", FraudAlert.Severity.LOW, false);
        FraudAlert criticalAlert = new FraudAlert(2L, "TX-2", 2L, "Critical wire block", FraudAlert.Severity.CRITICAL, false);
        FraudAlert mediumAlert = new FraudAlert(3L, "TX-3", 2L, "Medium anomaly", FraudAlert.Severity.MEDIUM, false);

        priorityQueue.offer(lowAlert);
        priorityQueue.offer(criticalAlert);
        priorityQueue.offer(mediumAlert);

        // Highest severity must poll first
        FraudAlert firstPolled = priorityQueue.poll();
        assertNotNull(firstPolled);
        assertEquals(FraudAlert.Severity.CRITICAL, firstPolled.getSeverity());

        FraudAlert secondPolled = priorityQueue.poll();
        assertNotNull(secondPolled);
        assertEquals(FraudAlert.Severity.MEDIUM, secondPolled.getSeverity());

        FraudAlert thirdPolled = priorityQueue.poll();
        assertNotNull(thirdPolled);
        assertEquals(FraudAlert.Severity.LOW, thirdPolled.getSeverity());
    }

    @Test
    @Order(7)
    @DisplayName("Verify Transaction Review and False-Positive Counter Synchronization")
    void testAdminReviewAndFalsePositiveTracking() throws Exception {
        int initialFp = adminService.getSystemMetricsSnapshot().get("falsePositives") instanceof Integer
                ? (Integer) adminService.getSystemMetricsSnapshot().get("falsePositives") : 0;

        // Create a transaction under review
        String reviewTxId = "TX-REV-" + System.currentTimeMillis();
        Transaction reviewTx = new CreditCardTransaction(reviewTxId, 2L, new BigDecimal("4900.00"), "USD",
                "Miami, US", "192.168.1.99", "dev-fp-unknown", "CRYPTO", "8888", "VISA", false, "33101");
        reviewTx.setStatus(Transaction.Status.REVIEW);
        reviewTx.setRiskScore(0.78);
        reviewTx.setDecisionEngine("ML_ANOMALY");

        TransactionDAO txDAO = new TransactionDAO(pool);
        txDAO.save(reviewTx);

        // Admin approves the flagged transaction
        boolean approved = adminService.reviewApproveTransaction(reviewTxId, "admin");
        assertTrue(approved);

        // Check updated status
        Optional<Transaction> updated = txDAO.findById(reviewTxId);
        assertTrue(updated.isPresent());
        assertEquals(Transaction.Status.APPROVED, updated.get().getStatus());

        // Check false positive metric incremented
        int updatedFp = (Integer) adminService.getSystemMetricsSnapshot().get("falsePositives");
        assertEquals(initialFp + 1, updatedFp);
    }
}
