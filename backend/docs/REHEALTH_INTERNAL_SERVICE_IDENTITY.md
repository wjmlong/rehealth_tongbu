# ReHealth 内部服务身份契约

## 目的

Device Service 不得信任移动客户端提供的 `userId`、租户信息或已解析身份请求头。在接受或读取遥测数据前，Device Service 必须调用 JeecgBoot 解析原始 Jeecg 会话，并验证当前设备绑定是否获得授权。

## 端点

```http
POST /rehealth/internal/v1/identity/authorize-device
X-ReHealth-Service-Credential: ${REHEALTH_INTERNAL_AUTH_SERVICE_CREDENTIAL}
X-Access-Token: <原始 Jeecg 用户会话>
Content-Type: application/json

{
  "tenantId": "0",
  "deviceId": "mrd-device-id"
}
```

成功响应：

```json
{
  "authorized": true,
  "code": "AUTHORIZED",
  "userId": "server-resolved-user-id",
  "tenantId": "0",
  "deviceId": "mrd-device-id"
}
```

`userId` 始终从通过验证的 Jeecg 令牌中解析。租户成员关系来自服务端的 `LoginUser.relTenantIds` 值；没有显式租户列表的账号仅属于 Jeecg 默认租户 `0`。设备归属通过状态为有效（`BOUND`）的 `rehealth_device_binding` 记录校验。

## 安全行为

- 通过部署密钥提供方配置 `REHEALTH_INTERNAL_AUTH_SERVICE_CREDENTIAL`。空值会以失败关闭方式禁用授权。绝不能提交其真实值。
- 服务凭据缺失或错误时返回 `403`。
- 用户令牌缺失、过期、已撤销或因其他原因无效时返回 `401`。
- 租户不匹配、设备未绑定、设备属于其他用户，或请求提供了任何 `X-ReHealth-User-Id`、`X-ReHealth-Tenant-Id`、`X-ReHealth-Device-Id` 请求头时，返回 `403`。
- 身份提供方或绑定存储失败时返回 `503`；Device Service 不得将该请求作为已授权请求入队或持久化。
- 端点不记录令牌或凭据材料，也不缓存凭据、用户令牌、身份解析结果或绑定判定。因此，退出登录、解绑和重新绑定会在下一次调用时生效，正向缓存最长有效期为零秒。
- Gateway 在路由所有外部请求前必须移除上述三个已解析身份请求头。即使 Gateway 配置错误，Jeecg 端点也会拒绝这些请求头，形成纵深防御。

Device Service 只能在当前操作中使用成功响应。不得序列化或缓存 `X-Access-Token` 或服务凭据。轮换服务凭据时，应更新密钥提供方，并在部署发布过程中重启 JeecgBoot 和 Device Service。

## 验证

在已配置 Java 17 的环境中，从仓库根目录运行：

```powershell
mvn `
  -f backend\jeecg-boot\pom.xml `
  -pl jeecg-boot-module\jeecg-module-rehealth `
  -Dtest=InternalIdentityAndDeviceAuthorizationIT test
```

发布门禁使用的对抗性用例选择：

```powershell
mvn `
  -f backend\jeecg-boot\pom.xml `
  -pl jeecg-boot-module\jeecg-module-rehealth `
  -Dtest=InternalIdentityAndDeviceAuthorizationIT `
  -Dcases=revoked,cross_device,unbound,spoofed_header,auth_unavailable test
```
