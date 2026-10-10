<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User Dashboard | FraudShield AI</title>
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
        .card-custom {
            background: rgba(30, 41, 59, 0.7);
            border: 1px solid rgba(255, 255, 255, 0.08);
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
        .alert-item {
            background: rgba(239, 68, 68, 0.1);
            border-left: 4px solid #ef4444;
            border-radius: 0.5rem;
            padding: 0.75rem 1rem;
            margin-bottom: 0.5rem;
        }
    </style>
</head>
<body>

<!-- Navigation Bar -->
<nav class="navbar navbar-expand-lg navbar-dark navbar-custom sticky-top">
    <div class="container-fluid px-4">
        <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="${pageContext.request.contextPath}/user/dashboard">
            <span class="p-2 rounded-3 bg-primary text-white"><i class="bi bi-wallet2"></i></span>
            <span>FraudShield <span class="text-primary">Member Portal</span></span>
        </a>
        <div class="d-flex align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/transactions/new" class="btn btn-primary btn-sm">
                <i class="bi bi-plus-circle me-1"></i> New Transaction
            </a>
            <div class="dropdown">
                <button class="btn btn-dark btn-sm dropdown-toggle border-secondary" type="button" data-bs-toggle="dropdown">
                    <i class="bi bi-person-circle me-1"></i> ${sessionScope.username}
                </button>
                <ul class="dropdown-menu dropdown-menu-dark dropdown-menu-end">
                    <c:if test="${sessionScope.role == 'ADMIN'}">
                        <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/dashboard"><i class="bi bi-speedometer2 me-2"></i>Admin Dashboard</a></li>
                        <li><hr class="dropdown-divider"></li>
                    </c:if>
                    <li><a class="dropdown-item text-danger" href="${pageContext.request.contextPath}/logout"><i class="bi bi-box-arrow-right me-2"></i>Sign Out</a></li>
                </ul>
            </div>
        </div>
    </div>
</nav>

<div class="container px-4 py-4">

    <!-- Active Alerts Banner -->
    <c:if test="${not empty alerts}">
        <div class="card-custom p-3 mb-4 border-danger">
            <div class="d-flex align-items-center gap-2 mb-2 text-danger fw-bold">
                <i class="bi bi-exclamation-octagon-fill fs-5"></i>
                <span>Security Notice: Suspicious Activity Detected on Your Account</span>
            </div>
            <c:forEach var="al" items="${alerts}">
                <div class="alert-item d-flex justify-content-between align-items-center">
                    <div>
                        <span class="badge ${al.severity == 'CRITICAL' ? 'bg-danger' : 'bg-warning text-dark'} me-2">${al.severity}</span>
                        <span class="small">${al.alertReason}</span>
                    </div>
                    <span class="small text-secondary"><fmt:formatDate value="${al.createdAt}" pattern="yyyy-MM-dd HH:mm"/></span>
                </div>
            </c:forEach>
        </div>
    </c:if>

    <!-- Welcome & Account Status -->
    <div class="row g-4 mb-4">
        <div class="col-md-8">
            <div class="card-custom p-4 h-100 d-flex flex-column justify-content-between">
                <div>
                    <h4 class="fw-bold text-white mb-1">Welcome back, ${sessionScope.username}!</h4>
                    <p class="text-secondary small mb-3">
                        Your account is protected in real-time by FraudShield's AI Multi-Feature Anomaly Engine.
                    </p>
                </div>
                <div class="d-flex gap-3">
                    <a href="${pageContext.request.contextPath}/transactions/new" class="btn btn-primary">
                        <i class="bi bi-send-fill me-1"></i> Send Payment
                    </a>
                    <a href="${pageContext.request.contextPath}/user/dashboard" class="btn btn-outline-secondary">
                        <i class="bi bi-arrow-clockwise me-1"></i> Refresh Ledger
                    </a>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card-custom p-4 h-100">
                <div class="text-secondary small fw-semibold">Account Security Status</div>
                <div class="d-flex align-items-center gap-2 mt-2">
                    <i class="bi bi-shield-fill-check fs-2 text-success"></i>
                    <div>
                        <div class="fw-bold text-white">AI Protected</div>
                        <div class="text-secondary small">Autonomous Heuristic Shield Active</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Transaction History Ledger -->
    <div class="card-custom">
        <div class="card-header bg-transparent border-secondary py-3 d-flex justify-content-between align-items-center">
            <h5 class="fw-bold mb-0 text-white"><i class="bi bi-clock-history text-primary me-2"></i>Payment History & Security Ledger</h5>
            <span class="badge bg-secondary">Real-Time Evaluation</span>
        </div>
        <div class="table-responsive">
            <table class="table table-custom table-hover mb-0">
                <thead>
                <tr>
                    <th>Transaction ID</th>
                    <th>Type & Details</th>
                    <th>Amount</th>
                    <th>Location / IP</th>
                    <th>Date & Time</th>
                    <th>Status</th>
                    <th>AI Diagnostic</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="tx" items="${transactions}">
                    <tr>
                        <td class="fw-mono small text-info fw-bold">${tx.transactionId}</td>
                        <td>
                            <div class="fw-semibold">${tx.transactionType}</div>
                            <div class="small text-secondary">${tx.transactionDetails}</div>
                        </td>
                        <td class="fw-bold text-white">
                            $<fmt:formatNumber value="${tx.amount}" pattern="#,##0.00"/> ${tx.currency}
                        </td>
                        <td class="small">
                            <div>${tx.location}</div>
                            <div class="text-secondary">${tx.ipAddress}</div>
                        </td>
                        <td class="small text-secondary">
                            <fmt:formatDate value="${tx.timestamp}" pattern="yyyy-MM-dd HH:mm:ss"/>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${tx.status == 'APPROVED'}">
                                    <span class="badge badge-approved"><i class="bi bi-check-circle me-1"></i> APPROVED</span>
                                </c:when>
                                <c:when test="${tx.status == 'REVIEW'}">
                                    <span class="badge badge-review"><i class="bi bi-exclamation-triangle me-1"></i> UNDER_REVIEW</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge badge-blocked"><i class="bi bi-x-octagon me-1"></i> FLAGGED_FRAUD</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="small" style="max-width: 250px;">
                            <span class="text-secondary text-truncate d-block" title="${tx.evaluationNotes}">
                                ${tx.evaluationNotes}
                            </span>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty transactions}">
                    <tr>
                        <td colspan="7" class="text-center py-4 text-secondary">
                            <i class="bi bi-receipt fs-2 d-block mb-1"></i> No transactions found in your account history.
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
