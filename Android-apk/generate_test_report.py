#!/usr/bin/env python3
"""
ReHealth Comprehensive Test Report Generator
Generates detailed reports based on all available data
"""

import os
import json
import subprocess
from datetime import datetime
from pathlib import Path

OUTPUT_DIR = "/mnt/d/rehealthAI/outputs"
PROJECT_DIR = "/mnt/d/rehealthAI/Android-apk"

def gather_system_info():
    """Collect system information"""
    info = {}

    try:
        info['hostname'] = subprocess.check_output(['hostname'], text=True).strip()
    except:
        info['hostname'] = 'unknown'

    try:
        info['os'] = subprocess.check_output(['uname', '-a'], text=True).strip()
    except:
        info['os'] = 'unknown'

    try:
        info['python_version'] = subprocess.check_output(['python3', '--version'], text=True).strip()
    except:
        info['python_version'] = 'unknown'

    return info

def check_build_status():
    """Check if APK was built"""
    apk_path = Path(PROJECT_DIR) / "app/build/outputs/apk/debug/app-debug.apk"

    if apk_path.exists():
        size = apk_path.stat().st_size
        return {
            'status': 'success',
            'path': str(apk_path),
            'size_mb': round(size / 1024 / 1024, 2),
            'size_bytes': size
        }
    else:
        return {
            'status': 'pending',
            'path': str(apk_path),
            'message': 'APK not found - build may be in progress'
        }

def check_backend_status():
    """Check backend service status"""
    try:
        result = subprocess.run(
            ['pgrep', '-f', 'uvicorn.*api.main'],
            capture_output=True,
            text=True
        )

        if result.returncode == 0:
            pids = result.stdout.strip().split('\n')
            return {
                'status': 'running',
                'pids': pids,
                'count': len(pids)
            }
        else:
            return {
                'status': 'stopped',
                'message': 'Backend service not running'
            }
    except Exception as e:
        return {
            'status': 'error',
            'message': str(e)
        }

def check_output_files():
    """Check what output files have been generated"""
    output_path = Path(OUTPUT_DIR)
    output_path.mkdir(parents=True, exist_ok=True)

    files = {}

    expected_files = [
        'automation.log',
        'backend.log',
        'automation_status.md',
        'INSTALL_GUIDE.txt',
        'BUILD_TRIGGER.txt',
        'APK_READY.txt',
        'screenshot.png',
        'rehealth_logs.txt',
        'crash_logs.txt'
    ]

    for filename in expected_files:
        filepath = output_path / filename
        if filepath.exists():
            files[filename] = {
                'exists': True,
                'size': filepath.stat().st_size,
                'modified': datetime.fromtimestamp(filepath.stat().st_mtime).isoformat()
            }
        else:
            files[filename] = {
                'exists': False
            }

    return files

