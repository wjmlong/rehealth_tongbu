#!/usr/bin/env bash
#
# sync_to_github.sh
# 定期把 D:\rehealthAI 同步到 GitHub 仓库 wjmlong/rehealth_tongbu。
# 在 WSL2 内运行：使用 gh CLI 做鉴权（gh auth setup-git 让 git 走 gh 的 token），
# 用 git 执行 add / commit / push。
#
# 用法：
#   bash sync_to_github.sh            # 正常同步（提交并推送）
#   bash sync_to_github.sh --dry-run  # 只暂存+检查，不提交、不推送（用于验证）
#
set -uo pipefail

# ---------------- 配置 ----------------
LOCAL_DIR="/mnt/d/rehealthAI"
REMOTE_URL="https://github.com/wjmlong/rehealth_tongbu.git"
LOG_FILE="${LOCAL_DIR}/sync_to_github.log"
MAX_LOG_LINES=2000
# 默认同步“当前所在分支”，跟随你正在工作的分支。
# 如需固定分支，改成例如：BRANCH="main"
BRANCH="${BRANCH:-$(git -C "$LOCAL_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo main)}"
DRY_RUN=0
[ "${1:-}" = "--dry-run" ] && DRY_RUN=1
[ "${DRY_RUN_ENV:-0}" = "1" ] && DRY_RUN=1

# ---------------- 日志 ----------------
log() {
  local ts; ts="$(date '+%Y-%m-%d %H:%M:%S')"
  echo "[$ts] $*" | tee -a "$LOG_FILE"
}
rotate_log() {
  if [ -f "$LOG_FILE" ] && [ "$(wc -l < "$LOG_FILE")" -gt "$MAX_LOG_LINES" ]; then
    tail -n "$MAX_LOG_LINES" "$LOG_FILE" > "${LOG_FILE}.tmp" && mv "${LOG_FILE}.tmp" "$LOG_FILE"
  fi
}

log "=== sync start (branch=$BRANCH, dry_run=$DRY_RUN) ==="

# ---------------- 1. 检查 gh ----------------
if ! command -v gh >/dev/null 2>&1; then
  log "ERROR: WSL2 中找不到 gh CLI，请先安装并在 WSL2 内执行 'gh auth login'"
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  log "ERROR: gh 未登录，请在 WSL2 中执行 'gh auth login'"
  exit 1
fi
# 让 git 通过 gh 的 token 进行 HTTPS 鉴权（幂等）
gh auth setup-git >/dev/null 2>&1 || true

# ---------------- 2. 进入仓库 ----------------
if [ ! -d "${LOCAL_DIR}/.git" ]; then
  log "ERROR: ${LOCAL_DIR} 不是 git 仓库"
  exit 1
fi
cd "$LOCAL_DIR" || exit 1

# ---------------- 3. 确保 remote ----------------
if ! git remote get-url origin >/dev/null 2>&1; then
  git remote add origin "$REMOTE_URL"
fi

# ---------------- 4. 拉取远端（rebase 友好） ----------------
git fetch origin "$BRANCH" 2>&1 | tee -a "$LOG_FILE" || true

# ---------------- 5. 暂存变更 ----------------
git add -A

if git diff --cached --quiet; then
  log "没有需要同步的变更，结束。"
  rotate_log
  [ "$DRY_RUN" = "1" ] && git reset -q
  exit 0
fi

# ---------------- 6. 大文件保护（GitHub 单文件 100MB 上限） ----------------
BIG=$(git diff --cached --diff-filter=AM --name-only | while read -r f; do
  if [ -f "$f" ]; then
    sz=$(stat -c%s "$f" 2>/dev/null || echo 0)
    if [ "$sz" -gt 104857600 ]; then echo "$f ($((sz/1024/1024))MB)"; fi
  fi
done)
if [ -n "$BIG" ]; then
  log "ERROR: 发现超过 100MB 的文件，已中止推送（请加入 .gitignore 或改用 Git LFS）："
  log "$BIG"
  [ "$DRY_RUN" = "1" ] && git reset -q
  exit 1
fi

# ---------------- 7. dry-run 摘要 ----------------
if [ "$DRY_RUN" = "1" ]; then
  n=$(git diff --cached --name-only | wc -l)
  log "[dry-run] 将要同步的文件数: $n"
  log "[dry-run] 未执行 commit / push。取消暂存并退出。"
  git reset -q
  rotate_log
  exit 0
fi

# ---------------- 8. 提交 ----------------
MSG="sync: $(date '+%Y-%m-%d %H:%M:%S')"
if ! git commit -q -m "$MSG" >>"$LOG_FILE" 2>&1; then
  log "ERROR: commit 失败，请检查 git 配置(user.name/email)或 pre-commit hook"
  exit 1
fi

# ---------------- 9. 推送（含 rebase 兜底） ----------------
if git push -u origin "$BRANCH" >>"$LOG_FILE" 2>&1; then
  log "OK: 已推送 $BRANCH"
else
  log "fast-forward 失败，尝试 rebase 后推送..."
  if git pull --rebase origin "$BRANCH" >>"$LOG_FILE" 2>&1 && \
     git push -u origin "$BRANCH" >>"$LOG_FILE" 2>&1; then
    log "OK: rebase 后已推送 $BRANCH"
  else
    log "ERROR: 推送失败，请手动检查冲突"
    exit 1
  fi
fi

rotate_log
log "=== sync done ==="
