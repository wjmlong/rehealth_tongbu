from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Protocol


@dataclass(frozen=True, slots=True)
class GateError(Exception):
    message: str

    def __str__(self) -> str:
        return self.message


@dataclass(frozen=True, slots=True)
class CutoverRequest:
    approval: Path
    reconciliation: Path
    signature: Path
    public_key: Path
    routes: Path
    audit: Path
    action: str
    cases: tuple[str, ...]


class SignatureVerifier(Protocol):
    def verify(self, reconciliation: bytes, signature: bytes, public_key: bytes) -> None: ...
