<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard | FraudShield AI</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body {
            background-color: #0b1329;
            color: #e2e8f0;
            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
            min-height: 100vh;
        }
        .navbar-custom {
            background: rgba(15, 23, 42, 0.95);
            backdrop-filter: blur(10px);
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
        }
        .metric-card {
            background: rgba(30, 41, 59, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 1rem;
            padding: 1.25rem;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        .metric-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.3);
        }
        .config-card {
            background: rgba(30, 41, 59, 0.8);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 1rem;
        }
        .table-custom {
            color: #cbd5e1;
            vertical-align: middle;
        }
        .table-custom thead th {
            background-color: rgba(15, 23, 42, 0.8);
            color: #94a3b8;
            font-size: 0.8rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }
        .badge-approved { background-color: rgba(16, 185, 129, 0.2); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.4); }
        .badge-review { background-color: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.4); }
        .badge-blocked { background-color: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4); }
        .form-control, .form-select {
            background-color: rgba(15, 23, 42, 0.7);
            border: 1px solid rgba(255, 255, 255, 0.15);
            color: #f1f5f9;
        }
        .form-control:focus, .form-select:focus {
            background-color: rgba(15, 23, 42, 0.95);
            border-color: #3b82f6;
            color: #fff;
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.25);
        }
        .risk-pill {
            font-size: 0.85rem;
            font-weight: 700;
            padding: 0.2rem 0.6rem;
            border-radius: 0.5rem;
        }
    </style>
</head>
<body>

<!-- Navigation Bar -->
<nav class="navbar navbar-expand-lg navbar-dark navbar-custom sticky-top">
    <div class="container-fluid px-4">
        <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="${pageContext.request.contextPath}/admin/dashboard">
            <span class="p-2 rounded-3 bg-primary text-white"><i class="bi bi-shield-check"></i></span>
            <span>FraudShield <span class="text-primary">Admin Console</span></span>
        </a>
        <div class="d-flex align-items-center gap-3">
            <span class="badge bg-secondary py-2 px-3">
                <i class="bi bi-cpu-fill text-info me-1"></i> Engine: <strong class="text-white">${metrics.activeEngine}</strong>
            </span>
            <a href="${pageContext.request.contextPath}/alerts" class="btn btn-outline-light btn-sm position-relative">
                <i class="bi bi-bell"></i> Alerts
                <c:if test="${metrics.priorityAlertsCount > 0}">
                    <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
                        ${metrics.priorityAlertsCount}
                    </span>
                </c:if>
            </a>
            <div class="dropdown">
                <button class="btn btn-dark btn-sm dropdown-toggle border-secondary" type="button" data-bs-toggle="dropdown">
                    <i class="bi bi-person-circle me-1"></i> ${sessionScope.username}
                </button>
                <ul class="dropdown-menu dropdown-menu-dark dropdown-menu-end">
                    <li><a class="dropdown-item" href="${pageContext.request.contextPath}/user/dashboard"><i class="bi bi-arrow-repeat me-2"></i>Switch to User View</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item text-danger" href="${pageContext.request.contextPath}/logout"><i class="bi bi-box-arrow-right me-2"></i>Sign Out</a></li>
                </ul>
            </div>
        </div>
    </div>
</nav>

