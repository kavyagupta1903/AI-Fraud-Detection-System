# AI-Powered Fraud Detection System (Standard Java Enterprise)

A production-grade, enterprise financial fraud detection system built strictly in **Standard Java** without heavy frameworks or ORM layers. The platform combines deterministic rule-based algorithms with heuristic multivariate machine learning anomaly detection, an asynchronous multithreaded evaluation pipeline, pure JDBC persistence with connection pooling, and role-based web dashboards.

---

## 1. Technical Rubric & Architectural Alignment

| Rubric Requirement | Implementation Specifics | File Location |
| :--- | :--- | :--- |
| **Inheritance & Abstraction** | Abstract base class `Transaction` with specialized concrete subclasses `CreditCardTransaction` and `WireTransaction`. Polymorphic baseline risk calculations. | [`src/model/Transaction.java`](src/model/Transaction.java), [`CreditCardTransaction.java`](src/model/CreditCardTransaction.java), [`WireTransaction.java`](src/model/WireTransaction.java) |
| **Exception Hierarchy** | Custom checked exception hierarchy rooted in `FraudDetectionException`, specialized by `DatabaseException` and `ThresholdOutOfBoundsException`. | [`src/exception/FraudDetectionException.java`](src/exception/FraudDetectionException.java), [`DatabaseException.java`](src/exception/DatabaseException.java), [`ThresholdOutOfBoundsException.java`](src/exception/ThresholdOutOfBoundsException.java) |
| **Interfaces & Polymorphism** | Polymorphic `FraudDetectionEngine` with `RuleBasedEngine` and `MLAnomalyEngine`. Factory pattern for dynamic runtime engine switching. | [`src/engine/FraudDetectionEngine.java`](src/engine/FraudDetectionEngine.java), [`RuleBasedEngine.java`](src/engine/RuleBasedEngine.java), [`MLAnomalyEngine.java`](src/engine/MLAnomalyEngine.java), [`EngineFactory.java`](src/engine/EngineFactory.java) |
| **Generics & Collections** | Fully generic DAO interface `GenericDAO<T, ID>` with CRUD operations. High-concurrency collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`, `PriorityBlockingQueue`, bounded `LinkedBlockingQueue`). | [`src/dao/GenericDAO.java`](src/dao/GenericDAO.java), [`TransactionQueue.java`](src/thread/TransactionQueue.java), [`FraudAlert.java`](src/model/FraudAlert.java) |
| **Multithreading & Synchronization** | Asynchronous Producer-Consumer pipeline with bounded `LinkedBlockingQueue<Transaction>` processed by a managed `ThreadPoolExecutor`. Thread-safe metrics with `AtomicInteger` and `DoubleAdder`. | [`src/thread/TransactionQueue.java`](src/thread/TransactionQueue.java), [`FraudEvaluationWorker.java`](src/thread/FraudEvaluationWorker.java) |
| **Pure JDBC & Pooling** | Pure JDBC with `PreparedStatement` exclusively. Custom `DBConnectionPool` with leak prevention, health checks (`isValid(2)`), and manual transaction demarcations (`setAutoCommit(false)`, `commit()`, `rollback()`). | [`src/config/DBConnectionPool.java`](src/config/DBConnectionPool.java), [`schema.sql`](schema.sql) |
| **Presentation Layer** | Jakarta Servlets + JSP + JSTL with Bootstrap 5 dark-mode styling. Role-separated Admin and User portals. | [`src/controller/`](src/controller/), [`src/webapp/`](src/webapp/) |

---

## 2. Source Code Tree

```text
├── pom.xml
├── schema.sql
├── run.bat
├── run.sh
├── mvnw.cmd
├── mvnw
├── src/
│   ├── Main.java
│   ├── config/
│   │   ├── DBConnectionPool.java        (Custom pure JDBC connection pool)
│   │   └── AppConfig.java               (Thread-safe config with dynamic DB sync)
│   ├── model/
│   │   ├── Transaction.java             (Abstract base class)
│   │   ├── CreditCardTransaction.java   (Concrete card subclass)
│   │   ├── WireTransaction.java         (Concrete wire subclass)
│   │   ├── User.java                    (User & Admin accounts)
│   │   ├── FraudAlert.java              (Comparable priority alerts)
│   │   └── SystemConfig.java            (Key-value threshold model)
│   ├── dao/
│   │   ├── GenericDAO.java              (Generic CRUD interface)
│   │   ├── UserDAO.java                 (Pure JDBC User DAO)
│   │   ├── TransactionDAO.java          (Polymorphic transaction persistence)
│   │   ├── SystemConfigDAO.java         (Config threshold DAO)
│   │   └── FraudAlertDAO.java           (Security notice DAO)
│   ├── engine/
│   │   ├── FraudDetectionEngine.java    (Core interface & result record)
│   │   ├── RuleBasedEngine.java         (Velocity, geo, amount checks)
│   │   ├── MLAnomalyEngine.java         (Multivariate isolation forest heuristic)
│   │   └── EngineFactory.java           (Factory pattern & hybrid ensemble)
│   ├── thread/
│   │   ├── TransactionQueue.java        (Producer-Consumer queue & metrics)
│   │   └── FraudEvaluationWorker.java   (Consumer worker thread)
│   ├── exception/
│   │   ├── FraudDetectionException.java (Checked domain exception)
│   │   ├── DatabaseException.java       (Checked JDBC exception)
│   │   └── ThresholdOutOfBoundsException.java (Validation exception)
│   ├── service/
│   │   ├── TransactionService.java      (Payment lifecycle & sync/async execution)
│   │   └── AdminMonitoringService.java  (Admin ops, metrics & review triage)
│   ├── controller/
│   │   ├── AuthServlet.java             (Session authentication & routing)
│   │   ├── TransactionServlet.java      (Payment submission & user ledger)
│   │   ├── AdminDashboardServlet.java   (Admin oversight, threshold tuning)
│   │   └── AlertServlet.java            (Incident management)
│   ├── server/
│   │   └── AppServer.java               (Embedded server launcher)
│   ├── util/
│   │   └── SecurityUtil.java            (SHA-256 cryptographic hashing)
│   └── webapp/
│       ├── WEB-INF/
│       │   └── web.xml
│       ├── login.jsp                    (Login page with 1-click test credentials)
│       ├── admin-dashboard.jsp          (Admin operations & threshold tuning)
│       ├── user-dashboard.jsp           (User account ledger & security badge)
│       ├── new-transaction.jsp          (Payment initiation with real-time feedback)
│       └── alerts.jsp                   (Priority alert notification stream)
└── test/
    └── FraudDetectionSystemTest.java   (Complete automated JUnit 5 test suite)
