# Approved model artifact bundle

Mount the approved bundle read-only at this directory (or set
`REHEALTH_MODEL_ARTIFACT_DIR`). It must contain `model_manifest.json` and the
detached `model_manifest.sig`. The public verification key is supplied as a
runtime secret. Model Service is not started until the exact manifest bytes are
verified and their SHA-256 is written to the isolated verification volume.

Model binaries and signatures are release artifacts and are not committed here.
