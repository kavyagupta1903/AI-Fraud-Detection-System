package controller;

import dao.UserDAO;
import exception.DatabaseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import util.SecurityUtil;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet handling user authentication, session creation, and role-based routing.
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/login", "/logout", "/auth"})
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        if ("/logout".equals(path)) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/login?msg=logged_out");
            return;
        }

        // Forward to login page
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String quickRole = req.getParameter("quickLogin");

        // Convenience 1-click login for quick testing
        if ("admin".equalsIgnoreCase(quickRole)) {
            username = "admin";
            password = "admin123";
        } else if ("user".equalsIgnoreCase(quickRole)) {
            username = "john_doe";
            password = "password123";
        }

        if (username == null || username.isBlank() || password == null) {
            req.setAttribute("errorMessage", "Username and password are required.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        try {
            Optional<User> userOpt = userDAO.findByUsername(username.trim());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (SecurityUtil.verifyPassword(password, user.getPasswordHash())) {
                    HttpSession session = req.getSession(true);
                    session.setAttribute("user", user);
                    session.setAttribute("userId", user.getUserId());
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("role", user.getRole().name());

                    if (user.isAdmin()) {
                        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
                    } else {
                        resp.sendRedirect(req.getContextPath() + "/user/dashboard");
                    }
                    return;
                }
            }

            req.setAttribute("errorMessage", "Invalid username or password.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        } catch (DatabaseException e) {
            req.setAttribute("errorMessage", "Database error during authentication: " + e.getMessage());
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
