"""Tests for ReportSigner (Ed25519 sign & verify)."""

from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from bodyup_cloud.engine.report_signer import ReportSigner, generate_keypair


def _keypair():
    priv = Ed25519PrivateKey.generate()
    pub = priv.public_key()
    return priv, pub


class TestReportSigner:
    def test_sign_and_verify_roundtrip(self):
        priv, pub = _keypair()
        signer = ReportSigner(priv)
        report = signer.sign_report(
            {"att": -0.08, "status": "success"},
            device_id="dev_0001",
            disease_type="cvd",
        )
        assert report["version"] == "1.0.0"
        assert report["signature_algorithm"] == "Ed25519"
        assert "signature" in report
        assert ReportSigner.verify(report, pub)

    def test_verify_fails_with_wrong_key(self):
        priv1, _ = _keypair()
        _, pub2 = _keypair()
        signer = ReportSigner(priv1)
        report = signer.sign_report({"att": -0.05}, "dev_0002", "cvd")
        assert not ReportSigner.verify(report, pub2)

    def test_verify_fails_on_tampered_payload(self):
        priv, pub = _keypair()
        signer = ReportSigner(priv)
        report = signer.sign_report({"att": -0.08}, "dev_0003", "cvd")
        report["payload"]["att"] = -0.99
        assert not ReportSigner.verify(report, pub)

    def test_payload_contains_required_fields(self):
        priv, _ = _keypair()
        signer = ReportSigner(priv)
        report = signer.sign_report({"att": -0.1}, "dev_0004", "cvd")
        payload = report["payload"]
        assert payload["device_id"] == "dev_0004"
        assert payload["disease_type"] == "cvd"
        assert "timestamp" in payload
        assert payload["att"] == -0.1


class TestGenerateKeypair:
    def test_generates_pem_bytes(self):
        priv_pem, pub_pem = generate_keypair()
        assert priv_pem.startswith(b"-----BEGIN PRIVATE KEY-----")
        assert pub_pem.startswith(b"-----BEGIN PUBLIC KEY-----")
