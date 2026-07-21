"""
Audit Trail for PIAS Compliance

Immutable audit logging for all settlement computations
and data processing activities.
"""

from datetime import datetime
from typing import Dict, List, Optional, Any
from enum import Enum
from pydantic import BaseModel, Field
import hashlib
import json


class AuditEventType(str, Enum):
    """Audit event types."""
    DATA_ACCESS = "data_access"
    DATA_MODIFICATION = "data_modification"
    DATA_DELETION = "data_deletion"
    RISK_ASSESSMENT = "risk_assessment"
    SETTLEMENT_COMPUTATION = "settlement_computation"
    CLAIM_PROCESSING = "claim_processing"
    PREMIUM_ADJUSTMENT = "premium_adjustment"
    CONSENT_CHANGE = "consent_change"
    USER_LOGIN = "user_login"
    API_CALL = "api_call"
    SYSTEM_EVENT = "system_event"


class AuditEvent(BaseModel):
    """Audit event record."""
    event_id: str = Field(..., description="事件ID")
    event_type: AuditEventType = Field(..., description="事件类型")
    timestamp: datetime = Field(default_factory=datetime.now, description="时间戳")

    # Actor
    user_id: Optional[str] = Field(None, description="用户ID")
    actor_type: str = Field(default="system", description="操作者类型")
    ip_address: Optional[str] = Field(None, description="IP地址")
    user_agent: Optional[str] = Field(None, description="用户代理")

    # Action
    action: str = Field(..., description="操作")
    resource_type: str = Field(..., description="资源类型")
    resource_id: Optional[str] = Field(None, description="资源ID")

    # Details
    details: Dict[str, Any] = Field(default_factory=dict, description="详细信息")
    old_value: Optional[Any] = Field(None, description="旧值")
    new_value: Optional[Any] = Field(None, description="新值")

    # Result
    success: bool = Field(default=True, description="是否成功")
    error_message: Optional[str] = Field(None, description="错误信息")

    # Hash chain
    previous_hash: Optional[str] = Field(None, description="前一条记录哈希")
    current_hash: Optional[str] = Field(None, description="当前记录哈希")


