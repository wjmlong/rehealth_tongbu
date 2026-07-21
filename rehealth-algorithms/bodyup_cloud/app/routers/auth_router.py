"""
Authentication router -- register, login, and current-user endpoints.
"""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.ext.asyncio import AsyncSession

from bodyup_cloud.app.auth import (
    create_access_token,
    get_current_user,
    hash_password,
    verify_password,
)
from bodyup_cloud.db.database import get_db
from bodyup_cloud.db.models import User
from bodyup_cloud.db.repository import UserRepository
from bodyup_cloud.db.schemas import Token, UserCreate, UserOut

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register", response_model=UserOut, status_code=status.HTTP_201_CREATED)
async def register(
    user_in: UserCreate,
    db: Annotated[AsyncSession, Depends(get_db)],
    current_user: User | None = None,
):
    """Register a new user.

    * Anyone may self-register as a **patient**.
    * Only an authenticated **admin** may create **doctor** or **admin** accounts.
    """
    # Role guard: non-patient roles require an admin caller
    if user_in.role != "patient":
        if current_user is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Authentication required to create non-patient accounts.",
            )
        if current_user.role != "admin":
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Only admins can create doctor or admin accounts.",
            )

    repo = UserRepository(db)

    # Uniqueness check
    existing = await repo.get_by_email(user_in.email)
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="A user with this email already exists.",
        )

    hashed_pw = hash_password(user_in.password)
    user = await repo.create(user_in, hashed_pw)
    return user


@router.post("/login", response_model=Token)
async def login(
    form_data: Annotated[OAuth2PasswordRequestForm, Depends()],
    db: Annotated[AsyncSession, Depends(get_db)],
):
    """Authenticate with email + password and receive a JWT."""
    repo = UserRepository(db)
    user = await repo.get_by_email(form_data.username)

    if user is None or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="This account has been deactivated.",
        )

    access_token = create_access_token(data={"sub": str(user.id)})
    return Token(access_token=access_token)


@router.get("/me", response_model=UserOut)
async def read_current_user(
    current_user: Annotated[User, Depends(get_current_user)],
):
    """Return the profile of the currently authenticated user."""
    return current_user
