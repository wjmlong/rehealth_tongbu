#!/bin/sh
set -eu

manifest="$1"
signature="$2"
public_key="$3"
result="$4"

cosign verify-blob \
  --insecure-ignore-tlog=true \
  --key "$public_key" \
  --signature "$signature" \
  "$manifest" >/dev/null
jq -e '.artifacts | type == "array" and length > 0' "$manifest" >/dev/null

jq -r '.artifacts[] | [.path, .sha256] | @tsv' "$manifest" |
while IFS="$(printf '\t')" read -r relative_path expected_sha; do
  case "$relative_path" in
    ""|/*|*../*|../*)
      echo "unsafe artifact path" >&2
      exit 65
      ;;
  esac
  actual_sha="$(sha256sum "$(dirname "$manifest")/$relative_path" | awk '{print $1}')"
  test "$actual_sha" = "$expected_sha" || {
    echo "artifact hash mismatch" >&2
    exit 66
  }
done

mkdir -p "$(dirname "$result")"
temporary="$result.tmp"
sha256sum "$manifest" | awk '{print $1}' >"$temporary"
mv "$temporary" "$result"
