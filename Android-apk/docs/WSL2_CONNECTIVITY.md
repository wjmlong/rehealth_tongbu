# WSL2 ↔ Android 连通指南

WSL2 运行在独立的网络命名空间里。Android **模拟器**（跑在 Windows 主机）和**真机**
（同一 Wi-Fi）默认都无法直接访问 WSL2 内部的 JeecgBoot（`:8080`）与 model-service（`:8000`）。
本文说明如何打通。

## 1. 架构与 IP 映射

```text
[Android 模拟器] 10.0.2.2 ──(回环)──> [Windows 主机] ──(netsh 转发)──> [WSL2 IP]:8080
[Android 真机]  <WSL2_IP 或 主机LAN IP> ─────────────────────────────> [WSL2 IP]:8080
```

- 模拟器访问宿主机用特殊地址 `10.0.2.2`（不是 `localhost`）。
- WSL2 自身有个内网 IP（如 `172.28.x.x`），`localhost` 在 WSL2 内指向 WSL2 自己。

## 2. 一键脚本（推荐）

以 **管理员 PowerShell** 在项目根目录运行：

```powershell
# 建立转发（探测 WSL2 IP 并写 netsh portproxy）
.\tools\wsl2-android-connect.ps1

# 清理转发
.\tools\wsl2-android-connect.ps1 -Remove
```

脚本会：
1. `wsl hostname -I` 探测 WSL2 IP；
2. 对 `8080`(JeecgBoot)、`8000`(model-service) 建立
   `netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 connectaddress=<WSL2_IP>`；
3. 打印应写入 `Android-apk/local.properties` 的 `rehealth.api.base.url`：
   - 模拟器：`http://10.0.2.2:8080/jeecg-boot`
   - 真机：`http://<WSL2_IP>:8080/jeecg-boot`

## 3. 手动配置

### 3.1 让 WSL2 服务监听 0.0.0.0
确保 JeecgBoot 与 model-service 绑定 `0.0.0.0`（而非 `127.0.0.1`），否则只能 WSL2 内部访问。
JeecgBoot `application.yml`：`server.address: 0.0.0.0`（或省略），`server.port: 8080`。
model-service（FastAPI）：`uvicorn main:app --host 0.0.0.0 --port 8000`。

### 3.2 防火墙
Windows 防火墙需允许入站 `8080`/`8000`（真机场景）。可用：
```powershell
New-NetFirewallRule -DisplayName "ReHealth WSL2 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "ReHealth WSL2 8000" -Direction Inbound -LocalPort 8000 -Protocol TCP -Action Allow
```

### 3.3 端口转发（管理员）
```powershell
# 先取 WSL2 IP
$wslIp = (wsl hostname -I).Trim().Split()[0]
netsh interface portproxy add v4tov4 listenport=8080 listenaddress=0.0.0.0 connectport=8080 connectaddress=$wslIp
netsh interface portproxy add v4tov4 listenport=8000 listenaddress=0.0.0.0 connectport=8000 connectaddress=$wslIp
netsh interface portproxy show all
```

### 3.4 在 Android 端设置 base URL
编辑 `Android-apk/local.properties`（不入库）：
```
rehealth.api.base.url=http://10.0.2.2:8080/jeecg-boot   # 模拟器
# rehealth.api.base.url=http://172.28.x.x:8080/jeecg-boot  # 真机
```
Gradle 会把它注入 `BuildConfig.API_BASE_URL`，`ApiClient` 读取后构建 `ReHealthApi`。

## 4. 排错清单

| 现象 | 排查 |
|---|---|
| 模拟器请求超时 | 确认 `local.properties` 用的是 `10.0.2.2` 而非 `localhost`；确认 netsh 转发存在 |
| 真机连不上 | 用 `http://<WSL2_IP>:8080/jeecg-boot`；关闭/放行防火墙；确认 WSL2 服务监听 `0.0.0.0` |
| 401 未授权 | 先在 App「端侧健康模型」页用真实账号 `mLogin`；token 存于 `EncryptedSharedPreferences` |
| 返回 503 | 后台 hardware 持久化不可用，按后端契约重试同一 `batchId` |
| model-service 不可用 | `/rehealth/mobile/health` 看 `modelServiceAvailable`；`is_mock` 字段会标记演示数据 |

## 5. 验证一次完整调用
1. WSL2 内后端 + 模型服务已起，DB 迁移已执行；
2. Windows 跑 `wsl2-android-connect.ps1`；
3. `local.properties` 填好 base URL；
4. `./gradlew assembleDebug` 安装；
5. App → 端侧健康模型 → 登录后台 → 运行 CVD 风险评分 → 看到真实风险评分与贡献因素。
