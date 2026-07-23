# ReHealth internal service identity contract

## Purpose

The Device Service must not trust `userId`, tenant or resolved-identity headers supplied by a mobile client. Before it accepts or reads telemetry, it calls JeecgBoot to resolve the original Jeecg session and authorize the active device binding.

## Endpoint

```http
POST /rehealth/internal/v1/identity/authorize-device
X-ReHealth-Service-Credential: ${REHEALTH_INTERNAL_AUTH_SERVICE_CREDENTIAL}
X-Access-Token: <original Jeecg user session>
Content-Type: application/json

{
  "tenantId": "0",
  "deviceId": "mrd-device-id"
}
```

Successful response:

```json
{
  "authorized": true,
  "code": "AUTHORIZED",
  "userId": "server-resolved-user-id",
  "tenantId": "0",
  "deviceId": "mrd-device-id"
}
```

`userId` is always resolved from the verified Jeecg token. Tenant membership comes from the server-side `LoginUser.relTenantIds` value; an account without an explicit tenant list belongs only to Jeecg's default tenant `0`. Device ownership is checked against an active (`BOUND`) `rehealth_device_binding` row.

## Security behavior

- Configure `REHEALTH_INTERNAL_AUTH_SERVICE_CREDENTIAL` through the deployment secret provider. An empty value disables authorization by failing closed. Never commit its value.
- Missing or wrong service credential returns `403`.
- Missing, expired, revoked or otherwise invalid user token returns `401`.
- Tenant mismatch, unbound device, another user's device or any supplied `X-ReHealth-User-Id`, `X-ReHealth-Tenant-Id` or `X-ReHealth-Device-Id` header returns `403`.
- Identity-provider or binding-store failure returns `503`; the Device Service must not enqueue or persist the request as authorized.
- The endpoint does not log token or credential material and does not cache credentials, user tokens, identity results or binding decisions. Therefore logout, unbind and rebind take effect on the next call and the maximum positive cache age is zero seconds.
- Gateway must remove the three resolved-identity headers from all external requests before routing. The Jeecg endpoint rejects them as defense in depth even if Gateway is misconfigured.

The Device Service may use the successful response only for the current operation. It must not serialize or cache `X-Access-Token` or the service credential. Service credential rotation is performed by updating the secret provider and restarting JeecgBoot and Device Service within the deployment rollout.

## Verification

From the repository root with Java 17 configured:

```powershell
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd `
  -f backend\jeecg-boot\pom.xml `
  -pl jeecg-boot-module\jeecg-module-rehealth `
  -Dtest=InternalIdentityAndDeviceAuthorizationIT test
```

Adversarial selection used by the release gate:

```powershell
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd `
  -f backend\jeecg-boot\pom.xml `
  -pl jeecg-boot-module\jeecg-module-rehealth `
  -Dtest=InternalIdentityAndDeviceAuthorizationIT `
  -Dcases=revoked,cross_device,unbound,spoofed_header,auth_unavailable test
```
