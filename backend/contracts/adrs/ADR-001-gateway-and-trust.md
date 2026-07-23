# ADR-001: Gateway route ownership and service trust

Status: Accepted, version 1, 2026-07-23.

The public edge owns routing. `/jeecg-boot/rehealth/mobile/**` remains Android-compatible and Jeecg owns authenticated orchestration. During the Device Service migration, Gateway may route telemetry upload and recent-read operations to Device Service only after the signed cutover gate passes; every other mobile route remains with Jeecg. No internal service publishes a public port.

The client supplies `X-Access-Token` only to the public edge. Gateway propagates authenticated identity through a mutually authenticated internal trust channel. Device Service resolves the user, tenant, and bound device from that trusted identity; client body or query values never establish ownership. Internal identity headers arriving from the public client are discarded. Service credentials and tokens never enter Kafka events, evidence, or logs.

The frozen public shapes are in `openapi/rehealth-mobile-v1.openapi.json`. Route changes require a new contract version and an explicit Android migration.
