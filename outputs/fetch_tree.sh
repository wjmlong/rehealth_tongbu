#!/usr/bin/env bash
cd /tmp
rm -rf real_branch
mkdir -p real_branch
REPO="rehealthAI/Android-apk"
REF="codex/real-device"
echo "=== fetch tree (recursive) ==="
gh api "repos/${REPO}/git/trees/${REF}?recursive=1" --jq '.tree[] | "\(.type)\t\(.path)"' > /tmp/tree.txt
TOTAL=$(wc -l < /tmp/tree.txt)
echo "total tree entries: ${TOTAL}"
# blobs only, excluding binary/vendor artifacts we don't need for a source diff
BLOBS=$(awk -F'\t' '$1=="blob"{print $2}' /tmp/tree.txt \
  | grep -viE '\.(aar|jar|bin|apk|png|jpe?g|gif|ico|ttf|woff2?|zip|keystore|so|mp4|wav)$')
echo "source blobs to fetch: $(echo "$BLOBS" | wc -l)"
COUNT=0
while IFS= read -r p; do
  dir=$(dirname "$p")
  mkdir -p "real_branch/$dir"
  gh api "repos/${REPO}/contents/${p}?ref=${REF}" --jq '.content' 2>/dev/null | base64 -d > "real_branch/$p"
  COUNT=$((COUNT+1))
done < <(printf '%s\n' "$BLOBS")
echo "fetched files: ${COUNT}"
echo "real_branch file count: $(find real_branch -type f | wc -l)"
