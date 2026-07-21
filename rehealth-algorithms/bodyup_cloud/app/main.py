import logging
import time
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles

from bodyup_cloud.app.logging_config import setup_logging
from bodyup_cloud.config.settings import settings
from bodyup_cloud.db.database import init_db, async_session_maker
from bodyup_cloud.app.dependencies import init_model_registry, init_llm_provider, init_signer

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    setup_logging("DEBUG" if settings.environment == "development" else "INFO")
    logger.info("Starting BodyUP Cloud (env=%s)", settings.environment)

    await init_db()
    logger.info("Database initialized")

    await _seed_admin()

    _auto_generate_keys()

    init_model_registry()
    init_llm_provider()
    init_signer()

    logger.info("Startup complete")
    yield


async def _seed_admin():
    from bodyup_cloud.db.repository import UserRepository
    from bodyup_cloud.db.models import User
    from bodyup_cloud.app.auth import hash_password

    async with async_session_maker() as session:
        repo = UserRepository(session)
        count = await repo.count_by_role("admin")
        if count > 0:
            return
        if not settings.admin_email or not settings.admin_password:
            logger.warning("No admin users exist and ADMIN_EMAIL/ADMIN_PASSWORD not set")
            return
        user = User(
            email=settings.admin_email,
            hashed_password=hash_password(settings.admin_password),
            full_name="System Admin",
            role="admin",
        )
        session.add(user)
        await session.commit()
        logger.info("Auto-seeded initial admin: %s", settings.admin_email)


def _auto_generate_keys():
    key_path = settings.project_root / settings.ed25519_private_key_path
    if key_path.exists():
        return
    logger.warning("Ed25519 key not found — generating new keypair")
    from bodyup_cloud.engine.report_signer import generate_keypair
    private_pem, public_pem = generate_keypair()
    key_path.parent.mkdir(parents=True, exist_ok=True)
    key_path.write_bytes(private_pem)
    (key_path.parent / "public.pem").write_bytes(public_pem)
    logger.info("Generated Ed25519 keypair at %s", key_path.parent)


docs_url = "/docs" if settings.environment != "production" else None
redoc_url = "/redoc" if settings.environment != "production" else None

app = FastAPI(
    title="BodyUP Cloud Actuary",
    description="睿禾健康 — 端云协同心血管风险干预系统",
    version="0.1.0",
    lifespan=lifespan,
    docs_url=docs_url,
    redoc_url=redoc_url,
)


# --- CORS ---
origins = [o.strip() for o in settings.cors_origins.split(",") if o.strip()]
allow_all = "*" in origins
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"] if allow_all else origins,
    allow_credentials=not allow_all,
    allow_methods=["*"],
    allow_headers=["*"],
)


# --- Request logging ---
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    duration = time.time() - start
    if not request.url.path.startswith("/static"):
        logger.info("%s %s -> %d (%.3fs)", request.method, request.url.path, response.status_code, duration)
    return response


# --- Global exception handler ---
@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.error("Unhandled: %s %s — %s", request.method, request.url.path, exc, exc_info=True)
    return JSONResponse(status_code=500, content={"detail": "Internal server error"})


# --- Static files ---
static_dir = Path(__file__).resolve().parent.parent / "static"
if static_dir.exists():
    app.mount("/static", StaticFiles(directory=str(static_dir)), name="static")


# --- Routers ---
from bodyup_cloud.app.routers import (
    auth_router, inference, attribution, verification,
    doctor, patient, admin,
)

app.include_router(auth_router.router)
app.include_router(inference.router)
app.include_router(attribution.router)
app.include_router(verification.router)
app.include_router(doctor.router)
app.include_router(patient.router)
app.include_router(admin.router)


@app.get("/health")
async def health_check():
    return {"status": "ok", "service": "bodyup-cloud-actuary"}
