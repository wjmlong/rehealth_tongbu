# ReHealth platform contracts

This directory is the versioned compatibility boundary for ReHealth services. `openapi/` freezes the Android-facing Jeecg API, `schemas/events/` freezes privacy-safe Kafka envelopes, `adrs/` records ownership and safety decisions, and `fixtures/` supplies executable examples. `fixtures/valid` and `fixtures/forbidden` contain Kafka event fixtures consumed by the validator; `fixtures/telemetry` contains mobile telemetry request examples and is validated by service tests instead.

Run from the repository root with Python 3.11 or newer. The no-argument command runs the complete static suite and fails if no checks execute.

```powershell
python backend/contracts/scripts/validate_contracts.py
python backend/contracts/scripts/validate_contracts.py --all --fixtures backend/contracts/fixtures/valid --report contracts.json
python backend/contracts/scripts/validate_contracts.py --fixtures backend/contracts/fixtures/forbidden --expect-rejected token,raw_signal,client_owner
```

Public mobile routes are compatibility contracts. A service cutover may change the internal Gateway target only after migration reconciliation; it must not change the Android path or response fields.
