# ReHealth telemetry contract

`rehealth-telemetry-contract` is the dependency-free business contract shared by Jeecg compatibility tests and the Device Service. It contains only versioned request/response DTOs and boundary validation; persistence, authentication, Kafka and Spring wiring remain in their owning services.

The public Android shape remains compatible with `d2-v1`. New producers may send top-level `schemaVersion: telemetry-v1`. A client-supplied `userId` is rejected: Gateway/service authentication establishes ownership after client validation.

Run:

```powershell
mvn -f backend/contracts/telemetry/pom.xml test -Dfixtures=src/test/resources/legacy-valid
mvn -f backend/contracts/telemetry/pom.xml test -Dtest=TelemetryContractRejectionTest
```
