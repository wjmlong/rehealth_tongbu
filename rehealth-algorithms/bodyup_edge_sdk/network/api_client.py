import httpx
from typing import Optional


class BodyUPClient:

    def __init__(self, base_url: str, device_id: str, token: Optional[str] = None):
        self.base_url = base_url.rstrip("/")
        self.device_id = device_id
        self.token = token

    def _headers(self) -> dict:
        h = {"Content-Type": "application/json"}
        if self.token:
            h["Authorization"] = f"Bearer {self.token}"
        return h

    def login(self, email: str, password: str) -> str:
        resp = httpx.post(
            f"{self.base_url}/auth/login",
            data={"username": email, "password": password},
        )
        resp.raise_for_status()
        self.token = resp.json()["access_token"]
        return self.token

    def upload_desensitized(self, payload: dict) -> dict:
        resp = httpx.post(
            f"{self.base_url}/patient/me/upload",
            json=payload,
            headers=self._headers(),
        )
        resp.raise_for_status()
        return resp.json()

    def get_risk_prediction(self) -> dict:
        resp = httpx.post(
            f"{self.base_url}/attribution/predict/{self.device_id}",
            headers=self._headers(),
        )
        resp.raise_for_status()
        return resp.json()

    def get_prescription(self, memory_snapshot: dict) -> dict:
        resp = httpx.post(
            f"{self.base_url}/inference/prescription",
            json={"risk_result": {}, "memory_snapshot": memory_snapshot},
            headers=self._headers(),
        )
        resp.raise_for_status()
        return resp.json()

    def send_alert(self, message: str = "") -> dict:
        resp = httpx.post(
            f"{self.base_url}/patient/me/alert",
            json={"message": message, "include_risk_score": True},
            headers=self._headers(),
        )
        resp.raise_for_status()
        return resp.json()

    def upload_health_data(self, data_type: str, value: float) -> dict:
        resp = httpx.post(
            f"{self.base_url}/patient/me/health-data",
            json={"data_type": data_type, "value": value, "device_id": self.device_id},
            headers=self._headers(),
        )
        resp.raise_for_status()
        return resp.json()
