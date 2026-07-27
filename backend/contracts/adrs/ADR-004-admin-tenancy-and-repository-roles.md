# ADR-004: Admin tenancy and repository roles

Status: Accepted, version 1, 2026-07-23.

Jeecg is the authority for users, tenants, institutions, roles, permissions, consent, clinician assignments, insurance workflows, and admin audit. All admin records and links carry tenant or organization scope derived from the authenticated principal. Platform admin is the only cross-tenant role; institution admins, clinicians, insurance operators, operations reviewers, and auditors are least-privileged. Hidden menus do not authorize APIs, and non-platform roles receive no wildcard ReHealth permission.

Repository responsibilities are non-overlapping: Android collects and persists locally; Device Service owns hardware ingestion and Timescale; Jeecg owns authenticated business orchestration and `software_db`; `model-service` owns model and agent APIs; `rehealth-algorithms` owns training/research plus the hardened PIAS application. No repository duplicates credentials, inference authority, or hardware telemetry ownership.
