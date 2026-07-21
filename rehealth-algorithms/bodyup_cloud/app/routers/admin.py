from pathlib import Path

from fastapi import APIRouter, Depends, Request, HTTPException
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.ext.asyncio import AsyncSession

from bodyup_cloud.app.auth import (
    require_role_cookie, verify_password, create_access_token,
    COOKIE_NAME,
)
from bodyup_cloud.config.settings import settings
from bodyup_cloud.db.database import get_db
from bodyup_cloud.db.models import User
from bodyup_cloud.db.repository import (
    UserRepository, HealthRepository, ConfigRepository,
)
from bodyup_cloud.db.schemas import LLMConfigUpdate, SystemStats

router = APIRouter(prefix="/admin", tags=["admin"])

_templates_dir = Path(__file__).resolve().parent.parent.parent / "templates"
templates = Jinja2Templates(directory=str(_templates_dir))


# ─── Login / Logout ───

@router.get("/login", response_class=HTMLResponse)
async def admin_login_page(request: Request):
    return templates.TemplateResponse("login.html", {"request": request, "error": None})


@router.post("/login")
async def admin_login_submit(request: Request, db: AsyncSession = Depends(get_db)):
    form = await request.form()
    email = form.get("email", "")
    password = form.get("password", "")

    repo = UserRepository(db)
    user = await repo.get_by_email(str(email))

    if not user or not verify_password(str(password), user.hashed_password):
        return templates.TemplateResponse(
            "login.html", {"request": request, "error": "邮箱或密码错误"}, status_code=401,
        )
    if user.role != "admin":
        return templates.TemplateResponse(
            "login.html", {"request": request, "error": "需要管理员权限"}, status_code=403,
        )

    token = create_access_token(data={"sub": str(user.id)})
    response = RedirectResponse(url="/admin/", status_code=303)
    response.set_cookie(
        key=COOKIE_NAME,
        value=token,
        httponly=True,
        samesite="lax",
        max_age=settings.access_token_expire_minutes * 60,
        secure=(settings.environment == "production"),
    )
    return response


@router.get("/logout")
async def admin_logout():
    response = RedirectResponse(url="/admin/login", status_code=303)
    response.delete_cookie(COOKIE_NAME)
    return response


# ─── API endpoints (cookie auth) ───

@router.get("/api/stats", response_model=SystemStats)
async def system_stats(
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    user_repo = UserRepository(db)
    health_repo = HealthRepository(db)
    n_patients = await user_repo.count_by_role("patient")
    n_doctors = await user_repo.count_by_role("doctor")
    n_admins = await user_repo.count_by_role("admin")
    n_scores = await health_repo.count_scores_today()
    return SystemStats(
        total_users=n_patients + n_doctors + n_admins,
        total_patients=n_patients,
        total_doctors=n_doctors,
        total_scores_today=n_scores,
        active_devices=0,
    )


@router.get("/api/users")
async def list_users(
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    repo = UserRepository(db)
    users = await repo.list_all()
    return [
        {"id": u.id, "email": u.email, "full_name": u.full_name,
         "role": u.role, "is_active": u.is_active, "created_at": str(u.created_at)}
        for u in users
    ]


@router.get("/api/users/{user_id}")
async def get_user(
    user_id: int,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    repo = UserRepository(db)
    user = await repo.get_by_id(user_id)
    if not user:
        raise HTTPException(404, "User not found")
    health_repo = HealthRepository(db)
    history = await health_repo.get_risk_history(patient_id=user_id, limit=90)
    return {
        "user": {"id": user.id, "email": user.email, "full_name": user.full_name,
                 "role": user.role, "is_active": user.is_active},
        "risk_history": [
            {"date": r.date, "risk_score": r.risk_score, "intervention": r.intervention}
            for r in history
        ],
    }


@router.get("/api/llm/config")
async def get_llm_config(
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    repo = ConfigRepository(db)
    config = await repo.get_active_llm_config()
    if not config:
        return {"provider": "not_configured", "model": "", "is_active": False}
    return {"id": config.id, "provider": config.provider,
            "model": config.model, "is_active": config.is_active}


@router.put("/api/llm/config")
async def update_llm_config(
    update: LLMConfigUpdate,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    repo = ConfigRepository(db)
    config = await repo.update_llm_config(update)
    await db.commit()
    return {"id": config.id, "provider": config.provider,
            "model": config.model, "is_active": config.is_active}


@router.get("/api/models")
async def list_models(
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    repo = ConfigRepository(db)
    models = await repo.get_model_configs()
    return [
        {"id": m.id, "name": m.name, "version": m.version,
         "auc": m.auc, "data_source": m.data_source, "is_default": m.is_default}
        for m in models
    ]


# ─── Jinja2 browser pages ───

@router.get("/", response_class=HTMLResponse)
async def admin_dashboard(
    request: Request,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    user_repo = UserRepository(db)
    health_repo = HealthRepository(db)
    stats = {
        "patients": await user_repo.count_by_role("patient"),
        "doctors": await user_repo.count_by_role("doctor"),
        "scores_today": await health_repo.count_scores_today(),
    }
    return templates.TemplateResponse("dashboard.html", {"request": request, "stats": stats})


@router.get("/users", response_class=HTMLResponse)
async def admin_users_page(
    request: Request,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    repo = UserRepository(db)
    users = await repo.list_all()
    return templates.TemplateResponse("users.html", {"request": request, "users": users})


@router.get("/llm", response_class=HTMLResponse)
async def admin_llm_page(
    request: Request,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    repo = ConfigRepository(db)
    config = await repo.get_active_llm_config()
    return templates.TemplateResponse("llm_config.html", {"request": request, "config": config})


@router.get("/models", response_class=HTMLResponse)
async def admin_models_page(
    request: Request,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role_cookie("admin")),
):
    repo = ConfigRepository(db)
    models = await repo.get_model_configs()
    return templates.TemplateResponse("models.html", {"request": request, "models": models})
