# F_model_service Status

## Implementation Notes

- Created standalone `model-service/` FastAPI service.
- Added typed schemas for the accepted Android C1 CVD feature contract, including `featureQuality` quality metadata.
- Added `GET /health`.
- Added `POST /v1/cvd/risk/evaluate`.
- Added `POST /v1/cvd/intervention/generate`.
- Added `POST /v1/cvd/attribution/individual`.
- Added deterministic MVP baseline scorer `cvd-baseline-rules-v1`.
- Added conservative intervention generation with an explicit medical disclaimer.
- Added individual attribution trend summary endpoint.
- Added README, requirements, Dockerfile, and tests.

## Validation Logs

- Initial import check showed bundled Python did not have FastAPI installed.
- Ran `python -m pip install -r requirements.txt` from `model-service/`.
- Ran `python -m pytest` from `model-service/`: 8 passed.
- Ran `python -m compileall app`: passed.
- Removed generated `.pytest_cache` and `__pycache__` directories.
- Final rerun `python -m pytest`: 8 passed.

## Self-Review Notes

- Searched `model-service/` for TODO/FIXME, mock-only behavior, medical safety wording, endpoint path mismatch, and C1 contract names.
- No TODO/FIXME markers remain.
- Service does not log request bodies or raw health payloads.
- Endpoint paths match the MVP contract:
  - `GET /health`
  - `POST /v1/cvd/risk/evaluate`
  - `POST /v1/cvd/intervention/generate`
  - `POST /v1/cvd/attribution/individual`
- Risk responses include `model_version`.
- Risk API accepts both flat Android feature-vector payloads and wrapped `{ "featureVector": ... }` payloads.
- Risk API accepts both Android camelCase field names and backend/model snake_case field names.
- Intervention output includes conservative safety wording and does not claim diagnosis.

## Unresolved Risks

- No CatBoost artifact is present yet; current scorer is a deterministic MVP baseline, not a trained clinical model.
- No persistence is included in F1; backend remains responsible for storing evaluations and feedback.
- No auth is included in F1; backend should protect access before exposing this service externally.
- Feature thresholds in the baseline scorer require clinical/product review before pilot use.

## Exact Next Recommended Task

- Start E1 next so JeecgBoot can call `model-service` through a client abstraction and persist mobile API records.

## Final Git Status

- No root Git repository exists at `D:\rehealthAI`.
- `model-service/` is newly created outside an existing Git repository, so no commit was possible without creating a new repo boundary.
- `Android-apk`: clean on `work/C_android_feature_extractor`.
- `backend`: unchanged; still has pre-existing untracked `jeecg-boot/jeecg-boot-module/jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth/`.
- `rehealth-android`: clean on `main`.

