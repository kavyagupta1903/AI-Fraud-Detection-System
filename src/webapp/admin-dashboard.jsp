<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Enterprise Admin Console | FraudShield AI</title>
    <!-- Bootstrap 5 & Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <!-- Chart.js CDN -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
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
            background-color: rgba(15, 23, 42, 0.85);
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
        .nav-tabs .nav-link {
            color: #94a3b8;
            border: none;
            border-bottom: 2px solid transparent;
            font-weight: 600;
        }
        .nav-tabs .nav-link.active {
            color: #38bdf8;
            background: transparent;
            border-bottom: 2px solid #38bdf8;
        }
        .sim-gauge {
            height: 12px;
            border-radius: 6px;
            background: rgba(255,255,255,0.1);
            overflow: hidden;
        }
        .sim-gauge-bar {
            height: 100%;
            transition: width 0.4s ease, background-color 0.4s ease;
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
                <i class="bi bi-cpu-fill text-info me-1"></i> Active: <strong class="text-white">${metrics.activeEngine}</strong>
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
                <div class="fs-3 fw-bold text-white mt-1" id="mTotalProcessed">${metrics.totalProcessed}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-speedometer2 text-info me-1"></i> Queue: <span id="mQueueDepth">${metrics.queueDepth}</span></div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">Accuracy Rate</div>
                <div class="fs-3 fw-bold text-success mt-1" id="mAccuracyRate">${metrics.accuracyRate}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-bullseye text-success me-1"></i> Precision Metric</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">False Positives</div>
                <div class="fs-3 fw-bold text-warning mt-1" id="mFalsePositives">${metrics.falsePositives}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-arrow-counterclockwise text-warning me-1"></i> Overturned flags</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">Blocked Fraudulent</div>
                <div class="fs-3 fw-bold text-danger mt-1" id="mTotalBlocked">${metrics.totalBlocked}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-shield-slash text-danger me-1"></i> Auto & Manual Stops</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">Average Risk Score</div>
                <div class="fs-3 fw-bold text-info mt-1" id="mAvgRisk">${metrics.averageRiskScore}</div>
                <div class="small text-secondary mt-1"><i class="bi bi-graph-up text-info me-1"></i> Baseline Mean</div>
            </div>
        </div>
        <div class="col-xl-2 col-md-4 col-sm-6">
            <div class="metric-card">
                <div class="text-secondary small fw-semibold">JDBC Connection Pool</div>
                <div class="fs-3 fw-bold text-primary mt-1">${metrics.activeDBConnections} <span class="fs-6 text-secondary">/ ${metrics.idleDBConnections} idle</span></div>
                <div class="small text-secondary mt-1"><i class="bi bi-database-check text-primary me-1"></i> Zero-ORM Pool</div>
            </div>
        </div>
    </div>

    <!-- Row 2: Visual Analytics & Fraud Simulation Lab -->
    <div class="row g-4 mb-4">
        <!-- Chart 1: Verdict Distribution Doughnut -->
        <div class="col-xl-4 col-lg-5">
            <div class="config-card p-4 h-100">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <h6 class="fw-bold text-white mb-0"><i class="bi bi-pie-chart text-primary me-2"></i>Verdict Distribution</h6>
                    <span class="badge bg-secondary small">Real-time</span>
                </div>
                <p class="text-secondary small mb-3">Live classification breakdown across the transaction pipeline.</p>
                <div style="height: 240px; position: relative;">
                    <canvas id="verdictChart"></canvas>
                </div>
                <div class="d-flex justify-content-around text-center mt-3 pt-2 border-top border-secondary small">
                    <div><span class="text-success fw-bold">Approved</span>: <span id="lblApproved">${metrics.totalApproved}</span></div>
                    <div><span class="text-warning fw-bold">Review</span>: <span id="lblReview">${metrics.totalFlagged}</span></div>
                    <div><span class="text-danger fw-bold">Blocked</span>: <span id="lblBlocked">${metrics.totalBlocked}</span></div>
                </div>
            </div>
        </div>

        <!-- Chart 2: Feature Anomaly Radar Chart -->
        <div class="col-xl-4 col-lg-7">
            <div class="config-card p-4 h-100">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <h6 class="fw-bold text-white mb-0"><i class="bi bi-radar text-info me-2"></i>Multi-Dimensional Feature Radar</h6>
                    <span class="badge bg-info-subtle text-info border border-info small">Heuristic Vector</span>
                </div>
                <p class="text-secondary small mb-3">Feature sensitivity breakdown for current ML model weights.</p>
                <div style="height: 240px; position: relative;">
                    <canvas id="radarChart"></canvas>
                </div>
                <div class="text-center text-secondary small mt-3 pt-2 border-top border-secondary">
                    <i class="bi bi-info-circle me-1"></i> Dynamic vector adjusts as weights and simulations change.
                </div>
            </div>
        </div>

        <!-- Card 3: Interactive Fraud Simulation Lab Sandbox -->
        <div class="col-xl-4 col-lg-12">
            <div class="config-card p-4 h-100 border-primary border-opacity-50">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <h6 class="fw-bold text-white mb-0"><i class="bi bi-lightning-charge-fill text-warning me-2"></i>Fraud Simulation Lab</h6>
                    <span class="badge bg-warning-subtle text-warning border border-warning small">Live Sandbox</span>
                </div>
                <p class="text-secondary small mb-3">Test transaction scenarios against active AI models with sub-millisecond evaluation.</p>
                
                <form id="simForm" onsubmit="event.preventDefault(); runSimulation();">
                    <div class="row g-2 mb-2">
                        <div class="col-7">
                            <label class="form-label small text-secondary mb-1">Amount ($ USD)</label>
                            <input type="number" id="simAmount" class="form-control form-control-sm" value="8500" min="1" step="50" required>
                        </div>
                        <div class="col-5">
                            <label class="form-label small text-secondary mb-1">Category</label>
                            <select id="simCategory" class="form-select form-select-sm">
                                <option value="LUXURY_GOODS">Luxury</option>
                                <option value="CRYPTO_TRANSFER">Crypto</option>
                                <option value="WIRE_TRANSFER">Wire</option>
                                <option value="RETAIL" selected>Retail</option>
                                <option value="GROCERY">Grocery</option>
                            </select>
                        </div>
                    </div>

                    <div class="row g-2 mb-2">
                        <div class="col-6">
                            <label class="form-label small text-secondary mb-1">Previous Location</label>
                            <input type="text" id="simPrevLocation" class="form-control form-control-sm" value="New York, USA" placeholder="City, Country">
                        </div>
                        <div class="col-6">
                            <label class="form-label small text-secondary mb-1">Current Location</label>
                            <input type="text" id="simLocation" class="form-control form-control-sm" value="Moscow, Russia" placeholder="City, Country">
                        </div>
                    </div>

                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label small text-secondary mb-1">Elapsed Hours</label>
                            <input type="number" id="simHours" class="form-control form-control-sm" value="1.0" min="0.1" step="0.5">
                        </div>
                        <div class="col-6">
                            <label class="form-label small text-secondary mb-1">Engine</label>
                            <select id="simEngine" class="form-select form-select-sm">
                                <option value="ML_ANOMALY" selected>ML Anomaly</option>
                                <option value="RULE_BASED">Rule-Based</option>
                                <option value="HYBRID">Hybrid</option>
                            </select>
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary btn-sm w-100 fw-semibold mb-3">
                        <i class="bi bi-play-circle me-1"></i> Run Instant AI Evaluation
                    </button>
                </form>

                <!-- Live Simulation Output Box -->
                <div id="simResultBox" class="p-3 rounded-3" style="background: rgba(15, 23, 42, 0.7); border: 1px solid rgba(255,255,255,0.1); display: none;">
                    <div class="d-flex justify-content-between align-items-center mb-1">
                        <span class="small text-secondary">Evaluated Risk Score:</span>
                        <span id="simRiskScoreText" class="fw-bold">0.00</span>
                    </div>
                    <div class="sim-gauge mb-2">
                        <div id="simRiskGaugeBar" class="sim-gauge-bar" style="width: 0%; background-color: #10b981;"></div>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="small text-secondary">Recommendation:</span>
                        <span id="simStatusBadge" class="badge badge-approved">APPROVED</span>
                    </div>
                    <div id="simImpossibleTravelAlert" class="alert alert-danger py-1 px-2 mb-2 small" style="display: none;">
                        <i class="bi bi-exclamation-triangle-fill me-1"></i> <span id="simVelocityText"></span>
                    </div>
                    <div class="small text-secondary" id="simBreakdownText"></div>
                </div>
            </div>
        </div>
    </div>

    <!-- Row 3: Dynamic System Configuration Forms -->
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
                            <div class="form-text text-secondary small">Default: 0.70 (Triggers review)</div>
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

    <!-- Row 4: Tabbed Operations: Ledger, Compliance Audit Trail, and REST API -->
    <div class="card config-card">
        <div class="card-header bg-transparent border-secondary pt-3 pb-0">
            <ul class="nav nav-tabs border-secondary" id="adminTabs" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" id="ledger-tab" data-bs-toggle="tab" data-bs-target="#tabLedger" type="button" role="tab">
                        <i class="bi bi-journal-text me-1"></i> Live Transaction Ledger
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="audit-tab" data-bs-toggle="tab" data-bs-target="#tabAudit" type="button" role="tab">
                        <i class="bi bi-file-earmark-lock2 me-1"></i> Regulatory Compliance Audit Trail
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="api-tab" data-bs-toggle="tab" data-bs-target="#tabApi" type="button" role="tab">
                        <i class="bi bi-code-slash me-1"></i> Developer REST API
                    </button>
                </li>
            </ul>
        </div>

        <div class="card-body p-0">
            <div class="tab-content" id="adminTabContent">

                <!-- Tab 1: Transaction Ledger -->
                <div class="tab-pane fade show active" id="tabLedger" role="tabpanel">
                    <div class="p-3 d-flex flex-wrap justify-content-between align-items-center border-bottom border-secondary gap-2">
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
                        <div>
                            <a href="${pageContext.request.contextPath}/export/transactions" class="btn btn-outline-info btn-sm">
                                <i class="bi bi-download me-1"></i> Export Transactions (CSV)
                            </a>
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
                                            <form action="${pageContext.request.contextPath}/admin/actions" method="POST" class="d-inline">
                                                <input type="hidden" name="action" value="approve">
                                                <input type="hidden" name="transactionId" value="${tx.transactionId}">
                                                <button type="submit" class="btn btn-outline-success btn-sm" title="Approve & Clear">
                                                    <i class="bi bi-check-lg"></i>
                                                </button>
                                            </form>
                                            <form action="${pageContext.request.contextPath}/admin/actions" method="POST" class="d-inline">
                                                <input type="hidden" name="action" value="investigate">
                                                <input type="hidden" name="transactionId" value="${tx.transactionId}">
                                                <button type="submit" class="btn btn-outline-warning btn-sm" title="Mark for Investigation">
                                                    <i class="bi bi-search"></i>
                                                </button>
                                            </form>
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

                <!-- Tab 2: Regulatory Compliance Audit Trail -->
                <div class="tab-pane fade" id="tabAudit" role="tabpanel">
                    <div class="p-3 d-flex justify-content-between align-items-center border-bottom border-secondary">
                        <div>
                            <span class="text-secondary small">Immutable event sequence recorded for banking compliance & forensics.</span>
                        </div>
                        <div>
                            <a href="${pageContext.request.contextPath}/export/audit" class="btn btn-outline-info btn-sm">
                                <i class="bi bi-shield-shaded me-1"></i> Export Audit Trail (CSV)
                            </a>
                        </div>
                    </div>
                    <div class="table-responsive">
                        <table class="table table-custom table-hover mb-0">
                            <thead>
                            <tr>
                                <th>Audit ID</th>
                                <th>Action Type</th>
                                <th>Performed By</th>
                                <th>Target Entity</th>
                                <th>Compliance Event Details</th>
                                <th>Timestamp</th>
                            </tr>
                            </thead>
                            <tbody>
                            <c:forEach var="log" items="${auditLogs}">
                                <tr>
                                    <td class="fw-mono text-secondary">#${log.auditId}</td>
                                    <td>
                                        <span class="badge bg-dark border border-secondary text-info fw-semibold">${log.actionType}</span>
                                    </td>
                                    <td class="text-white"><i class="bi bi-person-fill text-secondary me-1"></i>${log.performedBy}</td>
                                    <td class="fw-mono text-warning">${log.targetId}</td>
                                    <td class="small text-light">${log.details}</td>
                                    <td class="small text-secondary">${log.timestamp}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty auditLogs}">
                                <tr>
                                    <td colspan="6" class="text-center py-4 text-secondary">
                                        <i class="bi bi-shield-lock fs-2 d-block mb-1"></i> No audit log records generated yet.
                                    </td>
                                </tr>
                            </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Tab 3: Developer REST API & Webhooks -->
                <div class="tab-pane fade p-4" id="tabApi" role="tabpanel">
                    <h5 class="fw-bold text-white mb-2"><i class="bi bi-terminal text-info me-2"></i>Enterprise Integration Endpoints</h5>
                    <p class="text-secondary small mb-4">Integrate fraud scoring directly into existing payment gateways, microservices, or mobile clients.</p>

                    <div class="row g-4">
                        <div class="col-lg-6">
                            <div class="p-3 rounded-3" style="background: rgba(15,23,42,0.8); border: 1px solid rgba(255,255,255,0.1);">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="badge bg-primary">POST /api/v1/evaluate</span>
                                    <span class="text-secondary small">Synchronous Evaluation</span>
                                </div>
                                <pre class="p-2 rounded bg-black text-info small mb-0" style="overflow-x: auto;"><code>curl -X POST http://localhost:8080/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 9500.00,
    "currency": "USD",
    "location": "Moscow, Russia",
    "prevLocation": "New York, USA",
    "hoursElapsed": 1.5,
    "category": "CRYPTO_TRANSFER"
  }'</code></pre>
                            </div>
                        </div>

                        <div class="col-lg-6">
                            <div class="p-3 rounded-3" style="background: rgba(15,23,42,0.8); border: 1px solid rgba(255,255,255,0.1);">
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="badge bg-success">GET /api/v1/metrics</span>
                                    <span class="text-secondary small">Real-Time Telemetry</span>
                                </div>
                                <pre class="p-2 rounded bg-black text-success small mb-0" style="overflow-x: auto;"><code>curl -X GET http://localhost:8080/api/v1/metrics</code></pre>
                                <div class="text-secondary small mt-2">
                                    Returns accuracy rates, queue depth, JDBC connection pool statistics, and operational latency.
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>

