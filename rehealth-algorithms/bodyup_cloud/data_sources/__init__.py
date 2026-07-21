# -*- coding: utf-8 -*-
"""
bodyup_cloud.data_sources — 多源医学数据统一管道

Standardizes heterogeneous medical datasets (NHANES, CHARLS, MIMIC-IV)
to a common 16-feature format for CVD risk model training.

Quick start::

    from bodyup_cloud.data_sources import DataSourceRegistry, NHANESSource

    registry = DataSourceRegistry()
    registry.register(NHANESSource())
    df = registry.load_and_preprocess("nhanes", "/path/to/NHANES")
"""

from .base import (
    CATEGORICAL_FEATURES,
    CONTINUOUS_FEATURES,
    STANDARD_FEATURES,
    VALID_RANGES,
    DataSourceBase,
)
from .charls import CHARLSSource
from .mimic_iv import MIMICIVSource
from .nhanes import NHANESSource
from .registry import DataSourceRegistry

__all__ = [
    "STANDARD_FEATURES",
    "CONTINUOUS_FEATURES",
    "CATEGORICAL_FEATURES",
    "VALID_RANGES",
    "DataSourceBase",
    "NHANESSource",
    "CHARLSSource",
    "MIMICIVSource",
    "DataSourceRegistry",
]
