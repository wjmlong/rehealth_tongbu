#!/bin/bash
set -e

echo "=== Copy pycryptodome hash script into container ==="
docker cp D:/rehealthAI/backend/compute_hash_pycrypto.py rehealth-staging-model-service-1:/tmp/compute_hash_pycrypto.py

echo "=== Install pycryptodome ==="
docker exec rehealth-staging-model-service-1 pip install pycryptodome 2>&1 | tail -3

echo ""
echo "=== Compute hash ==="
docker exec rehealth-staging-model-service-1 python3 /tmp/compute_hash_pycrypto.py
