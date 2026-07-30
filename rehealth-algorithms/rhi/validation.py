from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from math import isfinite, sqrt
from statistics import fmean, median
from typing import Iterable, Mapping


class Direction(str, Enum):
    HIGHER_IS_BETTER = "higher_is_better"
    LOWER_IS_BETTER = "lower_is_better"


@dataclass(frozen=True)
class StabilityReport:
    observations: int
    mean: float
    standard_deviation: float
    maximum_day_change: float


@dataclass(frozen=True)
class ResponsivenessReport:
    baseline_median: float
    followup_median: float
    signed_change: float
    directionally_correct: bool


@dataclass(frozen=True)
class MonotonicityReport:
    observations: int
    violations: int
    violation_rate: float


@dataclass(frozen=True)
class FairnessReport:
    device_count: int
    minimum_mean: float
    maximum_mean: float
    maximum_gap: float


def _values(values: Iterable[float], label: str) -> list[float]:
    result = [float(value) for value in values]
    if not result:
        raise ValueError(f"{label} must not be empty")
    if any(not isfinite(value) for value in result):
        raise ValueError(f"{label} must contain only finite values")
    return result


def stability(scores: Iterable[float]) -> StabilityReport:
    values = _values(scores, "scores")
    average = fmean(values)
    variance = fmean((value - average) ** 2 for value in values)
    changes = [abs(right - left) for left, right in zip(values, values[1:])]
    return StabilityReport(
        observations=len(values),
        mean=average,
        standard_deviation=sqrt(variance),
        maximum_day_change=max(changes, default=0.0),
    )


def responsiveness(
    baseline_scores: Iterable[float],
    followup_scores: Iterable[float],
    expected_direction: Direction,
) -> ResponsivenessReport:
    baseline = median(_values(baseline_scores, "baseline_scores"))
    followup = median(_values(followup_scores, "followup_scores"))
    signed_change = followup - baseline
    correct = (
        signed_change >= 0
        if expected_direction == Direction.HIGHER_IS_BETTER
        else signed_change <= 0
    )
    return ResponsivenessReport(
        baseline_median=baseline,
        followup_median=followup,
        signed_change=signed_change,
        directionally_correct=correct,
    )


def monotonicity(
    input_output_pairs: Iterable[tuple[float, float]],
    expected_direction: Direction,
    tolerance: float = 1e-9,
) -> MonotonicityReport:
    pairs = sorted(input_output_pairs, key=lambda pair: pair[0])
    if len(pairs) < 2:
        raise ValueError("input_output_pairs must contain at least two observations")
    if any(not isfinite(value) for pair in pairs for value in pair):
        raise ValueError("input_output_pairs must contain only finite values")
    violations = 0
    for (_, left), (_, right) in zip(pairs, pairs[1:]):
        if expected_direction == Direction.HIGHER_IS_BETTER:
            violations += int(right + tolerance < left)
        else:
            violations += int(right - tolerance > left)
    comparisons = len(pairs) - 1
    return MonotonicityReport(
        observations=len(pairs),
        violations=violations,
        violation_rate=violations / comparisons,
    )


def device_fairness(scores_by_device: Mapping[str, Iterable[float]]) -> FairnessReport:
    if len(scores_by_device) < 2:
        raise ValueError("scores_by_device must contain at least two devices")
    means = {
        device: fmean(_values(scores, f"scores_by_device[{device}]"))
        for device, scores in scores_by_device.items()
    }
    minimum = min(means.values())
    maximum = max(means.values())
    return FairnessReport(
        device_count=len(means),
        minimum_mean=minimum,
        maximum_mean=maximum,
        maximum_gap=maximum - minimum,
    )
