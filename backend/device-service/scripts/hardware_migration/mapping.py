from __future__ import annotations

import hashlib
import json
from datetime import UTC, datetime
from decimal import Decimal
from typing import Mapping
from uuid import NAMESPACE_URL, UUID, uuid5

from .models import MappedRow, MappingError, Row, TableSpec, Value


def mysql_datetime_to_utc(value: Value) -> datetime | None:
    if value is None:
        return None
    if not isinstance(value, datetime):
        raise MappingError(f"expected MySQL datetime, observed {type(value).__name__}")
    return value.replace(tzinfo=UTC) if value.tzinfo is None else value.astimezone(UTC)


def stable_uuid(table: str, source_id: Value) -> UUID:
    try:
        return UUID(str(source_id))
    except ValueError:
        return uuid5(NAMESPACE_URL, f"rehealth:mysql:{table}:{source_id}")


def legacy_source_record_id(table: str, source_id: Value, client_id: Value) -> str:
    if client_id is not None and str(client_id).strip():
        return str(client_id)
    return f"legacy:{table}:{source_id}"


def quality_summary(raw: Value) -> dict[str, Value]:
    if raw is None or raw == "":
        return {}
    decoded = json.loads(str(raw))
    if not isinstance(decoded, dict):
        raise MappingError("legacy quality_json must contain a JSON object")
    return {str(key): value for key, value in decoded.items()}


def map_upload_batch(row: Row, tenant_id: str) -> MappedRow:
    return {
        "id": stable_uuid("hardware_upload_batch", row["id"]),
        "receipt_id": stable_uuid("hardware_upload_batch:receipt", row["receipt_id"]),
        "tenant_id": tenant_id,
        "user_id": row["user_id"],
        "device_id": row["device_id"],
        "batch_id": row["batch_id"],
        "source": row["source"],
        "collected_from": mysql_datetime_to_utc(row["collected_from"]),
        "collected_to": mysql_datetime_to_utc(row["collected_to"]),
        "received_at": mysql_datetime_to_utc(row["received_at"]),
        "committed_at": mysql_datetime_to_utc(row["committed_at"]),
        "status": "PERSISTED",
        "record_count": row["record_count"],
        "measurement_count": row["measurement_count"],
        "sleep_session_count": row["sleep_session_count"],
        "activity_count": row["activity_count"],
        "signal_metadata_count": row["signal_chunk_count"],
        "quality_summary": quality_summary(row["quality_json"]),
    }


def _common(row: Row, tenant_id: str, table: str) -> MappedRow:
    return {
        "id": stable_uuid(table, row["id"]),
        "upload_batch_id": stable_uuid("hardware_upload_batch", row["upload_batch_id"]),
        "tenant_id": tenant_id,
        "user_id": row["user_id"],
        "device_id": row["device_id"],
    }


def map_measurement(row: Row, tenant_id: str) -> MappedRow:
    return _common(row, tenant_id, "hardware_measurement") | {
        "source_record_id": legacy_source_record_id(
            "hardware_measurement", row["id"], row["client_record_id"]
        ),
        "metric_type": row["metric_type"],
        "observed_at": mysql_datetime_to_utc(row["measured_at"]),
        "primary_value": row["primary_value"],
        "secondary_value": row["secondary_value"],
        "unit": row["unit"],
        "quality_code": row["quality_code"],
        "source": row["source"],
        "created_at": mysql_datetime_to_utc(row["created_at"]),
    }


def map_sleep(row: Row, tenant_id: str) -> MappedRow:
    return _common(row, tenant_id, "hardware_sleep_session") | {
        "source_record_id": legacy_source_record_id(
            "hardware_sleep_session", row["id"], row["client_record_id"]
        ),
        "started_at": mysql_datetime_to_utc(row["started_at"]),
        "ended_at": mysql_datetime_to_utc(row["ended_at"]),
        "deep_minutes": row["deep_minutes"],
        "light_minutes": row["light_minutes"],
        "awake_minutes": row["awake_minutes"],
        "rem_minutes": row["rem_minutes"],
        "interruption_minutes": row["interruption_minutes"],
        "source": row["source"],
        "created_at": mysql_datetime_to_utc(row["created_at"]),
    }


