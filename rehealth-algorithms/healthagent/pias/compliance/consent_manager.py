"""
Consent Manager for PIPL Compliance

Manages user consent for health data processing
in compliance with China's Personal Information Protection Law (PIPL).
"""

from datetime import datetime, date
from typing import Dict, List, Optional, Any
from enum import Enum
from pydantic import BaseModel, Field


class ConsentType(str, Enum):
    """Types of consent."""
    HEALTH_DATA_PROCESSING = "health_data_processing"
    RISK_ASSESSMENT = "risk_assessment"
    INSURANCE_SHARING = "insurance_sharing"
    RESEARCH_USE = "research_use"
    CROSS_BORDER_TRANSFER = "cross_border_transfer"
    MARKETING = "marketing"


class ConsentStatus(str, Enum):
    """Consent status."""
    PENDING = "pending"
    GRANTED = "granted"
    REVOKED = "revoked"
    EXPIRED = "expired"


class ConsentRecord(BaseModel):
    """Consent record model."""
    consent_id: str = Field(..., description="同意记录ID")
    user_id: str = Field(..., description="用户ID")
    consent_type: ConsentType = Field(..., description="同意类型")
    status: ConsentStatus = Field(..., description="同意状态")

    # Consent details
    purpose: str = Field(..., description="数据使用目的")
    data_categories: List[str] = Field(..., description="数据类别")
    processing_methods: List[str] = Field(..., description="处理方式")
    retention_period: Optional[int] = Field(None, description="保留期限(天)")

    # Third party sharing
    third_parties: List[str] = Field(default_factory=list, description="第三方接收方")
    cross_border: bool = Field(default=False, description="是否跨境传输")

    # Timestamps
    granted_at: Optional[datetime] = Field(None, description="同意时间")
    revoked_at: Optional[datetime] = Field(None, description="撤回时间")
    expires_at: Optional[datetime] = Field(None, description="过期时间")

    # Audit
    ip_address: Optional[str] = Field(None, description="IP地址")
    user_agent: Optional[str] = Field(None, description="用户代理")
    consent_version: str = Field(default="1.0", description="同意版本")

    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)


