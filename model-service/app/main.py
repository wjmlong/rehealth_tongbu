from __future__ import annotations

import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request, Response

from app.attribution import IndividualAttributor
from app.attribution_schemas import DemoAttributionRequest, DemoAttributionResponse
from app.model_registry import ModelRegistry
from app.model_execution import ModelCallFailure, ModelExecutionGuard
from app.observability import CONTENT_TYPE, ServiceMetrics, observe_request
from app.prescription_generator import ConservativePrescriptionGenerator
from app.risk_scorer import RiskScorer, load_risk_scorer
from app.runtime_config import (
    RuntimeConfig,
    RuntimeStatus,
    load_runtime_config,
    runtime_status,
    validate_runtime_config,
)
from app.schemas import (
    ActiveModelResponse,
    HealthResponse,
    InterventionGenerateRequest,
    InterventionGenerateResponse,
    RiskEvaluateRequest,
    RiskEvaluateResponse,
    ReadinessResponse,
)


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("rehealth.model_service")

def create_app(
    runtime_config: RuntimeConfig | None = None,
    scorer: RiskScorer | None = None,
) -> FastAPI:
    config = load_runtime_config() if runtime_config is None else runtime_config
    validate_runtime_config(config)
    active_scorer = load_risk_scorer() if scorer is None else scorer
    model_registry = ModelRegistry(active_scorer)
    execution = ModelExecutionGuard(
        active_scorer,
        config.model_evaluation_timeout_seconds,
        config.model_circuit_failure_threshold,
        config.model_circuit_reset_seconds,
    )
    metrics = ServiceMetrics()

    @asynccontextmanager
    async def lifespan(_: FastAPI) -> AsyncIterator[None]:
        yield
        execution.close()

    prescription_generator = ConservativePrescriptionGenerator()
    service = FastAPI(
        title="ReHealth Model Service",
        version="0.1.0",
        description="CVD risk scoring and conservative intervention generation.",
        lifespan=lifespan,
    )

    @service.middleware("http")
    async def request_observability(request: Request, call_next):
        return await observe_request(request, call_next, metrics)

    @service.get("/health", response_model=HealthResponse, response_model_exclude_none=True)
    def health() -> HealthResponse:
        return HealthResponse(
            status="ok",
            service="model-service",
            model_version=active_scorer.model_version,
            model_registry_version=model_registry.registry_version,
            feature_schema_version=model_registry.feature_schema_version,
            scorer_mode=active_scorer.scorer_mode,
            model_available=active_scorer.scorer_mode == "real_available",
            model_unavailable_reason=(
                None if active_scorer.scorer_mode == "real_available" else "reviewed model is unavailable"
            ),
            expected_model_artifacts=[
                "models/rehealth_cvd_catboost.pkl",
                "models/rehealth_v2_final.pkl",
            ],
            supported_model_artifact_aliases=[
                "models/rehealth_cvd_catboost.pkl",
                "models/rehealth_v2_final.pkl",
            ],
            expected_feature_order_artifacts=[
                "models/feature_cols.pkl",
                "models/feature_cols_v2.pkl",
                "models/cvd_features.json",
            ],
            loaded_artifact_name=model_registry.active_entry().artifact_name,
        )

    @service.get("/ready", response_model=ReadinessResponse)
    def readiness() -> Response:
        ready, code = model_registry.readiness(config)
        entry = model_registry.active_entry()
        body = ReadinessResponse(
            status="ready" if ready else "unavailable",
            code=code,
            model_version=entry.model_version,
            feature_schema_version=entry.feature_schema_version,
            scorer_mode=entry.scorer_mode,
            is_mock=entry.is_mock,
        )
        return Response(
            status_code=200 if ready else 503,
            content=body.model_dump_json(),
            media_type="application/json",
        )

    @service.get("/v1/models/active", response_model=ActiveModelResponse)
    def active_model() -> ActiveModelResponse:
        entry = model_registry.active_entry()
        ready, code = model_registry.readiness(config)
        return ActiveModelResponse(
            registry_version=model_registry.registry_version,
            feature_schema_version=entry.feature_schema_version,
            model_version=entry.model_version,
            scorer_mode=entry.scorer_mode,
            is_mock=entry.is_mock,
            artifact_name=entry.artifact_name,
            ready=ready,
            readiness_code=code,
        )

    @service.get("/metrics")
    def prometheus_metrics() -> Response:
        return Response(content=metrics.render(), media_type=CONTENT_TYPE)

    @service.get("/runtime", response_model=RuntimeStatus)
    def runtime() -> RuntimeStatus:
        return runtime_status(config)

    @service.post("/v1/cvd/risk/evaluate", response_model=RiskEvaluateResponse, response_model_exclude_none=True)
    def evaluate_risk(request: RiskEvaluateRequest, http_request: Request) -> RiskEvaluateResponse:
        request_id = request.request_id or http_request.state.correlation_id
        http_request.state.correlation_id = request_id
        try:
            result = execution.evaluate(request.feature_vector)
        except ModelCallFailure as exc:
            http_request.state.model_outcome = {
                "model_timeout": "timeout",
                "model_circuit_open": "circuit_open",
            }.get(exc.code, "unavailable")
            raise HTTPException(
                status_code=503,
                detail={"code": exc.code, "message": exc.message},
            ) from exc
        result.request_id = request_id
        if result.model_trace is not None:
            result.model_trace.request_id = request_id
        return result

    @service.post("/v1/cvd/intervention/generate", response_model=InterventionGenerateResponse)
    def generate_intervention(request: InterventionGenerateRequest) -> InterventionGenerateResponse:
        logger.info("intervention generation requested")
        return prescription_generator.generate(request)

    if config.mock_attribution_enabled:
        attributor = IndividualAttributor()

        @service.post("/v1/cvd/attribution/individual", response_model=DemoAttributionResponse)
        def individual_attribution(request: DemoAttributionRequest) -> DemoAttributionResponse:
            logger.info("demo attribution requested")
            return attributor.evaluate(request)

    return service


app = create_app()
