"""
PIAS Attribution Module

Two-level attribution system:
- Level 1: Individual Attribution (个人归因)
- Level 2: Group Attribution (群体归因)

Designed for insurance actuary review with comprehensive
statistical rigor and Chinese regulatory compliance.
"""

from .individual import IndividualAttributor
from .group import GroupAttributor
from .report import AttributionReport

__all__ = [
    "IndividualAttributor",
    "GroupAttributor",
    "AttributionReport",
]
