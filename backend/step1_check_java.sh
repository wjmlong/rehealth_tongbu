#!/bin/bash
# Compute JeecgBoot PBEWithMD5AndDES hash using jshell in the backend container
# PasswordUtil.encrypt(plaintext, password, salt) -> hex string

echo "=== Check if jshell is available ==="
docker exec rehealth-staging-backend-1 which jshell 2>&1 || echo "jshell not found"

echo "=== Check Java version ==="
docker exec rehealth-staging-backend-1 java -version 2>&1

echo "=== Find the app JAR ==="
docker exec rehealth-staging-backend-1 sh -c "ls /app/*.jar 2>/dev/null; ls /app/target/*.jar 2>/dev/null; find / -name 'jeecg-boot*.jar' -maxdepth 5 2>/dev/null | head -5"
