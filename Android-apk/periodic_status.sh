#!/bin/bash
# Periodic Status Update Generator
# Runs every hour to generate status updates

OUTPUT_DIR="/mnt/d/rehealthAI/outputs"
PROJECT_DIR="/mnt/d/rehealthAI/Android-apk"

generate_status() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local status_file="$OUTPUT_DIR/status_$(date +%Y%m%d_%H%M%S).md"

    cat > "$status_file" << EOF
# ReHealth Automation Status Update

**Generated**: $timestamp

---

## Process Status

### Running Processes
$(ps aux | grep -E "overnight_automation|uvicorn|python.*generate" | grep -v grep | awk '{printf "- PID %s: %s (CPU: %s%%, MEM: %s%%)\n", $2, $11, $3, $4}')

### APK Build
$(if [ -f "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "- ✅ Complete"
    echo "- Size: $(du -h "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" | cut -f1)"
else
    echo "- ⏸️ In Progress"
fi)

### Backend Service
$(if ps aux | grep -q "[u]vicorn.*api.main"; then
    echo "- ✅ Running"
    echo "- PID: $(pgrep -f 'uvicorn.*api.main')"
else
    echo "- ⏸️ Stopped"
fi)

### Output Files Generated
$(ls -lh "$OUTPUT_DIR" | tail -n +2 | wc -l) files in output directory

### Disk Usage
Output directory: $(du -sh "$OUTPUT_DIR" | cut -f1)
Project directory: $(du -sh "$PROJECT_DIR" | cut -f1)

---

## Recent Activity (Last 10 log lines)
\`\`\`
$(tail -10 "$OUTPUT_DIR/overnight_master.log" 2>/dev/null || echo "Log not available")
\`\`\`

---

**Next update**: $(date -d '+1 hour' '+%Y-%m-%d %H:%M:%S')
EOF

    echo "$status_file"
}

# Generate initial status
echo "Generating periodic status updates..."
while true; do
    status_file=$(generate_status)
    echo "Status generated: $status_file"

    # Also update latest symlink
    ln -sf "$status_file" "$OUTPUT_DIR/status_latest.md"

    # Generate JSON summary
    python3 "$PROJECT_DIR/generate_test_report.py" > /dev/null 2>&1 || true

    # Wait 1 hour
    sleep 3600
done
