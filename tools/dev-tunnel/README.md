# ReHealth 真机联调公网通道（dev-tunnel）

> 建立日期：2026-07-29（同日升级 HTTPS）。用途：让真机（手机流量/任意 Wi-Fi）访问
> 开发机上的 `jeecg-boot:8080`，用于 Android 真机端到端联调。**仅限开发/联调，禁止用于生产。**

## 1. 链路架构

```text
手机 (任意网络)
  -> https://rehealth.47.80.30.228.sslip.io  (DNS: sslip.io 通配解析 -> ECS 公网 IP)
  -> ECS nginx :443 TLS 终止 (Let's Encrypt 证书, acme.sh 自动续期)
     (:80 仅保留 ACME 验证路径，其余 301 跳转 HTTPS)
  -> proxy_pass http://127.0.0.1:18080
  -> sshd 反向隧道 (开发机发起, ssh -R 127.0.0.1:18080:127.0.0.1:8080, 走 22 端口)
  -> 开发机 127.0.0.1:8080 (jeecg-boot)
```

选型说明：

- 曾尝试 frp（frps 容器 + frpc）。后经临时监听测试确认安全组 7000/8081/8443
  实际已放行，但 SSH 反向隧道已稳定工作、攻击面更小（frp 需在 ECS 常驻服务端
  并管理 token），故维持 SSH 隧道方案，frps 容器保持移除状态。
- **正式联调域名：`rehealth.youngjimmy.store`**（2026-07-29 在西部数码
  west.cn 添加 A 记录 -> 47.80.30.228，权威 NS 为 ns1/ns2.myhostadmin.net，
  已验证生效）。`rehealth.47.80.30.228.sslip.io` 作为备用（sslip.io 通配
  解析，无 DNS 依赖）。
- 证书为**单张多域名（SAN）证书**，同时覆盖上述两个域名，acme.sh 主域名
  记录为 `rehealth.47.80.30.228.sslip.io`（续期时两个域名一起续）。
- ECS 上的 sub2api/nginx/postgres 等既有服务不受影响（仅新增独立 vhost 与
  loopback 端口 18080）。

## 2. 关键资产

