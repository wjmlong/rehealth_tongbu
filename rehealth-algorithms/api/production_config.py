from __future__ import annotations

import os
from collections.abc import Mapping
from pathlib import Path

from pydantic import BaseModel, ConfigDict, Field


class PiasSettings(BaseModel):
    model_config = ConfigDict(frozen=True)

    internal_token: str = Field(min_length=16)
    engine_version: str = "pias-individual-v2"
    idempotency_capacity: int = Field(default=1_000, ge=1, le=100_000)


def load_settings(environ: Mapping[str, str] | None = None) -> PiasSettings:
    source = os.environ if environ is None else environ
    token = source.get("REHEALTH_PIAS_INTERNAL_TOKEN", "").strip()
    credential_file = source.get("REHEALTH_INTERNAL_SERVICE_CREDENTIAL_FILE", "").strip()
    if not token and credential_file:
        token = Path(credential_file).read_text(encoding="utf-8").strip()
    return PiasSettings(
        internal_token=token,
        engine_version=source.get("REHEALTH_PIAS_ENGINE_VERSION", "pias-individual-v2"),
        idempotency_capacity=int(source.get("REHEALTH_PIAS_IDEMPOTENCY_CAPACITY", "1000")),
    )
