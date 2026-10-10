#!/bin/bash
echo "==========================================================================="
echo " Starting Instant Public HTTPS Tunnel for FraudShield AI (Port 8080)..."
echo "==========================================================================="
ssh -o StrictHostKeyChecking=no -R 80:localhost:8080 nokey@localhost.run