| 资产 | 位置 | 说明 |
| --- | --- | --- |
| SSH 私钥 | `E:\aawjmlong\rehealth.pem`（不入库） | 阿里云密钥对 `rehealth`，登录用户为 `root` |
| ECS | `47.80.30.228`，Alibaba Cloud Linux 3，宝塔 nginx 1.28 | 实例 `7f8531c565e147d4a479621669e78235` |
| nginx vhost | ECS `/www/server/panel/vhost/nginx/rehealth.conf` | 副本见本目录 `rehealth_vhost.conf` |
| TLS 证书 | ECS `/etc/nginx/ssl/rehealth/{fullchain.pem,key.pem}` | acme.sh (Let's Encrypt)，cron 自动续期并 `nginx -s reload` |
| acme.sh | ECS `/root/.acme.sh/` | webroot 验证目录 `/www/wwwroot/rehealth_acme` |
| 隧道脚本 | `start_tunnel.ps1` | 杀旧进程后重建隧道，带 keep-alive |
| 开机自启 | `install_startup.ps1` | 写用户启动文件夹 VBS，免管理员 |

## 3. 日常操作

```powershell
# 启动/重启隧道（幂等，先杀旧进程）
powershell -NoProfile -ExecutionPolicy Bypass -File tools/dev-tunnel/start_tunnel.ps1

# 注册开机自启（用户登录时静默拉起）
powershell -NoProfile -ExecutionPolicy Bypass -File tools/dev-tunnel/install_startup.ps1

# 端到端自检（期望 200）
curl.exe -s -L -o NUL -w "%{http_code}" https://rehealth.youngjimmy.store/jeecg-boot/
```

Android 真机配置（`Android-apk/local.properties`，不入库）：

```properties
rehealth.api.base.url=https\://rehealth.youngjimmy.store/jeecg-boot/
```

通道已升级为 HTTPS（受信 Let's Encrypt 证书），Debug 与 Release 构建均可
通过此通道联调/验收网络链路。注意：Release 正式发布仍应指向生产后端域名，
本通道回源的是开发机，仅用于联调与验收演练。

## 4. 验证记录

2026-07-29（初次打通，HTTP）：

| 检查项 | 结果 |
| --- | --- |
| ECS `ss -ltnp` 显示 sshd 监听 `127.0.0.1:18080` | 通过 |
| ECS 本地 `curl 127.0.0.1:18080/jeecg-boot/` | 302（正常重定向） |
| `nginx -t` + reload | 通过，不影响既有站点 |
| 公网 `GET /jeecg-boot/` | 302 -> `/jeecg-boot/doc.html` -> 200，0.43s |
| 公网业务 API `GET /jeecg-boot/sys/randomImage/{ts}` | `{"success":true,...}` |
| 开机自启注册 | `Startup\rehealth_tunnel.vbs` 已写入 |

2026-07-29（HTTPS 升级）：

| 检查项 | 结果 |
| --- | --- |
| 安全组临时监听测试（ECS 起 python http.server:7000，外网连通） | 7000 已放行（此前不通是无监听，非安全组） |
| Let's Encrypt 证书签发（http-01 webroot） | 成功，续期窗口 2026-09-27 |
| `http://.../jeecg-boot/` | 301 -> `https://` |
| `https://.../jeecg-boot/` | 200（doc.html），SSL_VERIFY=0（证书受信），1.35s |
| HTTPS 业务 API `GET /jeecg-boot/sys/randomImage/{ts}` | `{"success":true,...}` |
| nginx 443 监听 + 18080 隧道 | 均正常 |

2026-07-29（正式域名切换）：

| 检查项 | 结果 |
| --- | --- |
| 权威 NS（ns1/ns2.myhostadmin.net）A 记录 | `rehealth.youngjimmy.store -> 47.80.30.228`，8.8.8.8/223.5.5.5 同步生效 |
| SAN 证书重签（两域名合一） | 成功，`DNS:rehealth.47.80.30.228.sslip.io, DNS:rehealth.youngjimmy.store` |
| `http://rehealth.youngjimmy.store/jeecg-boot/` | 301 -> `https://` |
| `https://rehealth.youngjimmy.store/jeecg-boot/` | 200（doc.html），SSL_VERIFY=0，1.23s |
| HTTPS 业务 API（正式域名） | `{"success":true,...}` |
| 备用域名 sslip.io 回归 | 200，仍可用 |

## 5. 故障排查

- 手机访问 502：多为开发机隧道断了。重跑 `start_tunnel.ps1`；确认本机
  `jeecg-boot:8080` 在监听。
- 隧道反复断：检查 ECS `sshd` 是否重启过；脚本自带
  `ServerAliveInterval=30` keep-alive，掉线后需重新拉起（脚本幂等）。
- ECS 侧 18080 被残留占用：`ExitOnForwardFailure=yes` 会导致新隧道退出；
  登录 ECS `ss -ltnp | grep 18080` 找到旧 sshd 进程 kill 后重连。
- 证书过期/浏览器告警：登录 ECS 执行 `/root/.acme.sh/acme.sh --renew -d
  rehealth.47.80.30.228.sslip.io --ecc --force`，正常情况 cron 会自动续期。

## 6. 安全边界

- 私钥、token、`local.properties` 一律不入 Git。
- 隧道仅转发 loopback 18080，ECS 公网只暴露 nginx 80/443（原有面）。
- 公网入口已启用 TLS（80 强制 301 至 443）；回源段（nginx->sshd 隧道->开发机）
  在 SSH 加密信道内。
- 本通道回源开发机，禁止在其上传输生产用户数据。
