# ReHealth 真机联调公网通道（dev-tunnel）

> 建立日期：2026-07-29。用途：让真机（手机流量/任意 Wi-Fi）访问开发机上的
> `jeecg-boot:8080`，用于 Android 真机端到端联调。**仅限开发/联调，禁止用于生产。**

## 1. 链路架构

```text
手机 (任意网络)
  -> http://rehealth.47.80.30.228.sslip.io  (DNS: sslip.io 通配解析 -> ECS 公网 IP)
  -> ECS nginx :80 (宝塔面板管理, vhost: /www/server/panel/vhost/nginx/rehealth.conf)
  -> proxy_pass http://127.0.0.1:18080
  -> sshd 反向隧道 (开发机发起, ssh -R 127.0.0.1:18080:127.0.0.1:8080, 走 22 端口)
  -> 开发机 127.0.0.1:8080 (jeecg-boot)
```

选型说明：

- 曾尝试 frp（frps 容器 + frpc），但 ECS 安全组未放行 7000/8081/8443；
  SSH 反向隧道走已放行的 22 端口，**零安全组/DNS 控制台操作**，故最终采用。
  frps 容器已从 ECS 移除。
- 域名用 `sslip.io` 通配（`rehealth.47.80.30.228.sslip.io` 自动解析到该 IP）。
  nginx vhost 已同时预留 `rehealth.youngjimmy.store`，将来在 DNS 商
  （当前 NS 在 myhostadmin）加一条 A 记录 `rehealth -> 47.80.30.228` 即可切换。
- ECS 上的 sub2api/nginx/postgres 等既有服务不受影响（仅新增独立 vhost 与
  loopback 端口 18080）。

## 2. 关键资产

| 资产 | 位置 | 说明 |
| --- | --- | --- |
| SSH 私钥 | `E:\aawjmlong\rehealth.pem`（不入库） | 阿里云密钥对 `rehealth`，登录用户为 `root` |
| ECS | `47.80.30.228`，Alibaba Cloud Linux 3，宝塔 nginx 1.28 | 实例 `7f8531c565e147d4a479621669e78235` |
| nginx vhost | ECS `/www/server/panel/vhost/nginx/rehealth.conf` | 副本见本目录 `rehealth_vhost.conf` |
| 隧道脚本 | `start_tunnel.ps1` | 杀旧进程后重建隧道，带 keep-alive |
| 开机自启 | `install_startup.ps1` | 写用户启动文件夹 VBS，免管理员 |

## 3. 日常操作

```powershell
# 启动/重启隧道（幂等，先杀旧进程）
powershell -NoProfile -ExecutionPolicy Bypass -File tools/dev-tunnel/start_tunnel.ps1

# 注册开机自启（用户登录时静默拉起）
powershell -NoProfile -ExecutionPolicy Bypass -File tools/dev-tunnel/install_startup.ps1

# 端到端自检（期望 200）
curl.exe -s -L -o NUL -w "%{http_code}" http://rehealth.47.80.30.228.sslip.io/jeecg-boot/
```

Android 真机 Debug 包配置（`Android-apk/local.properties`，不入库）：

```properties
rehealth.api.base.url=http\://rehealth.47.80.30.228.sslip.io/jeecg-boot/
```

Debug 构建 `usesCleartextTraffic=true`，允许明文 HTTP；Release 强制 HTTPS，
本通道不可用于 Release 验收（Release 需真实 HTTPS 域名 + 证书）。

## 4. 验证记录（2026-07-29）

| 检查项 | 结果 |
| --- | --- |
| ECS `ss -ltnp` 显示 sshd 监听 `127.0.0.1:18080` | 通过 |
| ECS 本地 `curl 127.0.0.1:18080/jeecg-boot/` | 302（正常重定向） |
| `nginx -t` + reload | 通过，不影响既有站点 |
| 公网 `GET /jeecg-boot/` | 302 -> `/jeecg-boot/doc.html` -> 200，0.43s |
| 公网业务 API `GET /jeecg-boot/sys/randomImage/{ts}` | `{"success":true,...}` |
| 开机自启注册 | `Startup\rehealth_tunnel.vbs` 已写入 |

## 5. 故障排查

- 手机访问 502：多为开发机隧道断了。重跑 `start_tunnel.ps1`；确认本机
  `jeecg-boot:8080` 在监听。
- 隧道反复断：检查 ECS `sshd` 是否重启过；脚本自带
  `ServerAliveInterval=30` keep-alive，掉线后需重新拉起（脚本幂等）。
- ECS 侧 18080 被残留占用：`ExitOnForwardFailure=yes` 会导致新隧道退出；
  登录 ECS `ss -ltnp | grep 18080` 找到旧 sshd 进程 kill 后重连。

## 6. 安全边界

- 私钥、token、`local.properties` 一律不入 Git。
- 隧道仅转发 loopback 18080，ECS 公网只暴露 nginx 80/443（原有面）。
- dev 通道为明文 HTTP，禁止在其上传输生产用户数据。
