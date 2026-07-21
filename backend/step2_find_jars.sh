#!/bin/bash
set -e
echo "=== Find all JARs in backend container ==="
docker exec rehealth-staging-backend-1 sh -c "find / -name '*.jar' -not -path '*/proc/*' 2>/dev/null | head -30" 2>&1

echo ""
echo "=== Check /app structure ==="
docker exec rehealth-staging-backend-1 sh -c "ls -la /app/ 2>/dev/null; ls -la /workspace/ 2>/dev/null; ls -la / 2>/dev/null | grep -v proc" 2>&1

echo ""
echo "=== Check where Spring Boot runs from ==="
docker exec rehealth-staging-backend-1 sh -c "ps aux 2>/dev/null | head -5" 2>&1