def map_activity(row: Row, tenant_id: str) -> MappedRow:
    return _common(row, tenant_id, "hardware_activity") | {
        "source_record_id": legacy_source_record_id(
            "hardware_activity", row["id"], row["client_record_id"]
        ),
        "started_at": mysql_datetime_to_utc(row["started_at"]),
        "ended_at": mysql_datetime_to_utc(row["ended_at"]),
        "activity_type": row["activity_type"],
        "steps": row["steps"],
        "distance_meters": row["distance_meters"],
        "calories_kcal": row["calories_kcal"],
        "duration_minutes": row["duration_minutes"],
        "average_heart_rate": row["average_heart_rate"],
        "source": row["source"],
        "created_at": mysql_datetime_to_utc(row["created_at"]),
    }


def map_signal(row: Row, tenant_id: str) -> MappedRow:
    return _common(row, tenant_id, "hardware_signal_chunk_metadata") | {
        "source_record_id": legacy_source_record_id(
            "hardware_signal_chunk_metadata", row["id"], None
        ),
        "signal_type": row["signal_type"],
        "started_at": mysql_datetime_to_utc(row["started_at"]),
        "ended_at": None,
        "sample_rate_hz": row["sample_rate_hz"],
        "sample_count": row["sample_count"],
        "quality_code": None,
        "created_at": mysql_datetime_to_utc(row["created_at"]),
    }


def map_quality(row: Row, tenant_id: str) -> MappedRow:
    message_hash = hashlib.sha256(str(row["message"]).encode("utf-8")).hexdigest()[:32]
    return _common(row, tenant_id, "hardware_data_quality_event") | {
        "source_record_id": legacy_source_record_id(
            "hardware_data_quality_event", row["id"], None
        ),
        "event_type": row["event_type"],
        "severity": str(row["severity"]).upper(),
        "detail_code": f"LEGACY_MESSAGE_SHA256:{message_hash}",
        "event_at": mysql_datetime_to_utc(row["occurred_at"]),
        "created_at": mysql_datetime_to_utc(row["created_at"]),
    }


TABLES = (
    TableSpec("hardware_upload_batch", "hardware_upload_batch", "received_at", "received_at",
              ("id", "receipt_id", "tenant_id", "user_id", "device_id", "batch_id", "source",
               "collected_from", "collected_to", "received_at", "committed_at", "status",
               "record_count", "measurement_count", "sleep_session_count", "activity_count",
               "signal_metadata_count", "quality_summary"), ("id",), map_upload_batch),
    TableSpec("hardware_measurement", "hardware_measurement", "measured_at", "observed_at",
              ("id", "upload_batch_id", "tenant_id", "user_id", "device_id", "source_record_id",
               "metric_type", "observed_at", "primary_value", "secondary_value", "unit",
               "quality_code", "source", "created_at"), ("id", "observed_at"), map_measurement),
    TableSpec("hardware_sleep_session", "hardware_sleep_session", "started_at", "started_at",
              ("id", "upload_batch_id", "tenant_id", "user_id", "device_id", "source_record_id",
               "started_at", "ended_at", "deep_minutes", "light_minutes", "awake_minutes",
               "rem_minutes", "interruption_minutes", "source", "created_at"),
              ("id", "started_at"), map_sleep),
    TableSpec("hardware_activity", "hardware_activity", "started_at", "started_at",
              ("id", "upload_batch_id", "tenant_id", "user_id", "device_id", "source_record_id",
               "started_at", "ended_at", "activity_type", "steps", "distance_meters",
               "calories_kcal", "duration_minutes", "average_heart_rate", "source", "created_at"),
              ("id", "started_at"), map_activity),
    TableSpec("hardware_signal_chunk_metadata", "hardware_signal_chunk_metadata", "started_at",
              "started_at", ("id", "upload_batch_id", "tenant_id", "user_id", "device_id",
               "source_record_id", "signal_type", "started_at", "ended_at", "sample_rate_hz",
               "sample_count", "quality_code", "created_at"), ("id",), map_signal),
    TableSpec("hardware_data_quality_event", "hardware_data_quality_event", "occurred_at",
              "event_at", ("id", "upload_batch_id", "tenant_id", "user_id", "device_id",
               "source_record_id", "event_type", "severity", "detail_code", "event_at",
               "created_at"), ("id", "event_at"), map_quality),
)
