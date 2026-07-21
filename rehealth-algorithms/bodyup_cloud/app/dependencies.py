import logging
from functools import lru_cache
from pathlib import Path
from typing import Optional

from bodyup_cloud.config.settings import settings
from bodyup_cloud.engine.risk_scorer import ModelRegistry
from bodyup_cloud.engine.llm_provider import create_provider, LLMProvider
from bodyup_cloud.engine.prescription_generator import PrescriptionGenerator
from bodyup_cloud.engine.report_signer import ReportSigner

logger = logging.getLogger(__name__)

_model_registry: Optional[ModelRegistry] = None
_llm_provider: Optional[LLMProvider] = None
_prescription_gen: Optional[PrescriptionGenerator] = None
_signer: Optional[ReportSigner] = None


def init_model_registry() -> ModelRegistry:
    global _model_registry
    _model_registry = ModelRegistry()
    model_path = str(settings.project_root / settings.model_path)
    if Path(model_path).exists():
        _model_registry.register("default", model_path)
        logger.info("Loaded risk model from %s", model_path)
    else:
        logger.warning("Risk model not found at %s — /inference/score will be unavailable", model_path)
    return _model_registry


def init_llm_provider() -> Optional[LLMProvider]:
    global _llm_provider, _prescription_gen
    api_keys = {
        "claude": settings.anthropic_api_key,
        "openai": settings.openai_api_key,
        "openrouter": settings.openrouter_api_key,
        "mimo": settings.mimo_api_key,
    }
    api_key = api_keys.get(settings.llm_provider, "")
    if api_key:
        extra = {}
        if settings.llm_provider == "mimo":
            extra["base_url"] = settings.mimo_base_url
        _llm_provider = create_provider(settings.llm_provider, api_key, settings.llm_model, **extra)
        _prescription_gen = PrescriptionGenerator(_llm_provider)
        logger.info("LLM provider initialized: %s (%s)", settings.llm_provider, settings.llm_model)
    else:
        logger.warning("No LLM API key for provider '%s' — prescription generation unavailable", settings.llm_provider)
    return _llm_provider


def init_signer() -> Optional[ReportSigner]:
    global _signer
    key_path = settings.project_root / settings.ed25519_private_key_path
    if key_path.exists():
        from cryptography.hazmat.primitives.serialization import load_pem_private_key
        with open(key_path, "rb") as f:
            private_key = load_pem_private_key(f.read(), password=None)
        _signer = ReportSigner(private_key)
        logger.info("Ed25519 signer initialized from %s", key_path)
    else:
        logger.warning("Ed25519 key not found at %s — report signing unavailable", key_path)
    return _signer


def get_model_registry() -> ModelRegistry:
    if _model_registry is None:
        init_model_registry()
    return _model_registry


def get_llm_provider() -> Optional[LLMProvider]:
    return _llm_provider


def get_prescription_generator() -> Optional[PrescriptionGenerator]:
    return _prescription_gen


def get_signer() -> Optional[ReportSigner]:
    return _signer
