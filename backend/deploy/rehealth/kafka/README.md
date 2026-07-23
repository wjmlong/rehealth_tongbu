# ReHealth telemetry Kafka operations

`provision-topics.sh` creates the two lifecycle topics with seven-day retention
and the consumer DLQ with thirty-day retention. The opaque `device_ref` is the
record key, so all events for one device remain ordered within one partition.

Run `provision-acls.sh` with externally supplied principals. The Device Service
principal receives write-only access to the lifecycle topics. The Jeecg
projection principal receives read access to those topics and write access to
the DLQ. Neither principal receives cluster administration or unrelated-topic
access.

Production must set `REHEALTH_KAFKA_SECURITY_PROTOCOL=SASL_SSL`, inject the
SASL JAAS material and truststore password through the platform secret manager,
and mount the truststore read-only. Do not put credentials in Compose, Git,
application YAML, command history, or evidence. The checked-in Compose
PLAINTEXT listener is development-only on an internal Docker network.

Lifecycle messages contain only schema/event/batch IDs, opaque tenant/user/device
references, time windows, counts, quality summary, and persistence status.
Metric values, tokens, phone numbers, raw PPG/RRI, and model inference inputs are
forbidden by both publisher and consumer parsers.
