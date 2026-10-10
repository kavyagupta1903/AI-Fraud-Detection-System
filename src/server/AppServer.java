package server;

import config.DBConnectionPool;
import controller.AdminDashboardServlet;
import controller.AlertServlet;
import controller.AuthServlet;
import controller.TransactionServlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import thread.TransactionQueue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded Application Server Launcher.
 * Boots embedded Tomcat with pure JDBC connection pool and asynchronous
 * worker thread pool in under two seconds.
 */
public class AppServer {

    private static final Logger LOGGER = Logger.getLogger(AppServer.class.getName());
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        } else if (System.getenv("PORT") != null) {
            try {
                port = Integer.parseInt(System.getenv("PORT"));
            } catch (NumberFormatException ignored) {}
        }

        try {
            startServer(port);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error starting AppServer", e);
            System.exit(1);
        }
    }

    public static void startServer(int port) throws Exception {
        // 1. Initialize Database & Schema
        System.out.println(">>> Initializing Pure JDBC Connection Pool & Database Schema...");
        DBConnectionPool.getInstance();

        // 2. Start Asynchronous Pipeline Worker Threads
        System.out.println(">>> Initializing Multithreaded Fraud Evaluation Workers...");
        TransactionQueue.getInstance().startWorkers(4);

        // 3. Resolve Webapp Base Directory
        String webappDirLocation = "src/webapp";
        if (!new File(webappDirLocation).exists()) {
            webappDirLocation = "webapp";
        }
        File webappDir = new File(webappDirLocation);

        // 4. Configure Embedded Tomcat
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Trigger connector creation

        // Base directory for Tomcat temp files
        Path tempBase = Paths.get(System.getProperty("java.io.tmpdir"), "tomcat-fraud-shield");
        Files.createDirectories(tempBase);
        tomcat.setBaseDir(tempBase.toString());

        // Context creation
        Context ctx = tomcat.addWebapp("", webappDir.getAbsolutePath());
        ctx.setParentClassLoader(AppServer.class.getClassLoader());

        // Explicitly register servlets to guarantee mapping across all environments
        registerServlets(ctx);

        // 5. Start Server
        tomcat.start();

        System.out.println("\n" +
                "===========================================================================\n" +
                " 🚀 AI-Powered Fraud Detection System is running!\n" +
                "===========================================================================\n" +
                " 🌐 Login Portal:       http://localhost:" + port + "/login\n" +
                " 🛡️ Admin Console:      http://localhost:" + port + "/admin/dashboard\n" +
                " 💳 User Portal:        http://localhost:" + port + "/user/dashboard\n" +
                " 💸 Initiate Payment:   http://localhost:" + port + "/transactions/new\n" +
                " 🔔 Security Alerts:    http://localhost:" + port + "/alerts\n" +
                "---------------------------------------------------------------------------\n" +
                " 🔑 Quick Credentials:\n" +
                "   👑 Admin:  admin     / admin123\n" +
                "   👤 User:   john_doe  / password123\n" +
                "===========================================================================\n");

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n>>> Stopping Embedded Tomcat Server...");
            try {
                tomcat.stop();
                tomcat.destroy();
                TransactionQueue.getInstance().shutdown();
                DBConnectionPool.getInstance().shutdown();
            } catch (Exception ignored) {}
            System.out.println(">>> Server successfully shut down.");
        }));

        tomcat.getServer().await();
    }

    private static void registerServlets(Context ctx) {
        // AuthServlet
        Tomcat.addServlet(ctx, "AuthServlet", new AuthServlet());
        ctx.addServletMappingDecoded("/login", "AuthServlet");
        ctx.addServletMappingDecoded("/logout", "AuthServlet");
        ctx.addServletMappingDecoded("/auth", "AuthServlet");

        // TransactionServlet
        Tomcat.addServlet(ctx, "TransactionServlet", new TransactionServlet());
        ctx.addServletMappingDecoded("/transactions", "TransactionServlet");
        ctx.addServletMappingDecoded("/transactions/new", "TransactionServlet");
        ctx.addServletMappingDecoded("/user/dashboard", "TransactionServlet");

        // AdminDashboardServlet
        Tomcat.addServlet(ctx, "AdminDashboardServlet", new AdminDashboardServlet());
        ctx.addServletMappingDecoded("/admin/dashboard", "AdminDashboardServlet");
        ctx.addServletMappingDecoded("/admin/actions", "AdminDashboardServlet");

        // AlertServlet
        Tomcat.addServlet(ctx, "AlertServlet", new AlertServlet());
        ctx.addServletMappingDecoded("/alerts", "AlertServlet");
    }
}
