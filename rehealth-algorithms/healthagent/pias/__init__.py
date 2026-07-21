"""
PIAS Engine - Predict, Intervene, Attribute, Settle

CVD risk prediction and intervention attribution platform.
Integrated from BodyUP V1 PIAS engine.
"""

from .risk_scorer import CVDRiskScorer, ModelRegistry
from .feature_engineering import FeatureEngineer, FeatureSelector
from .enhanced_scorer import EnhancedCVDRiskScorer, EnhancedModelRegistry
from .knowledge_distillation import KnowledgeDistiller, DistillationPipeline
from .china_calibration import ChinesePopulationCalibrator, ChineseFeatureThresholds
from .actuarial_validation import ActuarialValidator, FraminghamRiskScore, ChinaPARRiskScore

# Attribution module (new structure)
from .attribution import IndividualAttributor, GroupAttributor, AttributionReport

# Legacy modules (keep for backward compatibility)
from .individual_prediction import IndividualPredictor
from .group_attribution import GroupAttributor as LegacyGroupAttributor
from .enhanced_attribution import EnhancedGroupAttributor

# Insurance module
from .insurance import (
    InsurancePolicy,
    SettlementClaim,
    InsurerPartner,
    PremiumAdjustment,
    PolicyType,
    ClaimStatus,
    SettlementEngine,
    PremiumCalculator,
    SettlementReport,
    QuarterlyReport,
    AnnualReport,
    IndividualClaimReport,
)

# Compliance module
from .compliance import (
    ConsentManager,
    DataDeletionPipeline,
    AuditTrail,
)

__all__ = [
    # Risk Scoring
    "CVDRiskScorer",
    "ModelRegistry",
    "EnhancedCVDRiskScorer",
    "EnhancedModelRegistry",
    # Attribution (new)
    "IndividualAttributor",
    "GroupAttributor",
    "AttributionReport",
    # Attribution (legacy)
    "IndividualPredictor",
    "LegacyGroupAttributor",
    "EnhancedGroupAttributor",
    # Feature Engineering
    "FeatureEngineer",
    "FeatureSelector",
    # Knowledge Distillation
    "KnowledgeDistiller",
    "DistillationPipeline",
    # China Calibration
    "ChinesePopulationCalibrator",
    "ChineseFeatureThresholds",
    # Actuarial Validation
    "ActuarialValidator",
    "FraminghamRiskScore",
    "ChinaPARRiskScore",
    # Insurance
    "InsurancePolicy",
    "SettlementClaim",
    "InsurerPartner",
    "PremiumAdjustment",
    "PolicyType",
    "ClaimStatus",
    "SettlementEngine",
    "PremiumCalculator",
    "SettlementReport",
    "QuarterlyReport",
    "AnnualReport",
    "IndividualClaimReport",
    # Compliance
    "ConsentManager",
    "DataDeletionPipeline",
    "AuditTrail",
]
