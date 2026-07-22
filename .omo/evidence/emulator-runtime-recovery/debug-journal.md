# Emulator Runtime Recovery Journal
Started: 2026-07-23
Goal: Recover emulator system_server without wiping AVD data, then install final APK and verify attribution screen.

## Hypotheses
1. [OPEN] system_server transiently wedged; distinguishing evidence: services return after adb reboot; fix: graceful reboot.
2. [OPEN] emulator process/graphics state is wedged; distinguishing evidence: reboot fails and same-AVD restart restores services; fix: process restart.
3. [OPEN] AVD persistent state is damaged; distinguishing evidence: same-AVD restart still lacks package/activity, while fresh API31 AVD works; fix: temporary AVD.

## Artifacts to revert
- [ ] Emulator reboot/restart operations; preserve original AVD data and do not wipe.
- [ ] Runtime evidence files in this directory; remove after handoff only if requested.

## Findings
- Initial ADB/device, service list, and process snapshots are in `01-get-state.txt` through `09-emulator-process.txt` when present.
