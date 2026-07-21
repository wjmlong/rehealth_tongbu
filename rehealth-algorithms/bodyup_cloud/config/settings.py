from pathlib import Path
from pydantic import model_validator
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    environment: str = "development"
    secret_key: str = "dev-secret-change-in-production"
    access_token_expire_minutes: int = 1440
    algorithm: str = "HS256"

    llm_provider: str = "claude"
    llm_model: str = "claude-sonnet-4-20250514"
    anthropic_api_key: str = ""
    openai_api_key: str = ""
    openrouter_api_key: str = ""

    mimo_api_key: str = ""
    mimo_base_url: str = "https://token-plan-cn.xiaomimimo.com/anthropic"
    mimo_model: str = "mimo-v2.5"

    model_path: str = "train/rehealth_v2_final.pkl"
    database_url: str = "sqlite+aiosqlite:///./data/bodyup.db"
    ed25519_private_key_path: str = "keys/private.pem"

    cors_origins: str = "http://localhost:3000,http://localhost:8000"

    admin_email: str = ""
    admin_password: str = ""

    project_root: Path = Path(__file__).resolve().parent.parent.parent

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}

    @model_validator(mode="after")
    def _check_production_safety(self):
        if self.environment == "production":
            if self.secret_key.startswith("dev-") or len(self.secret_key) < 32:
                raise ValueError(
                    "SECRET_KEY must be ≥32 chars and not start with 'dev-' in production"
                )
        return self


settings = Settings()