</div>

<!-- Bootstrap 5 JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

<!-- Visual Analytics & Chart.js Scripts -->
<script>
    // 1. Verdict Distribution Doughnut Chart
    const approvedCount = Number("${metrics.totalApproved}") || 0;
    const reviewCount = Number("${metrics.totalFlagged}") || 0;
    const blockedCount = Number("${metrics.totalBlocked}") || 0;

    const ctxVerdict = document.getElementById('verdictChart').getContext('2d');
    const verdictChart = new Chart(ctxVerdict, {
        type: 'doughnut',
        data: {
            labels: ['Approved', 'Under Review', 'Blocked'],
            datasets: [{
                data: [approvedCount, reviewCount, blockedCount],
                backgroundColor: ['#10b981', '#f59e0b', '#ef4444'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { color: '#94a3b8' } }
            },
            cutout: '70%'
        }
    });

    // 2. Feature Radar Chart
    const wAmount = Number("${metrics.weightAmount}") || 0.35;
    const wLocation = Number("${metrics.weightLocation}") || 0.25;
    const wDevice = Number("${metrics.weightDevice}") || 0.20;
    const wVelocity = Number("${metrics.weightVelocity}") || 0.20;

    const ctxRadar = document.getElementById('radarChart').getContext('2d');
    const radarChart = new Chart(ctxRadar, {
        type: 'radar',
        data: {
            labels: ['Amount Deviation', 'Geo Discrepancy', 'Device Mismatch', 'Travel Velocity', 'Category Risk'],
            datasets: [
                {
                    label: 'Model Sensitivity',
                    data: [wAmount * 100, wLocation * 100, wDevice * 100, wVelocity * 100, 30],
                    backgroundColor: 'rgba(56, 189, 248, 0.2)',
                    borderColor: '#38bdf8',
                    pointBackgroundColor: '#38bdf8'
                },
                {
                    label: 'Active Simulation',
                    data: [20, 20, 20, 20, 20],
                    backgroundColor: 'rgba(239, 68, 68, 0.2)',
                    borderColor: '#ef4444',
                    pointBackgroundColor: '#ef4444'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                r: {
                    angleLines: { color: 'rgba(255, 255, 255, 0.1)' },
                    grid: { color: 'rgba(255, 255, 255, 0.1)' },
                    pointLabels: { color: '#94a3b8', font: { size: 10 } },
                    suggestedMin: 0,
                    suggestedMax: 100,
                    ticks: { display: false }
                }
            },
            plugins: {
                legend: { position: 'bottom', labels: { color: '#94a3b8' } }
            }
        }
    });

    // 3. Fraud Simulation Lab Functionality
    async function runSimulation() {
        const amount = document.getElementById('simAmount').value;
        const category = document.getElementById('simCategory').value;
        const prevLocation = document.getElementById('simPrevLocation').value;
        const location = document.getElementById('simLocation').value;
        const hoursElapsed = document.getElementById('simHours').value;
        const engine = document.getElementById('simEngine').value;

        const payload = {
            amount: parseFloat(amount),
            category: category,
            prevLocation: prevLocation,
            location: location,
            hoursElapsed: parseFloat(hoursElapsed),
            engine: engine
        };

        const queryParams = new URLSearchParams({
            amount: amount,
            category: category,
            prevLocation: prevLocation,
            location: location,
            hoursElapsed: hoursElapsed,
            engine: engine
        });

        try {
            const resp = await fetch('${pageContext.request.contextPath}/api/v1/evaluate?' + queryParams.toString(), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await resp.json();

            // Display Results
            const box = document.getElementById('simResultBox');
            box.style.display = 'block';

            const score = data.riskScore || 0;
            const scorePct = Math.round(score * 100);
            document.getElementById('simRiskScoreText').innerText = score.toFixed(3) + ' (' + scorePct + '%)';
            
            const bar = document.getElementById('simRiskGaugeBar');
            bar.style.width = scorePct + '%';
            if (score >= 0.90) {
                bar.style.backgroundColor = '#ef4444';
            } else if (score >= 0.70) {
                bar.style.backgroundColor = '#f59e0b';
            } else {
                bar.style.backgroundColor = '#10b981';
            }

            const badge = document.getElementById('simStatusBadge');
            badge.className = 'badge';
            if (data.recommendedStatus === 'BLOCKED') {
                badge.classList.add('badge-blocked');
                badge.innerHTML = '<i class="bi bi-x-octagon me-1"></i> BLOCKED';
            } else if (data.recommendedStatus === 'REVIEW') {
                badge.classList.add('badge-review');
                badge.innerHTML = '<i class="bi bi-exclamation-triangle me-1"></i> REVIEW';
            } else {
                badge.classList.add('badge-approved');
                badge.innerHTML = '<i class="bi bi-check-circle me-1"></i> APPROVED';
            }

            // Impossible Travel Alert
            const travelAlert = document.getElementById('simImpossibleTravelAlert');
            if (data.telemetry && data.telemetry.impossibleTravel) {
                travelAlert.style.display = 'block';
                document.getElementById('simVelocityText').innerText = 
                    'Impossible Travel: ' + data.telemetry.travelDistanceKm.toFixed(0) + ' km in ' + hoursElapsed + 'h (' + data.telemetry.velocityKmH.toFixed(0) + ' km/h exceeds 900 km/h commercial flight speed)';
            } else {
                travelAlert.style.display = 'none';
            }

            // Reasons
            const reasonsText = (data.reasons && data.reasons.length > 0) 
                ? 'Diagnosis: ' + data.reasons.join(' | ') 
                : 'Clear: No heuristic risk anomalies detected.';
            document.getElementById('simBreakdownText').innerText = reasonsText + ' (Evaluated in ' + data.executionTimeMs + 'ms)';

            // Update Radar Chart Dataset 1 (Active Simulation)
            let amtFactor = Math.min(100, (parseFloat(amount) / 10000) * 100);
            let velFactor = data.telemetry ? Math.min(100, (data.telemetry.velocityKmH / 1000) * 100) : 10;
            let geoFactor = data.telemetry ? Math.min(100, (data.telemetry.travelDistanceKm / 10000) * 100) : 10;
            let catFactor = (category === 'CRYPTO_TRANSFER' || category === 'LUXURY_GOODS') ? 85 : 25;

            radarChart.data.datasets[1].data = [amtFactor, geoFactor, 30, velFactor, catFactor];
            radarChart.update();

        } catch (e) {
            alert('Simulation error: ' + e.message);
        }
    }
</script>
</body>
</html>
