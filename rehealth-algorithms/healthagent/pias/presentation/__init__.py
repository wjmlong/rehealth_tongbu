"""
PIAS Presentation Module

ECharts chart configurations, animations, and report templates
for attribution report visualization.
"""

from .echarts import EChartsConfig
from .animations import AnimationConfig
from .templates import ReportTemplate

__all__ = [
    "EChartsConfig",
    "AnimationConfig",
    "ReportTemplate",
]
