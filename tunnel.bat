@echo off
echo ===========================================================================
echo  Starting Instant Public HTTPS Tunnel for FraudShield AI (Port 8080)...
echo ===========================================================================
echo  Note: Ensure the application server is running (via run.bat).
echo  Your live public URL with TLS termination will appear below:
echo ---------------------------------------------------------------------------
ssh -o StrictHostKeyChecking=no -R 80:localhost:8080 nokey@localhost.run
pause
