# PIAS model-service image (rehealth-algorithms).
# Build context = repo root (../ from this docker/ dir) so we can COPY api + healthagent.
FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONPATH=/app \
    PIP_NO_CACHE_DIR=1 \
    TZ=Asia/Shanghai

WORKDIR /app

# Minimal system deps. numpy/scipy/sklearn ship manylinux wheels for 3.12,
# but a few transitive builds want a compiler; keep build-essential for safety.
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential curl \
    && rm -rf /var/lib/apt/lists/*

COPY docker/requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt

# Application source (immutable in prod; bind-mounted in dev for live reload).
COPY api /app/api
COPY healthagent /app/healthagent

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8000/health').status==200 else 1)"

CMD ["sh", "-c", "uvicorn api.main:app --host 0.0.0.0 --port 8000"]
