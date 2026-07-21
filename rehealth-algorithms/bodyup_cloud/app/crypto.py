"""Fernet-based encryption helpers for API keys at rest."""

import base64
import hashlib

from cryptography.fernet import Fernet


def _derive_fernet_key(secret: str) -> bytes:
    key_bytes = hashlib.sha256(secret.encode()).digest()
    return base64.urlsafe_b64encode(key_bytes)


def encrypt_api_key(plaintext: str, secret: str) -> str:
    if not plaintext:
        return ""
    f = Fernet(_derive_fernet_key(secret))
    return f.encrypt(plaintext.encode()).decode()


def decrypt_api_key(ciphertext: str, secret: str) -> str:
    if not ciphertext:
        return ""
    f = Fernet(_derive_fernet_key(secret))
    return f.decrypt(ciphertext.encode()).decode()
