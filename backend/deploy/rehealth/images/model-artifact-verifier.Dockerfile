FROM ghcr.io/sigstore/cosign/cosign:v2.5.0@sha256:e82eb6d42ccb6bc048d8d9e5e598e4d5178e1af6c00e54e02c9b0569c5f3ec11 AS cosign
FROM alpine:3.22.1@sha256:4bcff63911fcb4448bd4fdacec207030997caf25e9bea4045fa6c8c44de311d1

RUN apk add --no-cache jq
COPY --from=cosign /ko-app/cosign /usr/local/bin/cosign
COPY deploy/rehealth/scripts/verify-model-artifact.sh /opt/rehealth/verify-model-artifact.sh
RUN chmod 0555 /opt/rehealth/verify-model-artifact.sh
