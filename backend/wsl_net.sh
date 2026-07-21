#!/usr/bin/env bash
set -u
echo "--- wsl eth0 ip ---"
ip -4 addr show eth0 2>/dev/null | grep -oP 'inet \K[\d.]+'
echo "--- gateway (windows host) ---"
ip -4 route show default 2>/dev/null | grep -oP 'via \K[\d.]+'
echo "--- probe 8080 from wsl ---"
curl -s -o /dev/null -w '%{http_code}\n' --max-time 5 http://localhost:8080/jeecg-boot/ 2>/dev/null || echo failed
