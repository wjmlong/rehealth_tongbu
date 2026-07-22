# Code Quality Review: Pre-existing Dirty Root/LFS/Evidence Files

## Review Decision

- `codeQualityStatus`: **BLOCK**
- `recommendation`: **REQUEST_CHANGES**
- Scope reviewed: `.gitattributes`, `.gitignore`, `sync_to_github.sh`, `_lfs_commit*.sh`, `_lfs_diag.sh`, `_lfs_push.sh`, `_lfs_verify*.sh`, `outputs/demo_ui_live_api_integration_report.md`, and the six `outputs/*animation*.png` files.
- Current-state note: `.gitattributes`, `.gitignore`, and `sync_to_github.sh` are already in pushed HEAD `5b44b0d`, not dirty. The seven `_lfs_*.sh` files were untracked when reviewed, then disappeared from the shared worktree during this audit; no reviewer action removed them.
- ULW status lookup was unavailable because `omo` is not installed/on `PATH`; this report uses the required fallback evidence path.

## Findings

### CRITICAL

None established. Repository visibility could not be verified because `gh` is unavailable, but no token/JWT/key-shaped literal was found in the scoped text or PNG metadata.

### HIGH

1. **The sync script can silently commit and push unrelated/user/agent state.** `sync_to_github.sh:84` runs `git add -A`, then `sync_to_github.sh:134` commits everything and `sync_to_github.sh:146` pushes it. A read-only `git add --dry-run -A` showed it would currently include `.omo/boulder.json`, plan/draft/ledger/QA artifacts, `.workbuddy` state, the modified live `.codegraph` database, the report, and all six PNGs. This violates scope control and can publish internal state or future secrets.

2. **Dry-run and helper verification destroy the caller's staged selection.** `sync_to_github.sh:103`, `:118`, and `:127` use repository-wide `git reset -q`. `_lfs_commit.sh:9`, `_lfs_commit2.sh:9`, `_lfs_verify.sh:6,49`, and `_lfs_verify2.sh:6,40` do the same. A dry run or diagnostic therefore mutates the index and discards intentional staging. `_lfs_commit.sh:10` additionally stages the entire repository before committing it.

3. **LFS enforcement is bypassed permanently when the required client is absent.** `sync_to_github.sh:140-142` renames the LFS `pre-push` hook, never restores it, and continues to push. That can publish pointer commits without their corresponding LFS objects and leave later pushes unprotected. The repository already contains multiple `*.disabled-lfs` hook remnants, consistent with persistent mutation.

4. **The committed LFS rules deliberately force-track 1.37 GB of ignored, generated, and locally mutable artifacts.** `.gitattributes:1-5` tracks an emulator archive, an Android SDK download, a virtualenv CatBoost binary, a built backend JAR, and `.codegraph/codegraph.db`; `.gitignore:6-10` explicitly classifies those locations/types as caches/build/download products. `sync_to_github.sh:87-97` overrides the ignores with `git add -f`. The codegraph DB is currently locked, has a 16 MB ignored WAL and 32 KB SHM sidecar, and differs from its committed LFS object; committing only the DB can produce an inconsistent SQLite snapshot and continual 210 MB churn. The JAR redundantly embeds source-tracked configuration, while the SDK downloads and venv binary add provenance/licensing and supply-chain review burden. `git lfs fsck` passes technically, but these files should not be versioned.

5. **The screenshot set is misleading evidence for the report's claimed animation/live-risk result.** `outputs/demo_ui_live_api_integration_report.md:189` names all six PNGs as animation evidence. `attribution-animation-start.png` is the Data screen, while the mid/end frames are the Attribution screen, so they are not frames from one animation. The PIAS files change scroll position/content rather than isolating an animation, and `pias-animation-start.png` visibly reports current risk `50.1%` while the report at `:185` and `:187` claims the live result is `2.38%`/about `2.4%`. This evidence bundle must be recaptured or relabeled before it can support the success claim.

### MEDIUM

1. **The temporary LFS helpers are duplicated, brittle one-offs rather than maintainable tooling.** `_lfs_commit.sh`/`_lfs_commit2.sh` and `_lfs_verify.sh`/`_lfs_verify2.sh` duplicate behavior, hard-code `/mnt/d/rehealthAI`, derive whitespace-split paths from `.gitattributes`, and lack regression coverage. `_lfs_commit*.sh:6` also deletes `.git/index.lock` without checking for a live Git process, which can corrupt concurrent Git operations.

2. **The diagnostic helper contains an unsupported command.** `_lfs_diag.sh:53-54` runs `git lfs ls-files --others`; the installed Git LFS reports `unknown flag: --others` and exits 127. The script then labels the failure as `(none / error)`, conflating a broken check with an empty result.

3. **The push helper can report success after a failed push.** `_lfs_push.sh:6` runs a hard-coded push to `codex/real-device`, `_lfs_push.sh:7` records its status, then `_lfs_push.sh:8` executes a successful `echo`; callers receive exit 0 even when the push failed. It also writes `/tmp/push_out.txt` outside repository ignore/retention controls and mutates local Git HTTP configuration.

