# C Android Feature Extractor Prompt

Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.

Workstream: C_android_feature_extractor.

Goal:
Generate CVD 16-feature vector from local Room and profile/interview data.

Tasks:
1. Inspect Room entities/DAO and profile/interview persistence.
2. Create features module.
3. Implement CvdFeatureVector, FeatureQuality, HealthFeatureExtractor, HealthMemorySnapshot.
4. Use 7/14/30 day windows where appropriate.
5. Add tests for missing, stale, repeated, abnormal data.
6. Do not implement networking.
