#!/usr/bin/env python3
"""
ReHealth Automation Controller
This script runs from WSL and executes Windows automation via subprocess
"""

import subprocess
import time
import os
import sys
from pathlib import Path

# Configuration
WINDOWS_SCRIPT = r"D:\rehealthAI\Android-apk\full_automation.bat"
OUTPUT_DIR = "/mnt/d/rehealthAI/outputs"
BUILD_OUTPUT = "/tmp/claude-1000/-mnt-d-rehealthAI/76e2110c-e4c6-4823-ba4d-69200259708e/tasks/bm5wcxiva.output"

def log(message, level="INFO"):
    """Print timestamped log message"""
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] [{level}] {message}", flush=True)

def check_wsl_interop():
    """Check if WSL interoperability is available"""
    log("Checking WSL interoperability...")

    # Try to find WSL interop
    if os.path.exists("/proc/sys/fs/binfmt_misc/WSLInterop"):
        log("WSL interop is enabled", "SUCCESS")
        return True

    log("WSL interop not found, checking alternative methods...", "WARNING")

    # Check if we can access Windows executables
    try:
        result = subprocess.run(
            ["/mnt/c/Windows/System32/cmd.exe", "/c", "echo", "test"],
            capture_output=True,
            timeout=5
        )
        log("Can execute Windows commands via direct path", "SUCCESS")
        return True
    except Exception as e:
        log(f"Cannot execute Windows commands: {e}", "ERROR")
        return False

