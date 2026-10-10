<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Initiate Payment | FraudShield AI</title>
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
        .nav-pills .nav-link {
            color: #94a3b8;
            border-radius: 0.75rem;
            padding: 0.75rem 1.25rem;
            font-weight: 600;
        }
        .nav-pills .nav-link.active {
            background: linear-gradient(135deg, #2563eb, #7c3aed);
            color: #fff;
        }
        .form-control, .form-select {
            background-color: rgba(15, 23, 42, 0.7);
            border: 1px solid rgba(255, 255, 255, 0.15);
            color: #f1f5f9;
            border-radius: 0.6rem;
        }
        .form-control:focus, .form-select:focus {
            background-color: rgba(15, 23, 42, 0.95);
            border-color: #3b82f6;
            color: #fff;
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.25);
        }
        .result-card {
            border-radius: 1rem;
            padding: 1.5rem;
            margin-bottom: 2rem;
            border: 1px solid;
        }
        .result-approved {
            background: rgba(16, 185, 129, 0.1);
            border-color: rgba(16, 185, 129, 0.4);
        }
        .result-review {
            background: rgba(245, 158, 11, 0.1);
            border-color: rgba(245, 158, 11, 0.4);
        }
        .result-blocked {
            background: rgba(239, 68, 68, 0.1);
            border-color: rgba(239, 68, 68, 0.4);
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
            <a href="${pageContext.request.contextPath}/user/dashboard" class="btn btn-outline-light btn-sm">
                <i class="bi bi-arrow-left me-1"></i> Back to Ledger
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

<div class="container px-4 py-4" style="max-width: 900px;">

    <!-- Result Banner If Processed -->
    <c:if test="${not empty evaluatedTx}">
        <c:choose>
            <c:when test="${evaluatedTx.status == 'APPROVED'}">
                <div class="result-card result-approved">
                    <div class="d-flex align-items-center gap-3">
                        <i class="bi bi-check-circle-fill text-success fs-1"></i>
                        <div class="flex-grow-1">
                            <h5 class="fw-bold text-success mb-1">TRANSACTION APPROVED</h5>
                            <div class="text-white small">
                                Transaction ID: <strong>${evaluatedTx.transactionId}</strong> | Risk Score:
                                <span class="badge bg-success">${evaluatedTx.riskScore}</span>
                            </div>
                            <div class="text-secondary small mt-1">${evaluatedTx.evaluationNotes}</div>
                        </div>
                    </div>
                </div>
            </c:when>
            <c:when test="${evaluatedTx.status == 'REVIEW'}">
                <div class="result-card result-review">
                    <div class="d-flex align-items-center gap-3">
                        <i class="bi bi-exclamation-triangle-fill text-warning fs-1"></i>
                        <div class="flex-grow-1">
                            <h5 class="fw-bold text-warning mb-1">TRANSACTION UNDER REVIEW</h5>
                            <div class="text-white small">
                                Transaction ID: <strong>${evaluatedTx.transactionId}</strong> | Risk Score:
                                <span class="badge bg-warning text-dark">${evaluatedTx.riskScore}</span>
                            </div>
                            <div class="text-secondary small mt-1">${evaluatedTx.evaluationNotes}</div>
                        </div>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="result-card result-blocked">
                    <div class="d-flex align-items-center gap-3">
                        <i class="bi bi-shield-x text-danger fs-1"></i>
                        <div class="flex-grow-1">
                            <h5 class="fw-bold text-danger mb-1">TRANSACTION BLOCKED</h5>
                            <div class="text-white small">
                                Transaction ID: <strong>${evaluatedTx.transactionId}</strong> | Risk Score:
                                <span class="badge bg-danger">${evaluatedTx.riskScore}</span>
                            </div>
                            <div class="text-secondary small mt-1">${evaluatedTx.evaluationNotes}</div>
                        </div>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger py-2 px-3 small rounded-3 mb-3">
            <i class="bi bi-exclamation-triangle-fill me-2"></i> ${errorMessage}
        </div>
    </c:if>
    <c:if test="${not empty successMessage && empty evaluatedTx}">
        <div class="alert alert-success py-2 px-3 small rounded-3 mb-3">
            <i class="bi bi-check-circle-fill me-2"></i> ${successMessage}
        </div>
    </c:if>

    <!-- Payment Initiation Card -->
    <div class="card-custom p-4">
        <h4 class="fw-bold text-white mb-1"><i class="bi bi-cash-stack text-primary me-2"></i>Initiate Payment</h4>
        <p class="text-secondary small mb-4">
            Select payment rails. Transactions are verified in standard Java through polymorphic engines and multithreaded queues.
        </p>

        <!-- Payment Rails Tabs -->
        <ul class="nav nav-pills mb-4" id="pills-tab" role="tablist">
            <li class="nav-item flex-fill text-center" role="presentation">
                <button class="nav-link active w-100" id="card-tab" data-bs-toggle="pill" data-bs-target="#tab-card" type="button" role="tab">
                    <i class="bi bi-credit-card me-2"></i> Credit Card Payment
                </button>
            </li>
            <li class="nav-item flex-fill text-center" role="presentation">
                <button class="nav-link w-100" id="wire-tab" data-bs-toggle="pill" data-bs-target="#tab-wire" type="button" role="tab">
                    <i class="bi bi-bank me-2"></i> High-Value Wire Transfer
                </button>
            </li>
        </ul>

        <div class="tab-content" id="pills-tabContent">

            <!-- TAB 1: CREDIT CARD -->
            <div class="tab-pane fade show active" id="tab-card" role="tabpanel">
                <form action="${pageContext.request.contextPath}/transactions/new" method="POST">
                    <input type="hidden" name="paymentType" value="CREDIT_CARD">

                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Payment Amount</label>
                            <div class="input-group">
                                <span class="input-group-text bg-dark text-secondary border-secondary">$</span>
                                <input type="number" step="0.01" min="1.00" name="amount" class="form-control" placeholder="125.00" required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Currency</label>
                            <select name="currency" class="form-select">
                                <option value="USD" selected>USD - United States Dollar</option>
                                <option value="EUR">EUR - Euro</option>
                                <option value="GBP">GBP - British Pound</option>
                            </select>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Card Number (Last 4 Digits)</label>
                            <input type="text" maxlength="4" pattern="\d{4}" name="cardNumberLast4" class="form-control" placeholder="4242" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Card Network</label>
                            <select name="cardNetwork" class="form-select">
                                <option value="VISA">Visa</option>
                                <option value="MASTERCARD">Mastercard</option>
                                <option value="AMEX">American Express</option>
                                <option value="DISCOVER">Discover</option>
                            </select>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Billing ZIP / Postal Code</label>
                            <input type="text" name="billingZip" class="form-control" placeholder="10001" value="10001">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">CVV Verification Match</label>
                            <div class="form-check form-switch mt-2">
                                <input class="form-check-input" type="checkbox" name="cvvVerified" value="true" checked>
                                <label class="form-check-label text-secondary small">CVV Matched Card Issuer</label>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Transaction Location</label>
                            <input type="text" name="location" class="form-control" placeholder="New York, US" value="New York, US" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Device Fingerprint</label>
                            <input type="text" name="deviceFingerprint" class="form-control" value="dev-fp-john-laptop" required>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Category</label>
                            <select name="category" class="form-select">
                                <option value="RETAIL">Retail Shopping</option>
                                <option value="ELECTRONICS">Electronics & Gadgets</option>
                                <option value="TRAVEL">Airlines & Travel</option>
                                <option value="CRYPTO">Cryptocurrency Exchange</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Execution Mode</label>
                            <div class="form-check mt-2">
                                <input class="form-check-input" type="checkbox" name="asyncEvaluation" value="true" id="asyncCheckCard">
                                <label class="form-check-label text-secondary small" for="asyncCheckCard">
                                    Send via Asynchronous Worker Pipeline (Producer-Consumer)
                                </label>
                            </div>
                        </div>

                        <div class="col-12 text-end mt-4">
                            <button type="submit" class="btn btn-primary px-4">
                                <i class="bi bi-shield-lock-fill me-1"></i> Authorize & Submit Card Payment
                            </button>
                        </div>
                    </div>
                </form>
            </div>

            <!-- TAB 2: WIRE TRANSFER -->
            <div class="tab-pane fade" id="tab-wire" role="tabpanel">
                <form action="${pageContext.request.contextPath}/transactions/new" method="POST">
                    <input type="hidden" name="paymentType" value="WIRE_TRANSFER">

                    <div class="row g-3">
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Wire Amount</label>
                            <div class="input-group">
                                <span class="input-group-text bg-dark text-secondary border-secondary">$</span>
                                <input type="number" step="0.01" min="10.00" name="amount" class="form-control" placeholder="12500.00" required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Currency</label>
                            <select name="currency" class="form-select">
                                <option value="USD" selected>USD - United States Dollar</option>
                                <option value="EUR">EUR - Euro</option>
                                <option value="CHF">CHF - Swiss Franc</option>
                            </select>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Recipient Bank Name</label>
                            <input type="text" name="recipientBank" class="form-control" placeholder="Standard Chartered Bank" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">SWIFT / BIC Code</label>
                            <input type="text" name="swiftCode" class="form-control" placeholder="SCBLUS33" required>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Target Account / IBAN</label>
                            <input type="text" name="recipientAccount" class="form-control" placeholder="US89370400440532013000" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Wire Transfer Purpose</label>
                            <input type="text" name="wirePurpose" class="form-control" placeholder="Commercial Invoicing" value="Commercial Invoicing">
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Destination Location / Country</label>
                            <input type="text" name="location" class="form-control" placeholder="London, UK" value="London, UK" required>
                            <div class="form-text text-secondary small">Tip: Try 'Lagos, NG' to simulate high-risk alert/block.</div>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Device Fingerprint</label>
                            <input type="text" name="deviceFingerprint" class="form-control" value="dev-fp-john-laptop" required>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Category</label>
                            <select name="category" class="form-select">
                                <option value="BUSINESS">Corporate / Business Settlement</option>
                                <option value="REAL_ESTATE">Real Estate Escrow</option>
                                <option value="INVESTMENT">Capital Investment</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small text-secondary fw-semibold">Execution Mode</label>
                            <div class="form-check mt-2">
                                <input class="form-check-input" type="checkbox" name="asyncEvaluation" value="true" id="asyncCheckWire">
                                <label class="form-check-label text-secondary small" for="asyncCheckWire">
                                    Send via Asynchronous Worker Pipeline (Producer-Consumer)
                                </label>
                            </div>
                        </div>

                        <div class="col-12 text-end mt-4">
                            <button type="submit" class="btn btn-primary px-4">
                                <i class="bi bi-bank2 me-1"></i> Authorize & Transmit Wire
                            </button>
                        </div>
                    </div>
                </form>
            </div>

        </div>
    </div>
</div>

<!-- Bootstrap 5 JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
