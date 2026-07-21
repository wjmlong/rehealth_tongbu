# -*- coding: utf-8 -*-
"""
数据源注册中心 — 管理多个异构数据源的加载、预处理和合并

Usage::

    from bodyup_cloud.data_sources.registry import DataSourceRegistry
    from bodyup_cloud.data_sources.nhanes import NHANESSource

    registry = DataSourceRegistry()
    registry.register(NHANESSource())

    # Load and preprocess a single source
    df = registry.load_and_preprocess("nhanes", "/path/to/NHANES")
    print(registry.list_sources())

    # Merge multiple standardized datasets
    merged = registry.merge_datasets({
        "nhanes": nhanes_df,
        "charls": charls_df,
    })
"""

import pandas as pd

from .base import STANDARD_FEATURES, DataSourceBase


class DataSourceRegistry:
    """Central registry for all medical data sources.

    Provides a uniform interface to load, preprocess, validate, and merge
    heterogeneous datasets (NHANES, CHARLS, MIMIC-IV, etc.) into the
    standard 16-feature format used by ReHealth's CVD risk model.
    """

    def __init__(self) -> None:
        self._sources: dict[str, DataSourceBase] = {}

    def register(self, source: DataSourceBase) -> None:
        """Register a data source by its ``name`` attribute."""
        self._sources[source.name] = source

    def get(self, name: str) -> DataSourceBase:
        """Retrieve a registered data source by name.

        Raises
        ------
        KeyError
            If no source with the given name is registered.
        """
        if name not in self._sources:
            available = list(self._sources.keys())
            raise KeyError(
                f"Data source '{name}' not registered. "
                f"Available sources: {available}"
            )
        return self._sources[name]

    def list_sources(self) -> list[dict[str, str]]:
        """Return metadata for all registered sources."""
        return [
            {
                "name": s.name,
                "version": s.version,
                "description": s.description,
            }
            for s in self._sources.values()
        ]

    def load_and_preprocess(self, name: str, path: str) -> pd.DataFrame:
        """Full pipeline: load → preprocess → standardize for one source.

        Parameters
        ----------
        name : str
            Registered data source name (e.g. "nhanes").
        path : str
            Path to the raw data directory/file.

        Returns
        -------
        pd.DataFrame
            Standardized DataFrame with the 16 standard features.
        """
        source = self.get(name)
        raw = source.load_raw(path)
        processed = source.preprocess(raw)
        standard = source.to_standard_features(processed)
        return standard

    def validate_source(self, name: str, df: pd.DataFrame) -> dict:
        """Run data quality validation on a standardized DataFrame.

        Parameters
        ----------
        name : str
            Data source name (for metadata in the report).
        df : pd.DataFrame
            Standardized DataFrame to validate.

        Returns
        -------
        dict
            Quality report with row count, missing %, range violations.
        """
        source = self.get(name)
        return source.validate(df)

    def merge_datasets(
        self,
        datasets: dict[str, pd.DataFrame],
        validate: bool = True,
    ) -> pd.DataFrame:
        """Merge multiple standardized datasets with source tracking.

        Concatenates DataFrames and adds a ``source`` column to identify
        which dataset each row came from.

        Parameters
        ----------
        datasets : dict[str, pd.DataFrame]
            Mapping of source name → standardized DataFrame.
        validate : bool
            If True, verify each DataFrame contains all standard features
            before merging.

        Returns
        -------
        pd.DataFrame
            Combined DataFrame with an additional ``source`` column.

        Raises
        ------
        ValueError
            If validate=True and any DataFrame is missing standard features.
        """
        frames = []
        for source_name, df in datasets.items():
            if validate:
                missing = [f for f in STANDARD_FEATURES if f not in df.columns]
                if missing:
                    raise ValueError(
                        f"Dataset '{source_name}' is missing standard features: {missing}"
                    )
            df = df.copy()
            df["source"] = source_name
            frames.append(df)

        if not frames:
            return pd.DataFrame(columns=STANDARD_FEATURES + ["source"])

        merged = pd.concat(frames, ignore_index=True)
        return merged