```

---

## 3. Quick Start & Execution

### Option A: 1-Click Launch (Windows)
Double-click `run.bat` or run:
```powershell
.\run.bat
```

### Option B: Maven Wrapper
```bash
# Windows
.\mvnw.cmd compile exec:java

# Linux / macOS
chmod +x mvnw run.sh
./run.sh
```

### Option C: Run Automated Tests
```bash
.\mvnw.cmd clean test
```

---

## 4. Access URLs & Test Credentials

Once started, open your web browser to:
- **Application Portal:** [http://localhost:8080/login](http://localhost:8080/login)
- **Admin Console:** [http://localhost:8080/admin/dashboard](http://localhost:8080/admin/dashboard)
- **User Dashboard:** [http://localhost:8080/user/dashboard](http://localhost:8080/user/dashboard)
- **Initiate Payment:** [http://localhost:8080/transactions/new](http://localhost:8080/transactions/new)
- **Security Alerts:** [http://localhost:8080/alerts](http://localhost:8080/alerts)

### Seed Credentials:
| Role | Username | Password | Notes |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Full access to metrics, threshold tuning, and manual triage |
| **Standard User** | `john_doe` | `password123` | Access to card/wire payment submission and account ledger |

*(The login screen also features 1-click test sign-in buttons for instant demonstration).*

---

## 5. Functional Demonstration Scenarios

1. **Approved Payment:**
   - Sign in as `john_doe`.
   - Submit a Credit Card transaction for **$85.00** in **New York, US** with verified CVV.
   - Result: `APPROVED` (Risk score ~0.10).

2. **Under Review (Flagged):**
   - Submit a Credit Card transaction for **$4,900.00** with unverified CVV or elevated amount.
   - Result: `UNDER_REVIEW` (Risk score exceeds flag threshold `0.70`).
   - Switch to the Admin Dashboard: Approve the flagged transaction. Notice the **False Positives** counter increments by 1.

3. **Critical Block:**
   - Submit a High-Value Wire Transfer for **$50,000.00** to **Lagos, NG** from an unrecognized device (`dev-fp-unknown`).
   - Result: `BLOCKED` (Risk score > 0.90).
   - Generates an immediate `CRITICAL` alert accessible in the Alerts console.

4. **Dynamic Threshold Tuning:**
   - Log in as `admin`.
   - Update Flag Threshold from `0.70` to `0.65` or adjust ML feature weights (Amount, Location, Device, Velocity).
   - The settings persist immediately to the database and sync lock-free across all threads via `ConcurrentHashMap`.