class AuditTrail:
    """
    Immutable audit trail for PIAS compliance.

    Maintains hash chain for tamper detection.
    """

    def __init__(self, storage_path: str = None):
        """
        Initialize audit trail.

        Parameters
        ----------
        storage_path : str, optional
            Path to audit log storage
        """
        self.storage_path = storage_path
        self.events: List[AuditEvent] = []
        self.last_hash: Optional[str] = None

    def log_event(
        self,
        event_type: AuditEventType,
        action: str,
        resource_type: str,
        user_id: str = None,
        resource_id: str = None,
        details: Dict[str, Any] = None,
        old_value: Any = None,
        new_value: Any = None,
        success: bool = True,
        error_message: str = None,
        ip_address: str = None,
        user_agent: str = None,
    ) -> AuditEvent:
        """
        Log audit event.

        Parameters
        ----------
        event_type : AuditEventType
            Event type
        action : str
            Action performed
        resource_type : str
            Resource type
        user_id : str, optional
            User ID
        resource_id : str, optional
            Resource ID
        details : dict, optional
            Additional details
        old_value : any, optional
            Previous value
        new_value : any, optional
            New value
        success : bool
            Whether action succeeded
        error_message : str, optional
            Error message if failed
        ip_address : str, optional
            IP address
        user_agent : str, optional
            User agent

        Returns
        -------
        AuditEvent
        """
        import uuid

        event_id = f"AUDIT-{datetime.now().strftime('%Y%m%d%H%M%S')}-{uuid.uuid4().hex[:8]}"

        event = AuditEvent(
            event_id=event_id,
            event_type=event_type,
            user_id=user_id,
            ip_address=ip_address,
            user_agent=user_agent,
            action=action,
            resource_type=resource_type,
            resource_id=resource_id,
            details=details or {},
            old_value=old_value,
            new_value=new_value,
            success=success,
            error_message=error_message,
            previous_hash=self.last_hash,
        )

        # Calculate hash
        event.current_hash = self._calculate_hash(event)

        # Store event
        self.events.append(event)
        self.last_hash = event.current_hash

        return event

    def _calculate_hash(self, event: AuditEvent) -> str:
        """
        Calculate SHA-256 hash for event.

        Parameters
        ----------
        event : AuditEvent
            Audit event

        Returns
        -------
        str
            SHA-256 hash
        """
        # Create hash input from key fields
        hash_input = json.dumps({
            "event_id": event.event_id,
            "event_type": event.event_type.value,
            "timestamp": event.timestamp.isoformat(),
            "user_id": event.user_id,
            "action": event.action,
            "resource_type": event.resource_type,
            "resource_id": event.resource_id,
            "details": event.details,
            "success": event.success,
            "previous_hash": event.previous_hash,
        }, sort_keys=True)

        return hashlib.sha256(hash_input.encode()).hexdigest()

    def verify_chain(self) -> Dict[str, Any]:
        """
        Verify audit trail integrity.

        Returns
        -------
        dict with verification result
        """
        if not self.events:
            return {"valid": True, "message": "No events to verify"}

        errors = []
        for i, event in enumerate(self.events):
            # Verify hash
            expected_hash = self._calculate_hash(event)
            if event.current_hash != expected_hash:
                errors.append(f"Event {event.event_id}: hash mismatch")

            # Verify chain
            if i > 0:
                if event.previous_hash != self.events[i - 1].current_hash:
                    errors.append(f"Event {event.event_id}: chain break")

        return {
            "valid": len(errors) == 0,
            "total_events": len(self.events),
            "errors": errors,
        }

    def get_events(
        self,
        event_type: AuditEventType = None,
        user_id: str = None,
        resource_type: str = None,
        start_time: datetime = None,
        end_time: datetime = None,
        limit: int = 100,
    ) -> List[AuditEvent]:
        """
        Query audit events.

        Parameters
        ----------
        event_type : AuditEventType, optional
            Filter by event type
        user_id : str, optional
            Filter by user ID
        resource_type : str, optional
            Filter by resource type
        start_time : datetime, optional
            Start time filter
        end_time : datetime, optional
            End time filter
        limit : int
            Maximum results

        Returns
        -------
        list of AuditEvent
        """
        filtered = self.events

        if event_type:
            filtered = [e for e in filtered if e.event_type == event_type]
        if user_id:
            filtered = [e for e in filtered if e.user_id == user_id]
        if resource_type:
            filtered = [e for e in filtered if e.resource_type == resource_type]
        if start_time:
            filtered = [e for e in filtered if e.timestamp >= start_time]
        if end_time:
            filtered = [e for e in filtered if e.timestamp <= end_time]

        return filtered[-limit:]

    def get_user_activity(
        self,
        user_id: str,
        limit: int = 50,
    ) -> List[AuditEvent]:
        """
        Get user activity log.

        Parameters
        ----------
        user_id : str
            User ID
        limit : int
            Maximum results

        Returns
        -------
        list of AuditEvent
        """
        return self.get_events(user_id=user_id, limit=limit)

    def generate_audit_report(
        self,
        start_time: datetime,
        end_time: datetime,
    ) -> Dict[str, Any]:
        """
        Generate audit report for time period.

        Parameters
        ----------
        start_time : datetime
            Report start time
        end_time : datetime
            Report end time

        Returns
        -------
        dict with audit report
        """
        events = self.get_events(
            start_time=start_time,
            end_time=end_time,
            limit=10000,
        )

        # Count by type
        type_counts = {}
        for event in events:
            type_name = event.event_type.value
            type_counts[type_name] = type_counts.get(type_name, 0) + 1

        # Count by user
        user_counts = {}
        for event in events:
            if event.user_id:
                user_counts[event.user_id] = user_counts.get(event.user_id, 0) + 1

        # Count failures
        failures = [e for e in events if not e.success]

        return {
            "period": {
                "start": start_time.isoformat(),
                "end": end_time.isoformat(),
            },
            "total_events": len(events),
            "events_by_type": type_counts,
            "events_by_user": user_counts,
            "failures": len(failures),
            "failure_details": [
                {
                    "event_id": e.event_id,
                    "action": e.action,
                    "error": e.error_message,
                }
                for e in failures[:10]
            ],
            "chain_valid": self.verify_chain()["valid"],
        }
