# D3 集成快速参考

## 核心代码片段

### 登录钩子
```kotlin
// After successful login
app.sessionStore.token = response.token
app.sessionStore.userId = response.userInfo.id
app.authenticatedApiClient.onLoginSuccess(response.token)
app.syncRepository.resumeQueue()
MeasurementSyncWorker.schedule(context)
MeasurementSyncWorker.triggerImmediate(context)
```

### 登出钩子
```kotlin
// Before logout
MeasurementSyncWorker.cancel(context)
app.authenticatedApiClient.onLogout()
app.syncRepository.pauseQueue()
```

### 提交反馈
```kotlin
// Replace submitCheckIn with:
interventionFeedbackRepo.submitFeedback(
    interventionId = interventionId,
    status = "completed",  // or "not_applicable", "skipped"
    note = null
)
MeasurementSyncWorker.triggerImmediate(context)
```

### Banner集成
```kotlin
val queueState by app.syncRepository.queueState.collectAsState()
val pendingFeedback by app.interventionFeedbackRepository
    .observePendingFeedback()
    .collectAsState(initial = emptyList())

QueueStatusBanner(
    queueState = queueState,
    pendingCount = pendingFeedback.size,
    onLoginClick = { navController.navigate("login") }
)
```

### Worker初始化
```kotlin
// In ReHealthApplication.onCreate()
if (sessionStore.isLoggedIn) {
    MeasurementSyncWorker.schedule(this)
}
```

## 测试命令

```powershell
# 编译
.\gradlew.bat assembleDebug

# 安装
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 查看日志
adb logcat | findstr "MeasurementSyncWorker"
adb logcat | findstr "InterventionFeedback"
adb logcat | findstr "AuthenticatedApiClient"

# 检查数据库
adb shell run-as com.rehealth.genie
cd databases
sqlite3 rehealth-local.db
SELECT * FROM intervention_feedback_queue;
```

## 6个任务检查清单

- [ ] 任务1: 登录流程 (2h)
- [ ] 任务2: 登出流程 (1h)
- [ ] 任务3: 替换submitCheckIn (2h)
- [ ] 任务4: 添加Banner (1h)
- [ ] 任务5: Worker初始化 (0.5h)
- [ ] 任务6: 设备测试 (2h)

## 关键文件

- `LoginViewModel.kt` - 新建
- `LoginScreen.kt` - 修改
- `ProfileScreen.kt` - 新建
- `InterventionFeedbackViewModel.kt` - 新建
- `ReHealthApp.kt` - 修改
- `RingViewModel.kt` - 删除submitCheckIn
- `ReHealthApplication.kt` - 添加worker调度

## 常见问题

**Q: 编译错误 Unresolved reference?**
A: 运行 `.\gradlew.bat clean`

**Q: Worker不运行?**
A: 检查网络、电池、权限

**Q: 401不触发暂停?**
A: 检查AuthenticatedApiClient日志

**Q: Banner不显示?**
A: 检查collectAsState订阅

## 回滚
```powershell
git checkout backup/pre-d3-ui-integration
# 或
git stash pop
```
