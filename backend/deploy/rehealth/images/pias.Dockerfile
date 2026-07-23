FROM python:3.11.13-slim-bookworm@sha256:86adf8dbadc3d6e82ee5dd2c74bec2e1c2467cdad47886280501df722372d2e1

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PYTHONPATH=/opt/rehealth \
    PIP_NO_CACHE_DIR=1
RUN groupadd --system rehealth \
    && useradd --system --gid rehealth --home-dir /opt/rehealth rehealth
WORKDIR /opt/rehealth
COPY rehealth-algorithms/docker/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY rehealth-algorithms/api ./api
COPY rehealth-algorithms/healthagent ./healthagent
USER rehealth
EXPOSE 8010
ENTRYPOINT ["uvicorn", "api.production_main:app", "--host", "0.0.0.0", "--port", "8010", "--no-access-log"]
