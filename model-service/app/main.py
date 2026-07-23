from __future__ import annotations

import logging

from fastapi import FastAPI, HTTPException

from app.attribution import IndividualAttributor
from app.model_registry import ModelRegistry
from app.prescription_generator import ConservativePrescriptionGenerator
from app.risk_scorer import ModelUnavailableError, load_risk_scorer
from app.runtime_config import (
    RuntimeConfig,
    RuntimeStatus,
    load_runtime_config,
    runtime_status,
    validate_model_runtime,
    validate_runtime_config,
)
from app.schemas import (
    HealthResponse,
    IndividualAttributionRequest,
    IndividualAttributionResponse,
    InterventionGenerateRequest,
    InterventionGenerateResponse,
    RiskEvaluateRequest,
    RiskEvaluateResponse,
)


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("rehealth.model_service")

def create_app(runtime_config: RuntimeConfig | None = None) -> FastAPI:
    config = load_runtime_config() if runtime_config is None else runtime_config
    validate_runtime_config(config)
    scorer = load_risk_scorer()
    validate_model_runtime(config, scorer.scorer_mode)
    model_registry = ModelRegistry(scorer)
    prescription_generator = ConservativePrescriptionGenerator()
    service = FastAPI(
        title="ReHealth Model Service",
        version="0.1.0",
        description="CVD risk scoring and conservative intervention generation.",
    )

    @service.get("/health", response_model=HealthResponse, response_model_exclude_none=True)
    def health() -> HealthResponse:
        return HealthResponse(
            status="ok",
            service="model-service",
            model_version=scorer.model_version,
            model_registry_version=model_registry.registry_version,
            feature_schema_version=model_registry.feature_schema_version,
            scorer_mode=scorer.scorer_mode,
            model_available=scorer.scorer_mode == "real_available",
            model_unavailable_reason=scorer.model_unavailable_reason,
            expected_model_artifacts=scorer.expected_model_artifacts,
            supported_model_artifact_aliases=scorer.supported_model_artifact_aliases,
            expected_feature_order_artifacts=scorer.expected_feature_order_artifacts,
            loaded_artifact_name=scorer.loaded_artifact_name,
        )

    @service.get("/runtime", response_model=RuntimeStatus)
    def runtime() -> RuntimeStatus:
        return runtime_status(config)

    @service.post("/v1/cvd/risk/evaluate", response_model=RiskEvaluateResponse, response_model_exclude_none=True)
    def evaluate_risk(request: RiskEvaluateRequest) -> RiskEvaluateResponse:
        logger.info("risk evaluation requested")
        try:
            result = scorer.evaluate(request.feature_vector)
        except ModelUnavailableError as exc:
            raise HTTPException(status_code=503, detail={"code": "model_unavailable", "message": str(exc)}) from exc
        result.request_id = request.request_id
        if result.model_trace is not None:
            result.model_trace.request_id = request.request_id
        return result

    @service.post("/v1/cvd/intervention/generate", response_model=InterventionGenerateResponse)
    def generate_intervention(request: InterventionGenerateRequest) -> InterventionGenerateResponse:
        logger.info("intervention generation requested")
        return prescription_generator.generate(request)

    if config.mock_attribution_enabled:
        attributor = IndividualAttributor()

        @service.post("/v1/cvd/attribution/individual", response_model=IndividualAttributionResponse)
        def individual_attribution(request: IndividualAttributionRequest) -> IndividualAttributionResponse:
            logger.info("demo attribution requested")
            return attributor.evaluate(request)

    return service


app = create_app()
