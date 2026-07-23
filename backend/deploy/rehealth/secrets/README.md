# Runtime secrets

Create the files declared by `docker-compose.yml` immediately before deployment.
This directory is ignored except for this document. Never commit secret values.
Production secret files must be supplied by the deployment secret manager with
owner-only permissions and rotated independently of the image release.

Required filenames are the keys under the Compose top-level `secrets` section.