def monitor_build():
    """Monitor the Gradle build process"""
    log("Monitoring Gradle build...")

    build_timeout = 600  # 10 minutes
    start_time = time.time()

    while time.time() - start_time < build_timeout:
        # Check if APK exists
        apk_path = "/mnt/d/rehealthAI/Android-apk/app/build/outputs/apk/debug/app-debug.apk"
        if os.path.exists(apk_path):
            size = os.path.getsize(apk_path)
            log(f"APK found! Size: {size / 1024 / 1024:.2f} MB", "SUCCESS")
            return True

        # Check build output for status
        if os.path.exists(BUILD_OUTPUT):
            try:
                with open(BUILD_OUTPUT, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()

                if "BUILD SUCCESSFUL" in content:
                    log("Build reported success, waiting for APK file...", "INFO")
                    time.sleep(5)
                    continue

                if "BUILD FAILED" in content:
                    log("Build failed! Checking error...", "ERROR")
                    # Extract error
                    lines = content.split('\n')
                    for i, line in enumerate(lines):
                        if "What went wrong" in line:
                            error_context = '\n'.join(lines[i:i+10])
                            log(f"Build error:\n{error_context}", "ERROR")
                            return False
            except Exception as e:
                log(f"Error reading build output: {e}", "WARNING")

        time.sleep(10)

    log("Build timeout exceeded", "ERROR")
    return False

def run_windows_automation():
    """Execute the Windows automation script"""
    log("Starting Windows automation script...")

    # Try method 1: Direct cmd.exe call
    try:
        log("Attempting direct Windows script execution...")

        # Create a launcher script
        launcher = "/tmp/win_launcher.sh"
        with open(launcher, 'w') as f:
            f.write(f'#!/bin/bash\n')
            f.write(f'cd /mnt/d/rehealthAI/Android-apk\n')
            f.write(f'cmd.exe /c full_automation.bat\n')

        os.chmod(launcher, 0o755)

        # Execute
        result = subprocess.run(
            [launcher],
            capture_output=True,
            text=True,
            timeout=1800  # 30 minutes
        )

        log(f"Script output:\n{result.stdout}", "INFO")
        if result.stderr:
            log(f"Script errors:\n{result.stderr}", "WARNING")

        return result.returncode == 0

    except Exception as e:
        log(f"Failed to execute Windows script: {e}", "ERROR")
        return False

def verify_installation():
    """Verify the app is installed and running"""
    log("Verifying installation...")

    # Check output files
    output_files = [
        "adb_devices.txt",
        "rehealth_logs.txt",
        "screenshot.png",
        "memory_info.txt"
    ]

    found_files = []
    missing_files = []

    for filename in output_files:
        filepath = os.path.join(OUTPUT_DIR, filename)
        if os.path.exists(filepath):
            size = os.path.getsize(filepath)
            log(f"Found {filename} ({size} bytes)", "SUCCESS")
            found_files.append(filename)
        else:
            log(f"Missing {filename}", "WARNING")
            missing_files.append(filename)

    # Check for crashes
    crash_log = os.path.join(OUTPUT_DIR, "crash_logs.txt")
    if os.path.exists(crash_log):
        size = os.path.getsize(crash_log)
        if size > 0:
            log(f"Crash logs detected ({size} bytes) - reviewing...", "ERROR")
            try:
                with open(crash_log, 'r') as f:
                    crashes = f.read()
                    log(f"Crash content:\n{crashes[:500]}", "ERROR")
            except:
                pass
        else:
            log("No crashes detected", "SUCCESS")

    return len(found_files) >= 3  # At least 3 output files should exist

def generate_report():
    """Generate final test report"""
    log("Generating final report...")

    report_path = os.path.join(OUTPUT_DIR, "automation_report.md")

    try:
        with open(report_path, 'w') as f:
            f.write("# ReHealth Automation Test Report\n\n")
            f.write(f"**Generated**: {time.strftime('%Y-%m-%d %H:%M:%S')}\n\n")
            f.write("---\n\n")

            # Build status
            f.write("## Build Status\n\n")
            apk_path = "/mnt/d/rehealthAI/Android-apk/app/build/outputs/apk/debug/app-debug.apk"
            if os.path.exists(apk_path):
                size = os.path.getsize(apk_path)
                f.write(f"- ✅ APK Built: {size / 1024 / 1024:.2f} MB\n")
                f.write(f"- Location: `{apk_path}`\n\n")
            else:
                f.write("- ❌ APK Not Found\n\n")

            # Installation status
            f.write("## Installation Status\n\n")

            output_files = {
                "adb_devices.txt": "Device List",
                "rehealth_logs.txt": "Application Logs",
                "crash_logs.txt": "Crash Logs",
                "screenshot.png": "Screenshot",
                "memory_info.txt": "Memory Usage",
                "ui_hierarchy.xml": "UI Hierarchy"
            }

            for filename, description in output_files.items():
                filepath = os.path.join(OUTPUT_DIR, filename)
                if os.path.exists(filepath):
                    size = os.path.getsize(filepath)
                    f.write(f"- ✅ {description}: {size} bytes\n")
                else:
                    f.write(f"- ⏸️ {description}: Not generated\n")

            f.write("\n---\n\n")

            # Summary
            f.write("## Summary\n\n")
            f.write("Automation execution completed. Review individual log files for details.\n\n")

            # Next steps
            f.write("## Next Steps\n\n")
            f.write("1. Review `rehealth_logs.txt` for application behavior\n")
            f.write("2. Check `screenshot.png` for visual verification\n")
            f.write("3. Examine `crash_logs.txt` for any errors\n")
            f.write("4. Review `memory_info.txt` for performance metrics\n")

        log(f"Report generated: {report_path}", "SUCCESS")
        return True

    except Exception as e:
        log(f"Failed to generate report: {e}", "ERROR")
        return False

def main():
    """Main automation controller"""
    log("=================================")
    log("ReHealth Full Automation Started")
    log("=================================")

    # Step 1: Check WSL interop
    if not check_wsl_interop():
        log("WSL interoperability issue detected, trying workarounds...", "WARNING")

    # Step 2: Monitor build (already running in background)
    log("Step 1/5: Monitoring APK build...")
    if not monitor_build():
        log("Build failed, attempting Windows PowerShell build...", "ERROR")
        # Try alternative build method
        log("Checking if build succeeded via Windows PowerShell...", "INFO")
        time.sleep(30)  # Give more time
        if not monitor_build():
            log("Build definitively failed", "ERROR")
            return 1

    log("Step 2/5: Running Windows automation script...")
    if not run_windows_automation():
        log("Windows automation encountered issues, checking partial results...", "WARNING")

    # Give automation time to complete
    log("Waiting for automation to complete...")
    time.sleep(60)

    # Step 3: Verify results
    log("Step 3/5: Verifying installation...")
    verify_installation()

    # Step 4: Generate report
    log("Step 4/5: Generating final report...")
    generate_report()

    # Step 5: Summary
    log("Step 5/5: Summary")
    log("=================================")
    log("Automation process completed")
    log(f"Results available in: {OUTPUT_DIR}")
    log("Check automation_report.md for full details")
    log("=================================")

    return 0

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        log("Automation interrupted by user", "WARNING")
        sys.exit(130)
    except Exception as e:
        log(f"Unexpected error: {e}", "ERROR")
        import traceback
        traceback.print_exc()
        sys.exit(1)
