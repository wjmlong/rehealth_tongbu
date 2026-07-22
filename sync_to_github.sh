#!/usr/bin/env bash
#
# sync_to_github.sh
# 定期把 D:\rehealthAI 同步到 GitHub 仓库 wjmlong/rehealth_tongbu。
# 在 WSL2 内运行：使用 gh CLI 做鉴权（gh auth setup-git 让 git 走 gh 的 token），
# 用 git 执行 add / commit / push。对 WSL 下 git 偶发的 TLS 中断做了重试与规避。
#
# 用法：
#   bash sync_to_github.sh            # 正常同步（提交并推送）
#   bash sync_to_github.sh --dry-run  # 只暂存+检查，不提交、不推送（用于验证）
#
set -uo pipefail

# 确保 git-lfs 等在 PATH 中（非交互/计划任务环境可能 PATH 较精简）
export PATH="/usr/local/bin:$PATH"

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

# ---------------- 网络命令重试（应对 WSL GnuTLS 偶发 TLS 中断） ----------------
git_retry() {
  local n=1 max=4 delay=10
  while [ "$n" -le "$max" ]; do
    log "[try $n/$max] git $*"
    if git "$@" >>"$LOG_FILE" 2>&1; then return 0; fi
    log "网络命令失败（第 $n 次），${delay}s 后重试..."
    sleep "$delay"
    n=$((n+1))
  done
  return 1
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

# 规避 WSL 下 git/GnuTLS 偶发的 TLS 连接中断（HTTP/1.1 更稳定）
git config http.version HTTP/1.1 2>/dev/null || true

# ---------------- 3. 确保 remote ----------------
if ! git remote get-url origin >/dev/null 2>&1; then
  git remote add origin "$REMOTE_URL"
fi

# ---------------- 4. 仅在远端分支已存在时 fetch（带超时，避免首推无意义地挂起） ----------------
if git ls-remote --heads origin "$BRANCH" >/dev/null 2>&1; then
  timeout 120 git fetch origin "$BRANCH" >>"$LOG_FILE" 2>&1 || log "fetch 失败（忽略，将继续推送）"
else
  log "远端尚无 $BRANCH 分支，跳过 fetch"
fi

# ---------------- 5. 暂存变更 ----------------
git add -A

# 强制纳入所有命中 Git LFS 规则的文件（即使被 .gitignore 忽略）。
# 注意：不能用 `git lfs ls-files --others`（该参数不存在，且该命令基于
# `git ls-files --others`，会自动跳过被忽略的文件，导致大文件永远不被暂存）。
# 正确做法：直接从 .gitattributes 解析 filter=lfs 的路径，用 git add -f 强制暂存，
# git-lfs 的 clean 过滤器会把实际内容替换为指针（pointer）对象。
if [ -f .gitattributes ]; then
  while IFS= read -r lfs_path; do
    [ -z "$lfs_path" ] && continue
    if [ -f "$lfs_path" ]; then
      if git add -f -- "$lfs_path" >/dev/null 2>&1; then
        log "已强制暂存 LFS 文件: $lfs_path"
      else
        log "WARN: 无法暂存 LFS 文件 $lfs_path"
      fi
    fi
  done < <(grep -E 'filter=lfs' .gitattributes 2>/dev/null | awk '{print $1}')
fi

if git diff --cached --quiet; then
  log "没有需要同步的变更，结束。"
  rotate_log
  [ "$DRY_RUN" = "1" ] && git reset -q
  exit 0
fi

# ---------------- 6. 大文件保护（GitHub 单文件 100MB 上限） ----------------
BIG=$(git diff --cached --diff-filter=AM --name-only | while read -r f; do
  # 已标记为 Git LFS 跟踪的大文件由 git-lfs 处理，直接放行
  if git check-attr filter -- "$f" 2>/dev/null | grep -q "filter: lfs"; then continue; fi
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

# ---------------- 8.5 防御：LFS 钩子缺失时临时禁用，避免推送被阻断 ----------------
if [ -f .git/hooks/pre-push ] && ! command -v git-lfs >/dev/null 2>&1 && grep -q "git-lfs" .git/hooks/pre-push 2>/dev/null; then
  mv .git/hooks/pre-push .git/hooks/pre-push.disabled-lfs
  log "已临时禁用缺失 git-lfs 的 pre-push 钩子（如需 LFS 请先在 WSL 安装 git-lfs）"
fi

# ---------------- 9. 推送（含重试与 rebase 兜底） ----------------
if git_retry push -u origin "$BRANCH"; then
  log "OK: 已推送 $BRANCH"
else
  log "fast-forward 失败，尝试 rebase 后推送..."
  if git_retry pull --rebase origin "$BRANCH" && git_retry push -u origin "$BRANCH"; then
    log "OK: rebase 后已推送 $BRANCH"
  else
    log "ERROR: 推送失败，请手动检查冲突或网络"
    exit 1
  fi
fi

rotate_log
log "=== sync done ==="
