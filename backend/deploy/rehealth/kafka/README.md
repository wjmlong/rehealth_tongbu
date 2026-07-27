# ReHealth telemetry Kafka operations

`provision-topics.sh` creates the two lifecycle topics with seven-day retention
and the consumer DLQ with thirty-day retention. The opaque `device_ref` is the
record key, so all events for one device remain ordered within one partition.

Run `provision-acls.sh` with externally supplied principals. The Device Service
principal receives write-only access to the lifecycle topics. The Jeecg
projection principal receives read access to those topics and write access to
the DLQ. Neither principal receives cluster administration or unrelated-topic
access.

Production must set `REHEALTH_KAFKA_SECURITY_PROTOCOL=SASL_SSL`. The platform
secret manager must materialize `kafka_sasl_jaas_config`,
`kafka_truststore.p12`, and `kafka_truststore_password` as read-only files.
Device Service and Jeecg load the two textual secrets with typed configuration;
neither accepts JAAS material or the truststore password as a direct environment
value. Do not put credentials in Compose, Git, application YAML, command
history, or evidence. The checked-in Compose PLAINTEXT listener is
development-only on an internal Docker network.

Lifecycle messages contain only schema/event/batch IDs, opaque tenant/user/device
references, time windows, counts, quality summary, and persistence status.
Metric values, tokens, phone numbers, raw PPG/RRI, and model inference inputs are
forbidden by both publisher and consumer parsers.