def read_log_summary(log_file, max_lines=50):
    """Read last N lines of a log file"""
    log_path = Path(OUTPUT_DIR) / log_file

    if not log_path.exists():
        return None

    try:
        with open(log_path, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()
            return ''.join(lines[-max_lines:])
    except Exception as e:
        return f"Error reading log: {e}"

def generate_json_report():
    """Generate machine-readable JSON report"""
    report = {
        'generated_at': datetime.now().isoformat(),
        'system': gather_system_info(),
        'build': check_build_status(),
        'backend': check_backend_status(),
        'output_files': check_output_files()
    }

    report_path = Path(OUTPUT_DIR) / 'test_report.json'
    with open(report_path, 'w') as f:
        json.dump(report, f, indent=2)

    return report_path

def generate_markdown_report():
    """Generate human-readable Markdown report"""

    system_info = gather_system_info()
    build_status = check_build_status()
    backend_status = check_backend_status()
    output_files = check_output_files()

    report = f"""# ReHealth 完整测试报告

**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

---

## 📊 执行总结

### 系统信息
- **主机**: {system_info['hostname']}
- **操作系统**: {system_info['os']}
- **Python版本**: {system_info['python_version']}

### 构建状态
"""

    if build_status['status'] == 'success':
        report += f"""- ✅ **状态**: 构建成功
- **APK路径**: `{build_status['path']}`
- **文件大小**: {build_status['size_mb']} MB ({build_status['size_bytes']:,} bytes)
"""
    else:
        report += f"""- ⏸️ **状态**: {build_status['status']}
- **说明**: {build_status.get('message', 'APK未找到')}
- **预期路径**: `{build_status['path']}`
"""

    report += "\n### 后端服务\n"

    if backend_status['status'] == 'running':
        report += f"""- ✅ **状态**: 运行中
- **进程数**: {backend_status['count']}
- **PID**: {', '.join(backend_status['pids'])}
- **端口**: 8000
- **URL**: http://localhost:8000 (http://10.0.2.2:8000 from emulator)
"""
    else:
        report += f"""- ⏸️ **状态**: {backend_status['status']}
- **说明**: {backend_status.get('message', '未运行')}
"""

    report += "\n---\n\n## 📁 输出文件\n\n"

    for filename, info in output_files.items():
        if info['exists']:
            size_kb = info['size'] / 1024
            report += f"- ✅ **{filename}** ({size_kb:.1f} KB) - 修改时间: {info['modified']}\n"
        else:
            report += f"- ⏸️ **{filename}** - 未生成\n"

    report += "\n---\n\n## 📝 日志摘要\n\n"

    # Automation log
    report += "### 自动化日志 (最近50行)\n\n```\n"
    auto_log = read_log_summary('automation.log', 50)
    if auto_log:
        report += auto_log
    else:
        report += "日志文件未找到\n"
    report += "```\n\n"

    # Backend log
    report += "### 后端服务日志 (最近50行)\n\n```\n"
    backend_log = read_log_summary('backend.log', 50)
    if backend_log:
        report += backend_log
    else:
        report += "日志文件未找到或服务未启动\n"
    report += "```\n\n"

    report += "---\n\n## 🎯 任务完成情况\n\n"

    tasks = [
        ("等待APK重新构建完成", build_status['status'] == 'success'),
        ("安装新版本APK到模拟器", False),  # Manual step
        ("验证应用启动成功", False),  # Manual step
        ("执行完整的功能和性能测试", False),  # Manual step
        ("生成测试报告和总结", True),  # This report
        ("启动后端服务", backend_status['status'] == 'running')
    ]

    completed = sum(1 for _, status in tasks if status)
    total = len(tasks)

    report += f"**完成度**: {completed}/{total} ({completed*100//total}%)\n\n"

    for task, status in tasks:
        icon = "✅" if status else "⏸️"
        report += f"- {icon} {task}\n"

    report += "\n---\n\n## 🚀 下一步操作\n\n"

    if build_status['status'] == 'success':
        report += """### APK已构建完成 ✅

请按以下步骤继续：

1. **安装APK**
   ```bash
   adb install -r {apk_path}
   ```

2. **启动应用**
   ```bash
   adb shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1
   ```

3. **运行完整测试**
   ```bash
   cd {project_dir}
   ./complete_integration_test.sh
   ```

4. **查看实时日志**
   ```bash
   adb logcat | grep ReHealth
   ```
""".format(apk_path=build_status['path'], project_dir=PROJECT_DIR)
    else:
        report += """### APK构建待完成 ⏸️

构建可能仍在进行中，或需要手动干预。

**选项1：等待自动构建完成**
- 监控文件：{build_trigger}
- 构建完成后会自动更新状态

**选项2：手动构建（Windows环境）**
```powershell
cd D:\\rehealthAI\\Android-apk
.\\gradlew.bat assembleDebug
```

或运行：
```powershell
.\\full_automation.bat
```

**选项3：检查构建日志**
- 查看是否有构建错误
- 检查SDK配置是否正确
""".format(build_trigger=Path(PROJECT_DIR) / "BUILD_TRIGGER.txt")

    report += "\n---\n\n## 📞 支持信息\n\n"

    report += """### 关键文件位置

- **输出目录**: `{output_dir}`
- **项目目录**: `{project_dir}`
- **APK位置**: `{apk_path}`
- **测试脚本**: `{test_script}`

### 常见问题

**Q: 构建失败怎么办？**
A: 检查以下配置文件是否正确：
- `local.properties` - SDK路径是否为WSL格式
- `build.gradle.kts` - baseUrl是否有结尾斜杠

**Q: 后端服务无法启动？**
A: 检查依赖是否安装：
```bash
cd {backend_dir}
source venv/bin/activate
pip install -r requirements.txt
```

**Q: 应用崩溃？**
A: 查看崩溃日志：
```bash
adb logcat | grep -A 10 "FATAL EXCEPTION"
```

### 详细文档

完整的技术文档和分析报告：
- `{output_dir}/final_delivery_report.md` (最详细)
- `{output_dir}/EXECUTIVE_SUMMARY.md` (执行总结)
- `{output_dir}/page_api_capability_mapping.md` (API映射)

---

**报告生成器版本**: 1.0
**生成工具**: Claude Code Automation
**下次更新**: 运行 `python3 generate_test_report.py`
""".format(
        output_dir=OUTPUT_DIR,
        project_dir=PROJECT_DIR,
        apk_path=build_status['path'],
        test_script=Path(PROJECT_DIR) / "complete_integration_test.sh",
        backend_dir="/mnt/d/rehealthAI/rehealth-algorithms"
    )

    report_path = Path(OUTPUT_DIR) / 'comprehensive_test_report.md'
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)

    return report_path

def main():
    """Main report generation"""
    print("=" * 60)
    print("ReHealth Test Report Generator")
    print("=" * 60)
    print()

    # Ensure output directory exists
    Path(OUTPUT_DIR).mkdir(parents=True, exist_ok=True)

    # Generate JSON report
    print("Generating JSON report...")
    json_path = generate_json_report()
    print(f"✓ JSON report saved: {json_path}")

    # Generate Markdown report
    print("Generating Markdown report...")
    md_path = generate_markdown_report()
    print(f"✓ Markdown report saved: {md_path}")

    print()
    print("=" * 60)
    print("Report generation complete!")
    print("=" * 60)
    print()
    print(f"View reports:")
    print(f"  - {md_path}")
    print(f"  - {json_path}")
    print()

    # Display summary
    build_status = check_build_status()
    backend_status = check_backend_status()

    print("Quick Summary:")
    print(f"  Build: {build_status['status']}")
    print(f"  Backend: {backend_status['status']}")
    print()

if __name__ == '__main__':
    main()
