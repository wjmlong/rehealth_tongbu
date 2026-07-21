# F Model Service Prompt

Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.

Workstream: F_model_service.

Goal:
Create Python FastAPI service for CVD risk scoring.

Tasks:
1. Create model-service layout if absent.
2. Implement typed schemas.
3. Add /health and /v1/cvd/risk/evaluate.
4. Include risk_score, risk_level, feature_contributions, model_version.
5. Add tests and Dockerfile.
6. Add README.
7. Do not touch Android or backend except API docs if needed.