class ConsentManager:
    """
    Manage user consent for PIPL compliance.

    Handles consent collection, verification, and revocation.
    """

    # Required consents for PIAS insurance
    REQUIRED_CONSENTS = [
        ConsentType.HEALTH_DATA_PROCESSING,
        ConsentType.RISK_ASSESSMENT,
    ]

    # Optional consents
    OPTIONAL_CONSENTS = [
        ConsentType.INSURANCE_SHARING,
        ConsentType.RESEARCH_USE,
    ]

    def __init__(self, storage_path: str = None):
        """
        Initialize consent manager.

        Parameters
        ----------
        storage_path : str, optional
            Path to consent storage (JSON file or database)
        """
        self.storage_path = storage_path
        self.consent_records: Dict[str, List[ConsentRecord]] = {}

    def collect_consent(
        self,
        user_id: str,
        consent_type: ConsentType,
        purpose: str,
        data_categories: List[str],
        processing_methods: List[str],
        third_parties: List[str] = None,
        cross_border: bool = False,
        retention_period: int = None,
        ip_address: str = None,
        user_agent: str = None,
    ) -> ConsentRecord:
        """
        Collect user consent.

        Parameters
        ----------
        user_id : str
            User ID
        consent_type : ConsentType
            Type of consent
        purpose : str
            Data processing purpose
        data_categories : list
            Categories of data to be processed
        processing_methods : list
            Data processing methods
        third_parties : list, optional
            Third party recipients
        cross_border : bool
            Whether cross-border transfer
        retention_period : int, optional
            Data retention period in days
        ip_address : str, optional
            User IP address
        user_agent : str, optional
            User agent string

        Returns
        -------
        ConsentRecord
        """
        import uuid

        consent_id = f"CONSENT-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"

        record = ConsentRecord(
            consent_id=consent_id,
            user_id=user_id,
            consent_type=consent_type,
            status=ConsentStatus.GRANTED,
            purpose=purpose,
            data_categories=data_categories,
            processing_methods=processing_methods,
            third_parties=third_parties or [],
            cross_border=cross_border,
            retention_period=retention_period,
            granted_at=datetime.now(),
            ip_address=ip_address,
            user_agent=user_agent,
        )

        # Store consent
        if user_id not in self.consent_records:
            self.consent_records[user_id] = []
        self.consent_records[user_id].append(record)

        return record

    def revoke_consent(
        self,
        user_id: str,
        consent_type: ConsentType,
    ) -> Optional[ConsentRecord]:
        """
        Revoke user consent.

        Parameters
        ----------
        user_id : str
            User ID
        consent_type : ConsentType
            Type of consent to revoke

        Returns
        -------
        ConsentRecord or None
        """
        if user_id not in self.consent_records:
            return None

        for record in reversed(self.consent_records[user_id]):
            if record.consent_type == consent_type and record.status == ConsentStatus.GRANTED:
                record.status = ConsentStatus.REVOKED
                record.revoked_at = datetime.now()
                record.updated_at = datetime.now()
                return record

        return None

    def check_consent(
        self,
        user_id: str,
        consent_type: ConsentType,
    ) -> bool:
        """
        Check if user has granted consent.

        Parameters
        ----------
        user_id : str
            User ID
        consent_type : ConsentType
            Type of consent to check

        Returns
        -------
        bool
            True if consent is granted and not expired
        """
        if user_id not in self.consent_records:
            return False

        for record in reversed(self.consent_records[user_id]):
            if record.consent_type == consent_type:
                if record.status == ConsentStatus.GRANTED:
                    # Check expiration
                    if record.expires_at and record.expires_at < datetime.now():
                        record.status = ConsentStatus.EXPIRED
                        return False
                    return True
                elif record.status == ConsentStatus.REVOKED:
                    return False

        return False

    def get_consent_history(
        self,
        user_id: str,
        consent_type: ConsentType = None,
    ) -> List[ConsentRecord]:
        """
        Get consent history for user.

        Parameters
        ----------
        user_id : str
            User ID
        consent_type : ConsentType, optional
            Filter by consent type

        Returns
        -------
        list of ConsentRecord
        """
        if user_id not in self.consent_records:
            return []

        records = self.consent_records[user_id]
        if consent_type:
            records = [r for r in records if r.consent_type == consent_type]

        return records

    def get_consent_summary(self, user_id: str) -> Dict[str, Any]:
        """
        Get consent summary for user.

        Parameters
        ----------
        user_id : str
            User ID

        Returns
        -------
        dict with consent summary
        """
        if user_id not in self.consent_records:
            return {"user_id": user_id, "consents": {}}

        summary = {}
        for consent_type in ConsentType:
            granted = self.check_consent(user_id, consent_type)
            summary[consent_type.value] = {
                "granted": granted,
                "required": consent_type in self.REQUIRED_CONSENTS,
            }

        return {
            "user_id": user_id,
            "consents": summary,
            "total_records": len(self.consent_records[user_id]),
        }

    def generate_consent_form(
        self,
        user_id: str,
        purpose: str,
        data_categories: List[str],
        processing_methods: List[str],
        third_parties: List[str] = None,
    ) -> Dict[str, Any]:
        """
        Generate consent form for user.

        Parameters
        ----------
        user_id : str
            User ID
        purpose : str
            Data processing purpose
        data_categories : list
            Categories of data
        processing_methods : list
            Processing methods
        third_parties : list, optional
            Third party recipients

        Returns
        -------
        dict with consent form
        """
        return {
            "user_id": user_id,
            "form_version": "1.0",
            "generated_at": datetime.now().isoformat(),
            "sections": [
                {
                    "title": "数据处理目的",
                    "content": purpose,
                    "required": True,
                },
                {
                    "title": "数据类别",
                    "content": data_categories,
                    "required": True,
                },
                {
                    "title": "处理方式",
                    "content": processing_methods,
                    "required": True,
                },
                {
                    "title": "第三方接收方",
                    "content": third_parties or ["无"],
                    "required": False,
                },
                {
                    "title": "您的权利",
                    "content": [
                        "您有权随时撤回同意",
                        "您有权访问、更正、删除您的个人信息",
                        "您有权撤回同意不影响撤回前已进行的处理",
                    ],
                    "required": True,
                },
            ],
            "required_consents": [c.value for c in self.REQUIRED_CONSENTS],
            "optional_consents": [c.value for c in self.OPTIONAL_CONSENTS],
        }
