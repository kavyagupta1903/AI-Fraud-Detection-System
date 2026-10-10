<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login | FraudShield AI Enterprise</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body {
            background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
            color: #f8fafc;
        }
        .login-card {
            background: rgba(30, 41, 59, 0.85);
            backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 1.25rem;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            width: 100%;
            max-width: 480px;
            padding: 2.5rem;
        }
        .brand-badge {
            background: linear-gradient(135deg, #3b82f6, #8b5cf6);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 60px;
            height: 60px;
            border-radius: 1rem;
            box-shadow: 0 10px 15px -3px rgba(59, 130, 246, 0.4);
            margin-bottom: 1.5rem;
        }
        .form-control {
            background-color: rgba(15, 23, 42, 0.6);
            border: 1px solid rgba(255, 255, 255, 0.15);
            color: #fff;
            padding: 0.75rem 1rem;
            border-radius: 0.75rem;
        }
        .form-control:focus {
            background-color: rgba(15, 23, 42, 0.9);
            border-color: #3b82f6;
            color: #fff;
            box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.25);
        }
        .btn-primary-gradient {
            background: linear-gradient(135deg, #2563eb, #7c3aed);
            border: none;
            padding: 0.75rem 1.5rem;
            border-radius: 0.75rem;
            font-weight: 600;
            transition: all 0.2s ease;
        }
        .btn-primary-gradient:hover {
            transform: translateY(-2px);
            box-shadow: 0 10px 20px -5px rgba(37, 99, 235, 0.5);
        }
        .demo-chip {
            background: rgba(255, 255, 255, 0.05);
            border: 1px dashed rgba(255, 255, 255, 0.2);
            border-radius: 0.75rem;
            padding: 1rem;
            margin-top: 1.5rem;
        }
    </style>
</head>
<body>

<div class="login-card">
    <div class="text-center">
        <div class="brand-badge">
            <i class="bi bi-shield-lock-fill fs-2 text-white"></i>
        </div>
        <h3 class="fw-bold mb-1">FraudShield AI</h3>
        <p class="text-secondary small mb-4">Enterprise Autonomous Fraud Detection Engine</p>
    </div>

    <!-- Error/Notice alerts -->
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger py-2 px-3 small rounded-3 mb-3" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>${errorMessage}
        </div>
    </c:if>
    <c:if test="${param.msg == 'logged_out'}">
        <div class="alert alert-info py-2 px-3 small rounded-3 mb-3" role="alert">
            <i class="bi bi-info-circle-fill me-2"></i>You have been logged out successfully.
        </div>
    </c:if>
    <c:if test="${param.msg == 'admin_required'}">
        <div class="alert alert-warning py-2 px-3 small rounded-3 mb-3" role="alert">
            <i class="bi bi-shield-exclamation me-2"></i>Administrator access required.
        </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/login" method="POST">
        <div class="mb-3">
            <label class="form-label small text-secondary fw-semibold">Username</label>
            <div class="input-group">
                <span class="input-group-text bg-transparent border-secondary text-secondary">
                    <i class="bi bi-person"></i>
                </span>
                <input type="text" name="username" class="form-control" placeholder="Enter username" required autofocus>
            </div>
        </div>

        <div class="mb-4">
            <label class="form-label small text-secondary fw-semibold">Password</label>
            <div class="input-group">
                <span class="input-group-text bg-transparent border-secondary text-secondary">
                    <i class="bi bi-key"></i>
                </span>
                <input type="password" name="password" class="form-control" placeholder="Enter password" required>
            </div>
        </div>

        <button type="submit" class="btn btn-primary-gradient w-100 mb-3">
            Sign In <i class="bi bi-arrow-right ms-2"></i>
        </button>
    </form>

    <!-- 1-Click Fast Login for Demonstrations -->
    <div class="demo-chip text-center">
        <div class="text-secondary small mb-2 fw-semibold">
            <i class="bi bi-lightning-charge-fill text-warning me-1"></i> Quick Test Sign-In
        </div>
        <div class="d-flex gap-2">
            <form action="${pageContext.request.contextPath}/login" method="POST" class="flex-fill">
                <input type="hidden" name="quickLogin" value="admin">
                <button type="submit" class="btn btn-sm btn-outline-info w-100">
                    <i class="bi bi-shield-check me-1"></i> Admin (admin)
                </button>
            </form>
            <form action="${pageContext.request.contextPath}/login" method="POST" class="flex-fill">
                <input type="hidden" name="quickLogin" value="user">
                <button type="submit" class="btn btn-sm btn-outline-light w-100">
                    <i class="bi bi-person-circle me-1"></i> User (john_doe)
                </button>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
