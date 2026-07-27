from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, TimeoutError
from dataclasses import dataclass
from threading import Lock
from time import monotonic

from app.risk_scorer import ModelUnavailableError, RiskScorer
from app.schemas import CvdFeatureVector, RiskEvaluateResponse


@dataclass(frozen=True, slots=True)
class ModelCallFailure(RuntimeError):
    code: str
    message: str

    def __str__(self) -> str:
        return self.message


class ModelExecutionGuard:
    def __init__(
        self,
        scorer: RiskScorer,
        timeout_seconds: float,
        failure_threshold: int,
        reset_seconds: float,
    ) -> None:
        self._scorer = scorer
        self._timeout_seconds = timeout_seconds
        self._failure_threshold = failure_threshold
        self._reset_seconds = reset_seconds
        self._executor = ThreadPoolExecutor(max_workers=4, thread_name_prefix="rehealth-model")
        self._lock = Lock()
        self._consecutive_failures = 0
        self._opened_until = 0.0

    def evaluate(self, vector: CvdFeatureVector) -> RiskEvaluateResponse:
        with self._lock:
            if monotonic() < self._opened_until:
                raise ModelCallFailure("model_circuit_open", "model execution circuit is open")
        future = self._executor.submit(self._scorer.evaluate, vector)
        try:
            result = future.result(timeout=self._timeout_seconds)
        except TimeoutError as error:
            future.cancel()
            self._record_failure()
            raise ModelCallFailure("model_timeout", "model evaluation timed out") from error
        except ModelUnavailableError as error:
            raise ModelCallFailure("model_unavailable", "reviewed model is unavailable") from error
        except Exception as error:  # noqa: BROAD_EXCEPT_OK
            self._record_failure()
            raise ModelCallFailure("model_execution_failed", "model evaluation failed") from error
        self._record_success()
        return result

    def close(self) -> None:
        self._executor.shutdown(wait=False, cancel_futures=True)

    def _record_failure(self) -> None:
        with self._lock:
            self._consecutive_failures += 1
            if self._consecutive_failures >= self._failure_threshold:
                self._opened_until = monotonic() + self._reset_seconds

    def _record_success(self) -> None:
        with self._lock:
            self._consecutive_failures = 0
            self._opened_until = 0.0
