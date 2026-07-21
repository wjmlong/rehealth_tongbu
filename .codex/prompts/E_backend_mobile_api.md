# E Backend Mobile API Prompt

Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.

Workstream: E_backend_mobile_api.

Goal:
Add minimal ReHealth mobile APIs to JeecgBoot backend.

Tasks:
1. Inspect backend module structure and auth conventions.
2. Add isolated rehealth mobile controller/service/entity/mapper package.
3. Implement auth/login, devices/bind, measurements/batch, risk/latest, interventions/today, feedback.
4. Add model-service client abstraction, but do not implement CatBoost/SHAP in Java.
5. Document API contract.
6. Run relevant Maven tests/build if possible.
