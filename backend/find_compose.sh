#!/usr/bin/env bash
set -u
echo "=== find staging compose dir ==="
find /mnt/d/rehealthai/backend -maxdepth 3 -name "docker-compose*.yml" 2>/dev/null | grep -i staging
echo "=== find by container name ==="
docker inspect rehealth-staging-backend-1 --format '{{index .Config.Labels "com.docker.compose.project.working_dir"}}' 2>/dev/null
echo "=== compose project ==="
docker inspect rehealth-staging-backend-1 --format '{{index .Config.Labels "com.docker.compose.project"}}' 2>/dev/null
echo "=== compose file ==="
docker inspect rehealth-staging-backend-1 --format '{{index .Config.Labels "com.docker.compose.project.config_files"}}' 2>/dev/null