4. **The report is safe from direct secret leakage but overstates independent verification.** The referenced demo branch and commits (`48bf5af`, `b666b76`, `c370b12`) and both APKs exist, and the report clearly labels replay history and physical-device gaps. However, this audit did not rerun its claimed 133-task build/E2E suite, and the supplied PNG evidence is inconsistent. Treat the report as conditional until coherent artifacts are attached.

### LOW

1. The six PNGs contain no PNG text/EXIF chunks and no visible account name, phone, token, device address, or other direct identifier. They do show health-like readings and model factors (blood pressure, age, smoking, diabetes/hypertension history). The report identifies these as capture/replay validation rather than a person's current measurements, so direct PII/PHI leakage was not established; still keep them in restricted QA evidence if provenance cannot be proven synthetic/replayed.

2. `sync_to_github.log` and `sync_debug.log` are ignored by `.gitignore:16-19`; the existing sync log contains Git/network output but no detected token. Generated `*.zip`, `**/build/`, `**/target/`, and `**/.venv/` paths are normally ignored, but the exact LFS paths bypass that protection.

## Commit Allowlist

### Safe as content, but already committed/pushed

- `.gitignore`: no secret found; its generated/log ignore rules are appropriate. It should be extended before any broad sync workflow is used, but there is no current dirty change to commit.

### Conditionally safe after evidence correction

- `outputs/demo_ui_live_api_integration_report.md`: no token, account identifier, phone number, or raw PII found. Commit only after replacing/removing the inconsistent animation references and recording reproducible artifact paths/results.
- `outputs/attribution-animation-mid.png`
- `outputs/attribution-animation-end.png`
- `outputs/pias-animation-mid.png`
- `outputs/pias-animation-end.png`

The four images above have no metadata/visible identifiers, but should be committed only as individually labeled screenshots, not as a coherent start/mid/end animation proof.

## Reject / Do Not Commit

- `sync_to_github.sh`: unsafe broad staging/reset, automatic commit/push/rebase, and persistent LFS-hook bypass. Already pushed at HEAD; requires a corrective commit before reuse.
- `.gitattributes`: current rules version generated/cache artifacts and conflict with `.gitignore`. Already pushed at HEAD; requires a corrective commit removing these LFS paths and appropriate history/storage remediation if desired.
- `_lfs_commit.sh`, `_lfs_commit2.sh`, `_lfs_diag.sh`, `_lfs_push.sh`, `_lfs_verify.sh`, `_lfs_verify2.sh`: unsafe/broken temporary helpers; they disappeared during review and should remain absent.
- `outputs/attribution-animation-start.png`: wrong screen for the claimed attribution animation sequence.
- `outputs/pias-animation-start.png`: visible 50.1% value contradicts the reported 2.4% live result and can mislead medical-risk QA.
- The five LFS payloads referenced by `.gitattributes`: `Android-apk/build/emulator-36.4.10.zip`, `model-service/.venv/Lib/site-packages/catboost/_catboost.pyd`, `backend/jeecg-boot/jeecg-module-system/jeecg-system-start/target/jeecg-system-start-3.9.2.jar`, `.codegraph/codegraph.db`, and `tools/commandlinetools-win_latest.zip`.
- Any `.omo/**`, `.workbuddy/**`, `.codegraph/**`, build logs, APKs, archives, or other unrelated files selected by `git add -A` unless explicitly reviewed and allowlisted.

## Skill-Perspective Check

- `remove-ai-slops`: **ran**. Violations found: duplicate one-off helpers, unsupported diagnostics, needless hard-coded automation, broad/destructive index manipulation, and evidence artifacts that create false confidence.
- `programming`: **ran** at the applicable shell/tooling level. Violations found: implementation-mirroring/brittle checks, destructive global side effects, unsupported command use, missing failure propagation, and validation that treats an LFS pointer check as sufficient while bypassing repository/provenance boundaries. No Python/Rust/TypeScript/Go file was in scope, so no language-specific reference was applicable.

## Verification Performed

- Full scoped file reads with line numbers; `git diff`, history, status, remote/branch checks.
- `bash -n`: PASS for all seven helper scripts while they existed.
- Secret/token and identifier scans: no token/JWT/key-shaped literal in scoped text; no PNG text metadata.
- Visual inspection of all six PNGs at original resolution.
- Git LFS: five objects listed; `git lfs fsck` PASS; attributes resolve to LFS.
- Dry-run staging: confirmed unrelated `.omo`, `.workbuddy`, `.codegraph`, report, and screenshot selection.
- Archive/JAR listings: confirmed SDK/emulator downloads and embedded application configs.
- No build/test rerun: production Android feature files were explicitly out of scope, and this review was read-only.

## Blockers Before Approval

1. Replace broad `git add -A`/global `git reset` automation with an explicit allowlist that preserves the existing index and excludes agent/cache state.
2. Never disable the LFS hook to push; fail closed when Git LFS is unavailable.
3. Stop tracking the five generated/cache LFS payloads and define an approved artifact distribution strategy outside source history.
4. Keep the `_lfs_*.sh` scratch helpers out of Git or replace them with one tested, non-destructive tool.
5. Recapture/relabel the six screenshot artifacts so every claimed sequence is coherent and the displayed risk agrees with the report, then update the report accordingly.
