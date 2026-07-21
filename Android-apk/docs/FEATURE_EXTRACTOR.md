# CVD Feature Extractor

This document defines the Android-local CVD feature extraction contract for the MVP.

## Contract

`HealthFeatureExtractor` produces a `CvdFeatureVector` with these 16 model fields:

- `age`
- `gender`
- `bmi`
- `sbp`
- `dbp`
- `fasting_glucose`
- `total_cholesterol`
- `ldl`
- `hdl`
- `triglycerides`
- `exercise_days`
- `smoking`
- `drinking`
- `diabetes_history`
- `hypertension_history`
- `family_history`

Every field has a `FeatureQuality` entry keyed by the snake_case field name. The quality entry records status, source, observation time when known, and a short reason.

## Sources

- Baseline profile/interview data: `age`, `gender`, `bmi`, `smoking`, `drinking`, `diabetes_history`, `hypertension_history`, `family_history`.
- Ring measurements: `sbp`, `dbp`, and step-derived `exercise_days`.
- Ring activities: activity-derived `exercise_days`.
- Clinical lab input: `fasting_glucose`, `total_cholesterol`, `ldl`, `hdl`, `triglycerides`.

`HealthMemorySnapshot.fromPatientProfile` maps the current patient MVP profile DTO into the extractor input model. The local interview free-text baseline can be carried in `interviewBaselineText`, but C1 does not parse unstructured medical facts from free text.

## Missing Data

The extractor does not invent missing medical values. Missing lab values remain `null` and receive `FeatureQualityStatus.MISSING`.

Implausible values are rejected as `null` with `FeatureQualityStatus.LOW_CONFIDENCE`. Current guards include:

- Adult age range: 18 to 120.
- BMI range: 10 to 80.
- Blood pressure: systolic 70 to 250 mmHg, diastolic 40 to 150 mmHg, systolic greater than diastolic.
- Step rows: 0 to 100,000 steps.
- Activity rows: non-negative distance/calories, 0 to 100,000 steps, 0 to 1,440 minutes.
- Lab rows: finite positive values up to a broad 1,000 upper bound. Units must already match the backend/model contract.

## Ring Data Rules

- Blood pressure uses the most recent plausible `ring_measurements` row where `metric_type == BLOOD_PRESSURE`.
- Duplicate blood pressure rows with the same timestamp and values are ignored for selection.
- `exercise_days` counts distinct UTC dates in the last 7 days with either at least 7,000 steps or at least 20 activity minutes.
- Sleep sessions are summarized on `HealthMemorySnapshot.sleepSummary` for local memory/context. Sleep is not included in the CVD 16-feature vector.

## Current Scope

C1 adds the pure extractor and unit tests. It does not wire feature extraction into upload, risk scoring, BLE collection, or Compose UI.
