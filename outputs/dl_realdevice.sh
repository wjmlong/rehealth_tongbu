#!/usr/bin/env bash
cd /tmp
TOKEN=$(gh auth token)
echo "=== get redirect Location (no follow) ==="
LOC=$(curl -sI -H "Authorization: Bearer ${TOKEN}" "https://api.github.com/repos/RehealthAI/Android-apk/tarball/codex/real-device" | tr -d '\r' | grep -i '^location:' | sed 's/^[Ll]ocation: //')
echo "LOC length: ${#LOC}"
echo "LOC prefix: ${LOC:0:70}"
rm -rf real_branch branch.tar.gz
echo "=== download via signed Location ==="
curl -sL -o branch.tar.gz "$LOC"
echo "curl exit=$?"
ls -la branch.tar.gz
file branch.tar.gz 2>/dev/null || true
echo "=== extract ==="
mkdir -p real_branch
tar -xzf branch.tar.gz -C real_branch
TOP=$(ls -1 real_branch | head -1)
echo "top dir: $TOP"
echo "file count: $(find real_branch -type f | wc -l)"
