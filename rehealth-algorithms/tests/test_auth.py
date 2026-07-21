"""Tests for JWT auth utilities (no database required)."""

import time
from datetime import timedelta
from unittest.mock import patch

from bodyup_cloud.app.auth import (
    hash_password,
    verify_password,
    create_access_token,
)


class TestPasswordHashing:
    def test_hash_and_verify(self):
        plain = "correct-horse-battery-staple"
        hashed = hash_password(plain)
        assert hashed != plain
        assert verify_password(plain, hashed)

    def test_wrong_password(self):
        hashed = hash_password("secret123")
        assert not verify_password("wrong", hashed)

    def test_different_hashes_for_same_input(self):
        h1 = hash_password("same")
        h2 = hash_password("same")
        assert h1 != h2  # bcrypt uses random salt


class TestJWT:
    @patch("bodyup_cloud.app.auth.settings")
    def test_create_token(self, mock_settings):
        mock_settings.secret_key = "test-secret-key-for-jwt"
        mock_settings.algorithm = "HS256"
        mock_settings.access_token_expire_minutes = 30

        token = create_access_token({"sub": 42})
        assert isinstance(token, str)
        assert len(token) > 20

    @patch("bodyup_cloud.app.auth.settings")
    def test_token_with_custom_expiry(self, mock_settings):
        mock_settings.secret_key = "test-secret-key-for-jwt"
        mock_settings.algorithm = "HS256"
        mock_settings.access_token_expire_minutes = 30

        token = create_access_token(
            {"sub": 1}, expires_delta=timedelta(hours=2)
        )
        assert isinstance(token, str)

    @patch("bodyup_cloud.app.auth.settings")
    def test_decode_roundtrip(self, mock_settings):
        from jose import jwt

        mock_settings.secret_key = "test-secret-key-for-jwt"
        mock_settings.algorithm = "HS256"
        mock_settings.access_token_expire_minutes = 60

        token = create_access_token({"sub": "99", "role": "admin"})
        payload = jwt.decode(token, "test-secret-key-for-jwt", algorithms=["HS256"])
        assert payload["sub"] == "99"
        assert payload["role"] == "admin"
        assert "exp" in payload
