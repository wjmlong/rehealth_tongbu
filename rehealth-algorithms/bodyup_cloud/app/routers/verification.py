from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from bodyup_cloud.engine.report_signer import ReportSigner
from cryptography.hazmat.primitives.serialization import load_pem_public_key

router = APIRouter(prefix="/verification", tags=["verification"])


class VerifyRequest(BaseModel):
    report: dict
    public_key_pem: str


@router.post("/verify")
async def verify_report(req: VerifyRequest):
    try:
        pub_key = load_pem_public_key(req.public_key_pem.encode())
    except Exception:
        raise HTTPException(400, "Invalid public key PEM")
    valid = ReportSigner.verify(req.report, pub_key)
    return {"valid": valid}
