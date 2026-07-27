from __future__ import annotations

import logging
from typing import Annotated

from fastapi import Depends, FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from api.production_auth import PiasApiError, authenticate, require_request_headers
from api.production_config import PiasSettings, load_settings
from api.production_schemas import (
    ErrorDetail,
    ErrorEnvelope,
    HealthStatus,
    IndividualAttributionRequest,
    SuccessEnvelope,
)
from api.production_service import IndividualAttributionService


RequestHeaders = Annotated[
    tuple[str | None, str | None, str | None],
    Depends(require_request_headers),
]
LOGGER = logging.getLogger("rehealth.pias")


def create_app(settings: PiasSettings | None = None) -> FastAPI:
    config = load_settings() if settings is None else settings
    attribution = IndividualAttributionService(config.engine_version, config.idempotency_capacity)
    service = FastAPI(
        title="ReHealth PIAS Service",
        version=config.engine_version,
        docs_url=None,
        redoc_url=None,
        description="Internal individual attribution service.",
    )

    @service.exception_handler(PiasApiError)
    async def pias_error_handler(_request: Request, error: PiasApiError) -> JSONResponse:
        body = ErrorEnvelope(
            code=error.status_code,
            request_id=error.request_id,
            error=ErrorDetail(code=error.code, message=error.message, retryable=error.retryable),
        )
        return JSONResponse(status_code=error.status_code, content=body.model_dump())

    @service.exception_handler(RequestValidationError)
    async def validation_error_handler(request: Request, _error: RequestValidationError) -> JSONResponse:
        body = ErrorEnvelope(
            code=422,
            request_id=request.headers.get("X-Request-ID"),
            error=ErrorDetail(
                code="PIAS_INVALID_REQUEST",
                message="attribution request validation failed",
                retryable=False,
            ),
        )
        return JSONResponse(status_code=422, content=body.model_dump())

    @service.exception_handler(Exception)
    async def unexpected_error_handler(request: Request, error: Exception) -> JSONResponse:
        LOGGER.exception("PIAS attribution engine failed", exc_info=error)
        body = ErrorEnvelope(
            code=500,
            request_id=request.headers.get("X-Request-ID"),
            error=ErrorDetail(
                code="PIAS_INTERNAL_ERROR",
                message="attribution engine failed",
                retryable=True,
            ),
        )
        return JSONResponse(status_code=500, content=body.model_dump())

    @service.get("/health", response_model=HealthStatus)
    def health() -> HealthStatus:
        return HealthStatus(status="ok", service="pias", engine_version=config.engine_version)

    @service.get("/health/readiness", response_model=HealthStatus)
    def ready() -> HealthStatus:
        return HealthStatus(status="ready", service="pias", engine_version=config.engine_version)

    @service.post(
        "/api/pias/v2/attribute/individual",
        response_model=SuccessEnvelope,
        response_model_by_alias=True,
    )
    def attribute_individual(
        request: IndividualAttributionRequest,
        headers: RequestHeaders,
    ) -> SuccessEnvelope:
        request_id, idempotency_key = authenticate(config.internal_token, headers)
        return attribution.attribute(request, request_id, idempotency_key)

    return service


app = create_app()
