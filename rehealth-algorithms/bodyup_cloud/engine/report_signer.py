"""
Ed25519 digital signatures for attribution reports.

Part of bodyup_cloud.engine — V1 specification.
"""

import json
import time
import base64

from cryptography.hazmat.primitives.asymmetric.ed25519 import (
    Ed25519PrivateKey,
    Ed25519PublicKey,
)
from cryptography.hazmat.primitives import serialization


class ReportSigner:
    """Sign and verify attribution reports using Ed25519."""

    def __init__(self, private_key: Ed25519PrivateKey):
        self.private_key = private_key

    def sign_report(
        self, attribution_result: dict, device_id: str, disease_type: str
    ) -> dict:
        """Create a signed report envelope.

        Parameters
        ----------
        attribution_result : dict
            The output of GroupAttributionEngine.estimate() or similar.
        device_id : str
            Identifier for the originating device.
        disease_type : str
            E.g. "cvd", "hypertension".

        Returns
        -------
        dict with version, signature_algorithm, payload, and signature.
        """
        payload = {
            "device_id": device_id,
            "disease_type": disease_type,
            "timestamp": int(time.time()),
            **attribution_result,
        }
        canonical = json.dumps(
            payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False
        ).encode("utf-8")
        signature = self.private_key.sign(canonical)
        return {
            "version": "1.0.0",
            "signature_algorithm": "Ed25519",
            "payload": payload,
            "signature": base64.b64encode(signature).decode("ascii"),
        }

    @staticmethod
    def verify(report: dict, public_key: Ed25519PublicKey) -> bool:
        """Verify the Ed25519 signature on a signed report.

        Returns True if valid, False on any error.
        """
        try:
            payload = report["payload"]
            canonical = json.dumps(
                payload,
                sort_keys=True,
                separators=(",", ":"),
                ensure_ascii=False,
            ).encode("utf-8")
            signature = base64.b64decode(report["signature"])
            public_key.verify(signature, canonical)
            return True
        except Exception:
            return False


def generate_keypair() -> tuple[bytes, bytes]:
    """Generate a fresh Ed25519 key pair.

    Returns
    -------
    (private_pem, public_pem) — both as PEM-encoded bytes.
    """
    private_key = Ed25519PrivateKey.generate()
    public_key = private_key.public_key()
    private_pem = private_key.private_bytes(
        serialization.Encoding.PEM,
        serialization.PrivateFormat.PKCS8,
        serialization.NoEncryption(),
    )
    public_pem = public_key.public_bytes(
        serialization.Encoding.PEM,
        serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    return private_pem, public_pem
