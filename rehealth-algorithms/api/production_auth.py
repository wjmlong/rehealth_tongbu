from __future__ import annotations

import hmac
from dataclasses import dataclass
from typing import Annotated

from fastapi import Header


@dataclass(frozen=True, slots=True)
class PiasApiError(Exception):
    status_code: int
    code: str
    message: str
    request_id: str | None
    retryable: bool = False

    def __str__(self) -> str:
        return f"{self.code}: {self.message}"


def require_request_headers(
    authorization: Annotated[str | None, Header()] = None,
    request_id: Annotated[str | None, Header(alias="X-Request-ID")] = None,
    idempotency_key: Annotated[str | None, Header(alias="Idempotency-Key")] = None,
) -> tuple[str | None, str | None, str | None]:
    return authorization, request_id, idempotency_key


def authenticate(
    expected_token: str,
    headers: tuple[str | None, str | None, str | None],
) -> tuple[str, str]:
    authorization, request_id, idempotency_key = headers
    if authorization is None or not authorization.startswith("Bearer "):
        raise PiasApiError(401, "PIAS_AUTH_REQUIRED", "internal service authentication is required", request_id)
    provided_token = authorization.removeprefix("Bearer ")
    if not hmac.compare_digest(provided_token, expected_token):
        raise PiasApiError(401, "PIAS_AUTH_INVALID", "internal service authentication failed", request_id)
    if request_id is None or not request_id.strip():
        raise PiasApiError(400, "PIAS_REQUEST_ID_REQUIRED", "X-Request-ID is required", None)
    if idempotency_key is None or not idempotency_key.strip():
        raise PiasApiError(400, "PIAS_IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key is required", request_id)
    return request_id.strip(), idempotency_key.strip()
