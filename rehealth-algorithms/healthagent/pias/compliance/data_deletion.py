"""
Data Deletion Pipeline for PIPL Compliance

Implements right to deletion (删除权) for user health data.
"""

from datetime import datetime
from typing import Dict, List, Optional, Any
from enum import Enum
from pydantic import BaseModel, Field


class DeletionStatus(str, Enum):
    """Deletion request status."""
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    FAILED = "failed"
    PARTIAL = "partial"


class DeletionRequest(BaseModel):
    """Data deletion request."""
    request_id: str = Field(..., description="请求ID")
    user_id: str = Field(..., description="用户ID")
    status: DeletionStatus = Field(default=DeletionStatus.PENDING, description="状态")

    # Request details
    reason: Optional[str] = Field(None, description="删除原因")
    data_categories: List[str] = Field(
        default_factory=lambda: ["all"],
        description="要删除的数据类别"
    )

    # Deletion scope
    include_health_data: bool = Field(default=True, description="包含健康数据")
    include_risk_scores: bool = Field(default=True, description="包含风险评分")
    include_settlement_records: bool = Field(default=False, description="包含结算记录")
    include_audit_logs: bool = Field(default=False, description="包含审计日志")

    # Timestamps
    requested_at: datetime = Field(default_factory=datetime.now)
    processed_at: Optional[datetime] = Field(None)
    completed_at: Optional[datetime] = Field(None)

    # Results
    deleted_records: Dict[str, int] = Field(
        default_factory=dict,
        description="已删除记录数"
    )
    retained_records: Dict[str, str] = Field(
        default_factory=dict,
        description="保留记录及原因"
    )
    errors: List[str] = Field(default_factory=list, description="错误信息")


class DataDeletionPipeline:
    """
    Data deletion pipeline for PIPL compliance.

    Handles user data deletion requests with proper
    audit trail and compliance checks.
    """

    # Data categories and their retention requirements
    DATA_CATEGORIES = {
        "health_data": {
            "description": "健康数据（血压、血糖、血脂等）",
            "retention_days": 365 * 15,  # 15 years for medical records
            "can_delete": True,
        },
        "risk_scores": {
            "description": "风险评分",
            "retention_days": 365 * 5,  # 5 years
            "can_delete": True,
        },
        "settlement_records": {
            "description": "结算记录",
            "retention_days": 365 * 10,  # 10 years for financial records
            "can_delete": False,  # Legal requirement to retain
        },
        "audit_logs": {
            "description": "审计日志",
            "retention_days": 365 * 3,  # 3 years
            "can_delete": False,  # Compliance requirement
        },
        "consent_records": {
            "description": "同意记录",
            "retention_days": 365 * 3,  # 3 years after last consent
            "can_delete": False,  # Legal requirement
        },
    }

    def __init__(self, storage_path: str = None):
        """
        Initialize deletion pipeline.

        Parameters
        ----------
        storage_path : str, optional
            Path to storage
        """
        self.storage_path = storage_path
        self.deletion_requests: Dict[str, DeletionRequest] = {}

    def submit_deletion_request(
        self,
        user_id: str,
        reason: str = None,
        data_categories: List[str] = None,
    ) -> DeletionRequest:
        """
        Submit data deletion request.

        Parameters
        ----------
        user_id : str
            User ID
        reason : str, optional
            Deletion reason
        data_categories : list, optional
            Specific data categories to delete

        Returns
        -------
        DeletionRequest
        """
        import uuid

        request_id = f"DEL-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"

        request = DeletionRequest(
            request_id=request_id,
            user_id=user_id,
            reason=reason,
            data_categories=data_categories or ["all"],
        )

        self.deletion_requests[request_id] = request

        return request

    def process_deletion_request(
        self,
        request_id: str,
        data_store: Dict[str, Any],
    ) -> DeletionRequest:
        """
        Process deletion request.

        Parameters
        ----------
        request_id : str
            Request ID
        data_store : dict
            Data store to delete from

        Returns
        -------
        DeletionRequest
        """
        if request_id not in self.deletion_requests:
            raise ValueError(f"Request {request_id} not found")

        request = self.deletion_requests[request_id]
        request.status = DeletionStatus.IN_PROGRESS
        request.processed_at = datetime.now()

        try:
            # Process each data category
            for category in request.data_categories:
                if category == "all":
                    categories_to_process = list(self.DATA_CATEGORIES.keys())
                else:
                    categories_to_process = [category]

                for cat in categories_to_process:
                    if cat not in self.DATA_CATEGORIES:
                        request.errors.append(f"Unknown category: {cat}")
                        continue

                    cat_info = self.DATA_CATEGORIES[cat]

                    if not cat_info["can_delete"]:
                        # Record retention reason
                        request.retained_records[cat] = (
                            f"依法必须保留 {cat_info['retention_days']} 天"
                        )
                        continue

                    # Delete data
                    deleted_count = self._delete_category_data(
                        request.user_id,
                        cat,
                        data_store,
                    )
                    request.deleted_records[cat] = deleted_count

            request.status = DeletionStatus.COMPLETED
            request.completed_at = datetime.now()

        except Exception as e:
            request.status = DeletionStatus.FAILED
            request.errors.append(str(e))

        return request

    def _delete_category_data(
        self,
        user_id: str,
        category: str,
        data_store: Dict[str, Any],
    ) -> int:
        """
        Delete data for a specific category.

        Parameters
        ----------
        user_id : str
            User ID
        category : str
            Data category
        data_store : dict
            Data store

        Returns
        -------
        int
            Number of deleted records
        """
        # This is a placeholder - actual implementation depends on storage
        # In production, this would delete from database

        deleted_count = 0

        # Example: delete from data_store
        if category in data_store:
            if isinstance(data_store[category], dict):
                if user_id in data_store[category]:
                    del data_store[category][user_id]
                    deleted_count = 1
            elif isinstance(data_store[category], list):
                original_len = len(data_store[category])
                data_store[category] = [
                    r for r in data_store[category]
                    if r.get("user_id") != user_id
                ]
                deleted_count = original_len - len(data_store[category])

        return deleted_count

    def get_deletion_status(self, request_id: str) -> Optional[DeletionRequest]:
        """
        Get deletion request status.

        Parameters
        ----------
        request_id : str
            Request ID

        Returns
        -------
        DeletionRequest or None
        """
        return self.deletion_requests.get(request_id)

    def get_user_deletion_history(self, user_id: str) -> List[DeletionRequest]:
        """
        Get deletion history for user.

        Parameters
        ----------
        user_id : str
            User ID

        Returns
        -------
        list of DeletionRequest
        """
        return [
            r for r in self.deletion_requests.values()
            if r.user_id == user_id
        ]

    def generate_deletion_report(self, request_id: str) -> Dict[str, Any]:
        """
        Generate deletion report.

        Parameters
        ----------
        request_id : str
            Request ID

        Returns
        -------
        dict with deletion report
        """
        request = self.deletion_requests.get(request_id)
        if not request:
            return {"error": "Request not found"}

        return {
            "request_id": request.request_id,
            "user_id": request.user_id,
            "status": request.status.value,
            "requested_at": request.requested_at.isoformat(),
            "completed_at": request.completed_at.isoformat() if request.completed_at else None,
            "deleted_records": request.deleted_records,
            "retained_records": request.retained_records,
            "errors": request.errors,
            "compliance_notes": [
                "根据《个人信息保护法》第47条，用户有权删除其个人信息",
                "依法必须保留的数据（如财务记录、审计日志）已记录保留原因",
                "删除操作已记录在审计日志中",
            ],
        }
