# Runtime secrets

Create the files declared by `docker-compose.yml` immediately before deployment.
This directory is ignored except for this document. Never commit secret values.
Production secret files must be supplied by the deployment secret manager with
owner-only permissions and rotated independently of the image release.

Required filenames are the keys under the Compose top-level `secrets` section.
`vision_provider_credential` is the server-only key used for photo food/OCR analysis;
it must never be copied into Android configuration or logs.

Alibaba Cloud Phone Number Verification Service uses two dedicated server-only files:

- `aliyun_sms_access_key_id`
- `aliyun_sms_access_key_secret`

Each file contains exactly one credential value with no quotes. Use a dedicated RAM
user allowed only to call `dypns:SendSmsVerifyCode` and `dypns:CheckSmsVerifyCode`;
never reuse the OSS credential or copy either value into
`.env`, Android configuration, logs, or tracked YAML.
