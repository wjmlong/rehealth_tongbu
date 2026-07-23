#!/bin/sh
set -eu

: "${REHEALTH_KAFKA_DEVICE_PRINCIPAL:?device publisher principal is required}"
: "${REHEALTH_KAFKA_JEECG_PRINCIPAL:?Jeecg consumer principal is required}"

BOOTSTRAP_SERVERS="${REHEALTH_KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
KAFKA_BIN="${KAFKA_BIN:-/opt/kafka/bin}"
CONFIG_ARGUMENTS=""
if [ -n "${REHEALTH_KAFKA_ADMIN_CONFIG:-}" ]; then
  CONFIG_ARGUMENTS="--command-config ${REHEALTH_KAFKA_ADMIN_CONFIG}"
fi

# shellcheck disable=SC2086
"${KAFKA_BIN}/kafka-acls.sh" --bootstrap-server "${BOOTSTRAP_SERVERS}" ${CONFIG_ARGUMENTS} \
  --add --allow-principal "${REHEALTH_KAFKA_DEVICE_PRINCIPAL}" \
  --operation Write --operation Describe \
  --topic rehealth.telemetry.persisted.v1 --topic rehealth.telemetry.quality.v1

# shellcheck disable=SC2086
"${KAFKA_BIN}/kafka-acls.sh" --bootstrap-server "${BOOTSTRAP_SERVERS}" ${CONFIG_ARGUMENTS} \
  --add --allow-principal "${REHEALTH_KAFKA_JEECG_PRINCIPAL}" \
  --operation Read --operation Describe \
  --topic rehealth.telemetry.persisted.v1 --topic rehealth.telemetry.quality.v1 \
  --group rehealth-jeecg-projection-v1

# shellcheck disable=SC2086
"${KAFKA_BIN}/kafka-acls.sh" --bootstrap-server "${BOOTSTRAP_SERVERS}" ${CONFIG_ARGUMENTS} \
  --add --allow-principal "${REHEALTH_KAFKA_JEECG_PRINCIPAL}" \
  --operation Write --operation Describe --topic rehealth.telemetry.dlq.v1
