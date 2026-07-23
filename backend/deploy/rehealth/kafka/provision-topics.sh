#!/bin/sh
set -eu

BOOTSTRAP_SERVERS="${REHEALTH_KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
KAFKA_BIN="${KAFKA_BIN:-/opt/kafka/bin}"
MAIN_RETENTION_MS=604800000
DLQ_RETENTION_MS=2592000000

for topic in rehealth.telemetry.persisted.v1 rehealth.telemetry.quality.v1; do
  "${KAFKA_BIN}/kafka-topics.sh" \
    --bootstrap-server "${BOOTSTRAP_SERVERS}" \
    --create --if-not-exists --topic "${topic}" \
    --partitions "${REHEALTH_KAFKA_MAIN_PARTITIONS:-12}" \
    --replication-factor "${REHEALTH_KAFKA_REPLICATION_FACTOR:-1}" \
    --config "retention.ms=${MAIN_RETENTION_MS}" \
    --config cleanup.policy=delete
done

"${KAFKA_BIN}/kafka-topics.sh" \
  --bootstrap-server "${BOOTSTRAP_SERVERS}" \
  --create --if-not-exists --topic rehealth.telemetry.dlq.v1 \
  --partitions "${REHEALTH_KAFKA_DLQ_PARTITIONS:-3}" \
  --replication-factor "${REHEALTH_KAFKA_REPLICATION_FACTOR:-1}" \
  --config "retention.ms=${DLQ_RETENTION_MS}" \
  --config cleanup.policy=delete
