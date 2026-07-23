FROM python:3.11.13-slim-bookworm@sha256:86adf8dbadc3d6e82ee5dd2c74bec2e1c2467cdad47886280501df722372d2e1

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1
RUN groupadd --system rehealth \
    && useradd --system --gid rehealth --home-dir /opt/rehealth rehealth
WORKDIR /opt/rehealth
COPY model-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY model-service/app ./app
USER rehealth
EXPOSE 8000
ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
