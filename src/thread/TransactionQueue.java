package thread;

import model.FraudAlert;
import model.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Logger;

/**
 * Thread-safe pipeline manager implementing Producer-Consumer pattern.
 * Uses bounded BlockingQueue, ConcurrentHashMap, CopyOnWriteArrayList,
 * PriorityBlockingQueue, and Atomic variables for thread-safe metrics.
 */
public class TransactionQueue {

    private static final Logger LOGGER = Logger.getLogger(TransactionQueue.class.getName());
    private static final int QUEUE_CAPACITY = 2000;

    private static volatile TransactionQueue instance;

    // Bounded FIFO Queue for Producer-Consumer pipeline
    private final BlockingQueue<Transaction> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    // Thread-safe Priority Queue for high-severity alert triage
    private final PriorityBlockingQueue<FraudAlert> alertPriorityQueue = new PriorityBlockingQueue<>();

    // In-memory sliding velocity cache: UserID -> List of recent transactions
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Transaction>> userVelocityCache = new ConcurrentHashMap<>();

    // Thread-Safe Performance & Accuracy Metrics
    private final AtomicInteger totalEnqueued = new AtomicInteger(0);
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger totalApproved = new AtomicInteger(0);
    private final AtomicInteger totalFlagged = new AtomicInteger(0);
    private final AtomicInteger totalBlocked = new AtomicInteger(0);
    private final AtomicInteger falsePositives = new AtomicInteger(0);
    private final DoubleAdder totalVolumeUSD = new DoubleAdder();
    private final DoubleAdder totalRiskAccumulator = new DoubleAdder();

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ThreadPoolExecutor threadPool;

    private TransactionQueue() {
    }

    public static TransactionQueue getInstance() {
        if (instance == null) {
            synchronized (TransactionQueue.class) {
                if (instance == null) {
                    instance = new TransactionQueue();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes and starts the worker thread pool for asynchronous evaluation.
     */
    public synchronized void startWorkers(int numThreads) {
        if (isRunning.compareAndSet(false, true)) {
            int coreThreads = Math.max(2, numThreads);
            int maxThreads = coreThreads * 2;
            LOGGER.info("Starting Transaction Consumer Worker Pool with " + coreThreads + " threads...");

            this.threadPool = new ThreadPoolExecutor(
                    coreThreads,
                    maxThreads,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(1000),
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("FraudWorker-" + t.getId());
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            for (int i = 0; i < coreThreads; i++) {
                threadPool.execute(new FraudEvaluationWorker(this));
            }
        }
    }

    /**
     * Producer: Enqueues a transaction into the evaluation pipeline.
     */
    public boolean enqueue(Transaction transaction) {
        if (transaction == null) return false;

        boolean offered = queue.offer(transaction);
        if (offered) {
            totalEnqueued.incrementAndGet();

            // Record into velocity cache for instantaneous sliding window tracking
            if (transaction.getUserId() != null) {
                userVelocityCache.computeIfAbsent(transaction.getUserId(), k -> new CopyOnWriteArrayList<>())
                        .add(transaction);
            }
        } else {
            LOGGER.warning("Transaction evaluation queue is full! Backpressure applied for TX: " + transaction.getTransactionId());
        }
        return offered;
    }

    /**
     * Consumer: Retrieves the next transaction, waiting up to the timeout.
     */
    public Transaction poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    /**
     * Calculates the number of transactions by user in the last N minutes.
     */
    public int getRecentTransactionCountForUser(Long userId, int windowMinutes) {
        if (userId == null) return 0;
        CopyOnWriteArrayList<Transaction> list = userVelocityCache.get(userId);
        if (list == null || list.isEmpty()) return 0;

        long thresholdMillis = System.currentTimeMillis() - (windowMinutes * 60L * 1000L);
        int count = 0;
        for (Transaction tx : list) {
            if (tx.getTimestamp() != null && tx.getTimestamp().getTime() >= thresholdMillis) {
                count++;
            }
        }
        return count;
    }

    // Metrics Tracking Helpers

    public void recordEvaluationResult(Transaction.Status status, double riskScore, BigDecimal amount) {
        totalProcessed.incrementAndGet();
        totalRiskAccumulator.add(riskScore);
        if (amount != null) {
            totalVolumeUSD.add(amount.doubleValue());
        }

        switch (status) {
            case APPROVED -> totalApproved.incrementAndGet();
            case REVIEW -> totalFlagged.incrementAndGet();
            case BLOCKED -> totalBlocked.incrementAndGet();
        }
    }

    public void incrementFalsePositive() {
        falsePositives.incrementAndGet();
    }

    public void pushAlert(FraudAlert alert) {
        if (alert != null) {
            alertPriorityQueue.offer(alert);
        }
    }

    public List<FraudAlert> pollTopPriorityAlerts(int maxCount) {
        List<FraudAlert> list = new ArrayList<>();
        int count = 0;
        FraudAlert alert;
        while (count < maxCount && (alert = alertPriorityQueue.poll()) != null) {
            list.add(alert);
            count++;
        }
        return list;
    }

    public synchronized void shutdown() {
        if (isRunning.compareAndSet(true, false)) {
            LOGGER.info("Shutting down Transaction Queue Worker Pool...");
            if (threadPool != null) {
                threadPool.shutdown();
                try {
                    if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        threadPool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Metrics Getters
    public int getTotalEnqueued() { return totalEnqueued.get(); }
    public int getTotalProcessed() { return totalProcessed.get(); }
    public int getTotalApproved() { return totalApproved.get(); }
    public int getTotalFlagged() { return totalFlagged.get(); }
    public int getTotalBlocked() { return totalBlocked.get(); }
    public int getFalsePositives() { return falsePositives.get(); }
    public double getTotalVolumeUSD() { return totalVolumeUSD.sum(); }
    public int getQueueDepth() { return queue.size(); }
    public int getPriorityAlertCount() { return alertPriorityQueue.size(); }

    public double getAverageRiskScore() {
        int processed = totalProcessed.get();
        return (processed > 0) ? (totalRiskAccumulator.sum() / processed) : 0.0;
    }

    public double getAccuracyRate() {
        int processed = totalProcessed.get();
        if (processed == 0) return 100.0;
        int fp = falsePositives.get();
        return Math.max(0.0, ((double) (processed - fp) / processed) * 100.0);
    }
}
