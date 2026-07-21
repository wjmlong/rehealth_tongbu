# P0b Push Retry Instructions

Date: 2026-07-16
Task: Retry pushing P0b Android canonical risk UI path to remote

## Background

P0b has been completed and committed locally:
- **Commit**: `02abe70` feat(android): wire canonical risk UI path
- **Branch**: `work/P0b_android_canonical_risk_ui_path`
- **Validation**: All tests passed
- **Local status**: Clean (at commit time)

Previous push attempts failed with GitHub connection reset ×2.

## Prerequisites

- Working directory: `D:\rehealthAI\Android-apk`
- Ensure no other git operations are running
- Ensure working tree is clean or stash changes

## Commands

### 1. Navigate and verify

```powershell
cd D:\rehealthAI\Android-apk
git branch --show-current
```

Expected: `work/P0b_android_canonical_risk_ui_path`

If on different branch:
```powershell
git stash save "temp: save before P0b push"
git checkout work/P0b_android_canonical_risk_ui_path
```

### 2. Verify commit

```powershell
git log --oneline --decorate -3
git status --short --branch
```

Expected:
- HEAD at `02abe70` feat(android): wire canonical risk UI path
- Working tree clean

### 3. Retry push

```powershell
git push -u origin work/P0b_android_canonical_risk_ui_path --verbose
```

### 4. If push succeeds

```powershell
git status --short --branch
git ls-remote --heads origin work/P0b_android_canonical_risk_ui_path
```

Document success:
- Create `codex-runs/2026-07-16/P0b_push_success.md`
- Record timestamp and remote SHA

### 5. If push fails again

Create patch archive:
```powershell
git format-patch -1 02abe70 -o .\codex-runs\2026-07-16\
```

Document failure:
- Create `codex-runs/2026-07-16/P0b_push_failed.md`
- Recommend: manual zip upload or network troubleshooting

## Success Criteria

- Remote branch exists at `origin/work/P0b_android_canonical_risk_ui_path`
- Remote SHA matches local `02abe70`
- Status shows tracking remote branch

## Fallback

If network issue persists:
1. Create patch: `git format-patch -1 02abe70`
2. Create branch archive: `git archive -o P0b_android_canonical_risk_ui_path.zip work/P0b_android_canonical_risk_ui_path`
3. Manual upload or defer to later network window