<div class="container-fluid px-4 py-4">

    <!-- Flash Notifications -->
    <c:if test="${not empty sessionScope.flashSuccess}">
        <div class="alert alert-success alert-dismissible fade show bg-success text-white border-0 py-2" role="alert">
            <i class="bi bi-check-circle-fill me-2"></i> ${sessionScope.flashSuccess}
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="alert"></button>
        </div>
        <c:remove var="flashSuccess" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.flashError}">
        <div class="alert alert-danger alert-dismissible fade show bg-danger text-white border-0 py-2" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i> ${sessionScope.flashError}
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="alert"></button>
        </div>
        <c:remove var="flashError" scope="session"/>
    </c:if>

    <!-- Row 1: System Performance Metrics Overview -->
    <div class="row g-3 mb-4">
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">Processed Transactions</div>
                <div class="fs-3 fw-bold text-white mt-1">${metrics.totalProcessed}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-speedometer2 text-info me-1"></i> Queue: ${metrics.queueDepth}</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">Accuracy Rate</div>
                <div class="fs-3 fw-bold text-success mt-1">${metrics.accuracyRate}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-bullseye text-success me-1"></i> Precision Metric</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">False Positives</div>
                <div class="fs-3 fw-bold text-warning mt-1">${metrics.falsePositives}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-arrow-counterclockwise text-warning me-1"></i> Overturned flags</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">Blocked Fraudulent</div>
                <div class="fs-3 fw-bold text-danger mt-1">${metrics.totalBlocked}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-shield-slash text-danger me-1"></i> Auto & Manual Stops</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">Average Risk Score</div>
                <div class="fs-3 fw-bold text-info mt-1">${metrics.averageRiskScore}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-graph-up text-info me-1"></i> Baseline Mean</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">JDBC Connection Pool</div>
                <div class="fs-3 fw-bold text-primary mt-1">${metrics.activeDBConnections} <span class="fs-6 text-secondary">/ ${metrics.idleDBConnections} idle</span></div>
                <div class="small text-secondary mt-1"><i class="bi bi-database-check text-primary me-1"></i> Pure JDBC Pool</div>
            </div>
        </div>
    </div>

    <!-- Row 2: Dynamic System Configuration Forms -->
    <div class="row g-4 mb-4">
        <!-- Card 1: Risk Threshold Configuration -->
        <div class="col-lg-6">
            <div class="config-card p-4 h-100">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h5 class="fw-bold mb-0 text-white"><i class="bi bi-sliders text-primary me-2"></i>Risk Threshold Settings</h5>
                    <span class="badge bg-primary-subtle text-primary border border-primary">Dynamic Bounds</span>
                </div>
                <p class="text-secondary small">
                    Adjust classification boundaries. Transactions exceeding Flag Threshold enter Review; exceeding Block Threshold are stopped automatically.
                </p>
                <form action="${pageContext.request.contextPath}/admin/actions" method="POST">
                    <input type="hidden" name="action" value="update_thresholds">
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Flag Threshold (Review)</label>
                            <div class="input-group">
                                <input type="number" step="0.01" min="0.01" max="0.99" name="flagThreshold"
                                       class="form-control" value="${metrics.flagThreshold}" required>
                                <span class="input-group-text bg-dark text-secondary border-secondary">Score</span>
                            </div>
                            <div class="form-text text-secondary small">Default: 0.70 (Triggers alert)</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Block Threshold (Hard Stop)</label>
                            <div class="input-group">
                                <input type="number" step="0.01" min="0.01" max="1.00" name="blockThreshold"
                                       class="form-control" value="${metrics.blockThreshold}" required>
                                <span class="input-group-text bg-dark text-secondary border-secondary">Score</span>
                            </div>
                            <div class="form-text text-secondary small">Default: 0.90 (Halts payment)</div>
                        </div>
                        <div class="col-12 text-end">
                            <button type="submit" class="btn btn-primary btn-sm px-4">
                                <i class="bi bi-save me-1"></i> Save Thresholds
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <!-- Card 2: Algorithm Weights & Active Engine Switcher -->
        <div class="col-lg-6">
            <div class="config-card p-4 h-100">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h5 class="fw-bold mb-0 text-white"><i class="bi bi-cpu text-info me-2"></i>Algorithm Feature Weights</h5>
                    <!-- Engine Switcher Dropdown Form -->
                    <form action="${pageContext.request.contextPath}/admin/actions" method="POST" class="d-flex align-items-center gap-2">
                        <input type="hidden" name="action" value="switch_engine">
                        <select name="engineName" class="form-select form-select-sm" onchange="this.form.submit()">
                            <option value="ML_ANOMALY" ${metrics.activeEngine == 'ML_ANOMALY' ? 'selected' : ''}>AI ML Anomaly</option>
                            <option value="RULE_BASED" ${metrics.activeEngine == 'RULE_BASED' ? 'selected' : ''}>Rule-Based Engine</option>
                            <option value="HYBRID" ${metrics.activeEngine == 'HYBRID' ? 'selected' : ''}>Hybrid Ensemble</option>
                        </select>
                    </form>
                </div>
                <p class="text-secondary small">
                    Fine-tune ML multivariate anomaly feature weights in real-time. Automatically normalizes and recalibrates model output.
                </p>
                <form action="${pageContext.request.contextPath}/admin/actions" method="POST">
                    <input type="hidden" name="action" value="update_weights">
                    <div class="row g-2">
                        <div class="col-6 col-md-3">
                            <label class="form-label small text-secondary">Amount Weight</label>
                            <input type="number" step="0.05" min="0.0" max="1.0" name="weightAmount" class="form-control form-control-sm" value="${metrics.weightAmount}">
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small text-secondary">Location Weight</label>
                            <input type="number" step="0.05" min="0.0" max="1.0" name="weightLocation" class="form-control form-control-sm" value="${metrics.weightLocation}">
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small text-secondary">Device Weight</label>
                            <input type="number" step="0.05" min="0.0" max="1.0" name="weightDevice" class="form-control form-control-sm" value="${metrics.weightDevice}">
                        </div>
                        <div class="col-6 col-md-3">
                            <label class="form-label small text-secondary">Velocity Weight</label>
                            <input type="number" step="0.05" min="0.0" max="1.0" name="weightVelocity" class="form-control form-control-sm" value="${metrics.weightVelocity}">
                        </div>
                        <div class="col-12 d-flex justify-content-between align-items-center mt-3">
                            <form action="${pageContext.request.contextPath}/admin/actions" method="POST" class="d-inline">
                                <input type="hidden" name="action" value="recalculate">
                                <button type="submit" class="btn btn-outline-secondary btn-sm">
                                    <i class="bi bi-arrow-clockwise me-1"></i> Recalculate Baselines
                                </button>
                            </form>
                            <button type="submit" class="btn btn-info btn-sm text-dark fw-semibold px-4">
                                <i class="bi bi-check2-circle me-1"></i> Update Weights
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Row 3: Detection Monitoring & Triage Actions -->
    <div class="card config-card">
        <div class="card-header bg-transparent border-secondary py-3 d-flex flex-wrap justify-content-between align-items-center gap-2">
            <div>
                <h5 class="fw-bold mb-0 text-white"><i class="bi bi-shield-shaded text-warning me-2"></i>Live Transaction Monitoring Ledger</h5>
                <span class="text-secondary small">Filterable real-time evaluation stream and risk audit trails</span>
            </div>
            <!-- Filter Pills -->
            <div class="btn-group btn-group-sm" role="group">
                <a href="${pageContext.request.contextPath}/admin/dashboard?status=ALL"
                   class="btn ${currentFilter == 'ALL' ? 'btn-primary' : 'btn-outline-secondary'}">All</a>
                <a href="${pageContext.request.contextPath}/admin/dashboard?status=REVIEW"
                   class="btn ${currentFilter == 'REVIEW' ? 'btn-warning' : 'btn-outline-secondary'}">Under Review</a>
                <a href="${pageContext.request.contextPath}/admin/dashboard?status=BLOCKED"
                   class="btn ${currentFilter == 'BLOCKED' ? 'btn-danger' : 'btn-outline-secondary'}">Blocked</a>
                <a href="${pageContext.request.contextPath}/admin/dashboard?status=APPROVED"
                   class="btn ${currentFilter == 'APPROVED' ? 'btn-success' : 'btn-outline-secondary'}">Approved</a>
            </div>
        </div>

        <div class="table-responsive">
            <table class="table table-custom table-hover mb-0">
                <thead>
                <tr>
                    <th>Tx ID</th>
                    <th>Type</th>
                    <th>User ID</th>
                    <th>Amount</th>
                    <th>Location / IP</th>
                    <th>Risk Score</th>
                    <th>Status</th>
                    <th>Engine & Diagnosis</th>
                    <th class="text-end">Actions</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="tx" items="${transactions}">
                    <tr>
                        <td class="fw-mono small text-info fw-bold">${tx.transactionId}</td>
                        <td>
                            <c:choose>
                                <c:when test="${tx.transactionType == 'CREDIT_CARD'}">
                                    <span class="badge bg-dark border border-secondary"><i class="bi bi-credit-card me-1"></i> Card</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge bg-dark border border-info text-info"><i class="bi bi-bank me-1"></i> Wire</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>#${tx.userId}</td>
                        <td class="fw-bold text-white">$<fmt:formatNumber value="${tx.amount}" pattern="#,##0.00"/></td>
                        <td class="small">
                            <div>${tx.location}</div>
                            <div class="text-secondary">${tx.ipAddress}</div>
                        </td>
                        <td>
                            <c:set var="scorePct" value="${tx.riskScore * 100}"/>
                            <span class="risk-pill ${tx.riskScore >= metrics.blockThreshold ? 'bg-danger text-white' : (tx.riskScore >= metrics.flagThreshold ? 'bg-warning text-dark' : 'bg-success text-white')}">
                                <fmt:formatNumber value="${tx.riskScore}" pattern="0.00"/>
                            </span>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${tx.status == 'APPROVED'}">
                                    <span class="badge badge-approved"><i class="bi bi-check-circle me-1"></i> APPROVED</span>
                                </c:when>
                                <c:when test="${tx.status == 'REVIEW'}">
                                    <span class="badge badge-review"><i class="bi bi-exclamation-triangle me-1"></i> REVIEW</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-blocked"><i class="bi bi-x-octagon me-1"></i> BLOCKED</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="small" style="max-width: 280px;">
                            <div class="text-info fw-semibold">${tx.decisionEngine}</div>
                            <div class="text-secondary text-truncate" title="${tx.evaluationNotes}">${tx.evaluationNotes}</div>
                        </td>
                        <td class="text-end">
                            <div class="btn-group btn-group-sm">
                                <!-- Approve Action -->
                                <form action="${pageContext.request.contextPath}/admin/actions" method="POST" class="d-inline">
                                    <input type="hidden" name="action" value="approve">
                                    <input type="hidden" name="transactionId" value="${tx.transactionId}">
                                    <button type="submit" class="btn btn-outline-success btn-sm" title="Approve & Clear">
                                        <i class="bi bi-check-lg"></i>
                                    </button>
                                </form>
                                <!-- Investigate Action -->
                                <form action="${pageContext.request.contextPath}/admin/actions" method="POST" class="d-inline">
                                    <input type="hidden" name="action" value="investigate">
                                    <input type="hidden" name="transactionId" value="${tx.transactionId}">
                                    <button type="submit" class="btn btn-outline-warning btn-sm" title="Mark for Investigation">
                                        <i class="bi bi-search"></i>
                                    </button>
                                </form>
                                <!-- Block Action -->
                                <form action="${pageContext.request.contextPath}/admin/actions" method="POST" class="d-inline">
                                    <input type="hidden" name="action" value="block">
                                    <input type="hidden" name="transactionId" value="${tx.transactionId}">
                                    <input type="hidden" name="reason" value="Admin manual intervention block">
                                    <button type="submit" class="btn btn-outline-danger btn-sm" title="Hard Block">
                                        <i class="bi bi-slash-circle"></i>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty transactions}">
                    <tr>
                        <td colspan="9" class="text-center py-4 text-secondary">
                            <i class="bi bi-inbox fs-2 d-block mb-1"></i> No transactions found matching this filter.
                        </td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

</div>

<!-- Bootstrap 5 JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
