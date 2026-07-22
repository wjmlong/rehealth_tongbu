# Static contract checks

- `git diff --check`: PASS (no output; command exit 0).
- `Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt`: all `rehealth/mobile/*` endpoints use relative Retrofit paths (`rehealth/mobile/health`, `config`, `features/evaluate`, `risk/latest`, `interventions/today`, `interventions/{id}/feedback`, `measurements/batch`, `attribution/events`). The three `/jeecg-boot/sys/*` absolute paths are system auth/registration, not mobile paths.
- `backend/.../dto/CvdFeatureVectorDto.java`: Jackson `@JsonProperty` + `@JsonAlias` present for snake_case/camelCase fields (`fasting_glucose`, `total_cholesterol`, `exercise_days`, `diabetes_history`, `hypertension_history`, `family_history`).
- `backend/.../dto/AttributionResponseDto.java`: Jackson `@JsonProperty` + `@JsonAlias` present across attribution response fields; Fastjson `@JSONField` also preserves wire names.
- Repository SHA at execution: `bda9dde3cab64b4621f61513c2c7988f9d7b4098` (Android and backend worktrees resolve same SHA).



- Evidence note: git diff --check produced no output and exited 0; no standalone empty log was retained.
