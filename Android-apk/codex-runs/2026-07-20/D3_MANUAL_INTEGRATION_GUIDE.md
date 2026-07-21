# D3 Android Auth + Typed Feedback - 详细手动集成实施指南

Date: 2026-07-20  
Estimated Time: 6-8 hours  
Prerequisites: Android Studio, JDK, Android device/emulator  
Branch: work/D3_android_auth_typed_feedback

---

## 概述

本指南提供D3 UI集成的**逐步实施说明**，包含所有代码、测试步骤和回滚方案。

**D3基础设施已完成** (4 commits, 16 files, +2,008 lines):
- ✅ AuthenticatedApiClient (401检测)
- ✅ InterventionFeedbackRepository (类型化反馈队列)
- ✅ SyncRepository (队列暂停/恢复)
- ✅ MeasurementSyncWorker (周期性上传)
- ✅ QueueStatusBanner (UI组件)
- ✅ ReHealthApplication (依赖注入)

**需要集成到UI**:
1. 登录流程集成 (2小时)
2. 登出流程集成 (1小时)
3. 替换submitCheckIn (2小时)
4. 添加QueueStatusBanner (1小时)
5. Worker初始化 (0.5小时)
6. 设备测试 (2小时)

---

## 前置准备

### 1. 环境验证

```powershell
# 切换到Android项目目录
cd D:\rehealthAI\Android-apk

# 切换到D3分支
git checkout work/D3_android_auth_typed_feedback

# 验证分支状态
git log --oneline -5
# 应该看到:
# ce4cde5 docs(android): D3 final status and summary
# f40f630 feat(android): D3 worker - periodic feedback sync
# 1e8dbac feat(android): D3 UI integration - dependencies and banner component
# 67f77df feat(android): D3 auth-aware upload queue and typed feedback

# 验证构建环境
.\gradlew.bat --version

# 清理构建
.\gradlew.bat clean
```

### 2. 备份当前代码

```powershell
# 创建备份分支
git branch backup/pre-d3-ui-integration

# 或创建备份文件
git stash push -m "Pre-D3 UI integration backup"
```

### 3. 了解现有文件结构

```
app/src/main/java/com/rehealth/genie/
├── ReHealthApplication.kt          # 已更新：D3依赖已注入
├── ui/
│   ├── LoginScreen.kt              # 需修改：添加登录钩子
│   ├── ReHealthApp.kt              # 需修改：添加banner，替换submitCheckIn
│   └── components/
│       └── QueueStatusBanner.kt    # 已创建：队列状态横幅
├── ring/
│   └── RingViewModel.kt            # 需修改：删除submitCheckIn方法
├── network/
│   ├── AuthenticatedApiClient.kt   # 已创建：401检测
│   └── SessionStore.kt             # 已创建：加密token存储
└── work/
    └── MeasurementSyncWorker.kt    # 已创建：周期性上传
```

---

## 任务1: 登录流程集成 (2小时)

### 目标
在成功登录后调用D3认证钩子，启动队列和worker。

### 现状分析

查看 `app/src/main/java/com/rehealth/genie/ui/LoginScreen.kt`:

现在LoginScreen使用模拟验证：
```kotlin
// 当前代码（第67-100行左右）
@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    // ... 模拟验证码逻辑
    
    Button(
        onClick = {
            if (canLogin) {
                onLogin(phone)  // 只传递手机号
            }
        }
    ) {
        Text("登录")
    }
}
```

### 步骤1.1: 实现真实JeecgBoot登录API

**文件**: `app/src/main/java/com/rehealth/genie/network/dto/AuthDto.kt`

如果文件已存在，检查是否有以下DTOs，否则添加：

```kotlin
package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MobileLoginRequest(
    val mobile: String,
    val captcha: String,
)

@JsonClass(generateAdapter = true)
data class MobileLoginResponse(
    val token: String,
    val userInfo: UserInfo,
)

@JsonClass(generateAdapter = true)
data class UserInfo(
    val id: String,
    val username: String?,
    val realname: String?,
)
```

### 步骤1.2: 添加登录API到ReHealthApi

**文件**: `app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt`

在接口中添加登录端点：

```kotlin
interface ReHealthApi {
    // ... 现有方法
    
    @POST("/jeecg-boot/sys/mLogin")
    suspend fun mobileLogin(@Body request: MobileLoginRequest): Response<JeecgResult<MobileLoginResponse>>
}
```

### 步骤1.3: 添加登录方法到AuthenticatedApiClient

**文件**: `app/src/main/java/com/rehealth/genie/network/AuthenticatedApiClient.kt`

在类中添加登录方法（注意：登录不需要token）：

```kotlin
class AuthenticatedApiClient(
    // ... 现有代码
) {
    // ... 现有方法
    
    /**
     * Mobile login (no auth required).
     */
    suspend fun mobileLogin(mobile: String, captcha: String): ApiResult<MobileLoginResponse> {
        // 直接调用API，不使用authenticatedClient
        val api = Retrofit.Builder()
            .baseUrl(BackendConfig.normalizeBaseUrl(baseUrl) + "/")
            .client(httpClient)  // 不带AuthInterceptor
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ReHealthApi::class.java)
            
        return executeWithoutAuth {
            api.mobileLogin(MobileLoginRequest(mobile, captcha))
        }
    }
    
    private suspend fun <T> executeWithoutAuth(
        block: suspend () -> Response<JeecgResult<T>>,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val response = block()
            if (!response.isSuccessful) {
                return@withContext ApiResult.NetworkError("HTTP ${response.code()}")
            }
            val envelope = response.body() ?: return@withContext ApiResult.InvalidResponse("Empty body")
            if (envelope.success != true) {
                return@withContext ApiResult.InvalidRequest(envelope.message ?: "Login failed")
            }
            val data = envelope.result ?: return@withContext ApiResult.InvalidResponse("No result")
            ApiResult.Success(data)
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Network error")
        }
    }
}
```

### 步骤1.4: 创建LoginViewModel

**文件**: `app/src/main/java/com/rehealth/genie/ui/LoginViewModel.kt` (新建)

```kotlin
package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.work.MeasurementSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
)

class LoginViewModel(
    private val context: Context,
) : ViewModel() {
    
    private val app = context.applicationContext as ReHealthApplication
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun login(mobile: String, captcha: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            
            when (val result = app.authenticatedApiClient.mobileLogin(mobile, captcha)) {
                is ApiResult.Success -> {
                    val response = result.data
                    
                    // D3: 保存token和用户信息
                    app.sessionStore.token = response.token
                    app.sessionStore.userId = response.userInfo.id
                    app.sessionStore.username = response.userInfo.username
                    
                    // D3: 通知认证客户端登录成功
                    app.authenticatedApiClient.onLoginSuccess(response.token)
                    
                    // D3: 恢复队列
                    app.syncRepository.resumeQueue()
                    
                    // D3: 调度worker
                    MeasurementSyncWorker.schedule(context)
                    
                    // D3: 立即触发同步（上传待处理的反馈）
                    MeasurementSyncWorker.triggerImmediate(context)
                    
                    _uiState.value = LoginUiState(isLoggedIn = true)
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = LoginUiState(errorMessage = "网络错误: ${result.message}")
                }
                is ApiResult.InvalidRequest -> {
                    _uiState.value = LoginUiState(errorMessage = "登录失败: ${result.message}")
                }
                is ApiResult.InvalidResponse -> {
                    _uiState.value = LoginUiState(errorMessage = "响应格式错误")
                }
                else -> {
                    _uiState.value = LoginUiState(errorMessage = "登录失败，请重试")
                }
            }
        }
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(context) as T
        }
    }
}
```

### 步骤1.5: 更新LoginScreen使用ViewModel

**文件**: `app/src/main/java/com/rehealth/genie/ui/LoginScreen.kt`

修改LoginScreen composable：

```kotlin
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.Factory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    // 登录成功时导航
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }
    
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var codeSent by remember { mutableStateOf(false) }
    
    val phoneValid = phone.length == 11 && phone.all(Char::isDigit)
    val canLogin = phoneValid && code.length >= 4 && agreed && !uiState.isLoading
    
    // ... 现有UI代码
    
    // 修改登录按钮
    Button(
        onClick = {
            if (canLogin) {
                viewModel.login(phone, code)
            }
        },
        enabled = canLogin,
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
            )
        } else {
            Text("登录")
        }
    }
    
    // 显示错误信息
    uiState.errorMessage?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
```

### 测试步骤1

```powershell
# 1. 编译
.\gradlew.bat assembleDebug

# 2. 安装到设备
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 3. 测试登录流程
# - 输入手机号和验证码
# - 点击登录
# - 检查logcat:
adb logcat | findstr "MeasurementSyncWorker"
# 应该看到: "MeasurementSyncWorker scheduled"

# 4. 验证SessionStore
# - 登录后，token应该已保存
# - 队列应该恢复（queueState = Active）
```

### 回滚方案1

如果登录集成失败：

```powershell
# 恢复到集成前
git checkout backup/pre-d3-ui-integration

# 或
git stash pop
```

---

## 任务2: 登出流程集成 (1小时)

### 目标
在登出时取消worker并暂停队列。

### 步骤2.1: 检查是否有ProfileScreen/SettingsScreen

```powershell
# 查找现有的profile/settings屏幕
find app/src -name "*Profile*" -o -name "*Settings*"
```

如果不存在，需要先创建一个简单的ProfileScreen。

### 步骤2.2: 创建ProfileScreen（如果不存在）

**文件**: `app/src/main/java/com/rehealth/genie/ui/ProfileScreen.kt` (新建)

```kotlin
package com.rehealth.genie.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.work.MeasurementSyncWorker

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ReHealthApplication
    val username by remember { mutableStateOf(app.sessionStore.username ?: "用户") }
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "欢迎, $username",
            style = MaterialTheme.typography.headlineMedium,
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { showLogoutDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("退出登录")
        }
    }
    
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认退出") },
            text = { Text("退出后需要重新登录才能同步反馈数据") },
            confirmButton = {
                TextButton(
                    onClick = {
                        performLogout(context)
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("确认退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun performLogout(context: Context) {
    val app = context.applicationContext as ReHealthApplication
    
    // D3: 取消worker
    MeasurementSyncWorker.cancel(context)
    
    // D3: 登出认证客户端
    app.authenticatedApiClient.onLogout()
    
    // D3: 暂停队列
    app.syncRepository.pauseQueue()
}
```

### 步骤2.3: 将ProfileScreen添加到导航

在ReHealthApp.kt中添加ProfileScreen到导航（具体位置取决于现有导航结构）。

### 测试步骤2

```powershell
# 1. 编译和安装
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 2. 测试登出流程
# - 登录应用
# - 进入Profile页面
# - 点击"退出登录"
# - 检查logcat:
adb logcat | findstr "MeasurementSyncWorker"
# 应该看到: "MeasurementSyncWorker cancelled"

# 3. 验证队列状态
# - 登出后，queueState应该是Paused
# - SessionStore应该清空（token = null）
```

### 回滚方案2

如果登出集成有问题，可以临时禁用：

```kotlin
// 在ProfileScreen.kt中注释掉D3调用
private fun performLogout(context: Context) {
    val app = context.applicationContext as ReHealthApplication
    // MeasurementSyncWorker.cancel(context)  // 临时禁用
    // app.authenticatedApiClient.onLogout()  // 临时禁用
    // app.syncRepository.pauseQueue()  // 临时禁用
}
```

---

## 任务3: 替换submitCheckIn (2小时)

### 目标
用类型化反馈替换遗留的submitCheckIn调用。

### 现状分析

**文件**: `app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt`

当前代码（第301-321行）：

```kotlin
fun submitCheckIn(itemId: String) {
    val client = backendClient
    if (client == null) {
        addLocalCheckIn(itemId)
        return
    }
    viewModelScope.launch {
        mutableUiState.update { it.copy(message = "正在提交干预打卡") }
        client.submitCheckIn(itemId = itemId)
            .onSuccess {
                refreshPatientMvp(silent = true)
                mutableUiState.update { state ->
                    state.copy(message = "打卡完成，干预反馈已记录")
                }
            }
            .onFailure { error ->
                Log.w(TAG, "check-in failed", error)
                addLocalCheckIn(itemId)
            }
    }
}
```

### 步骤3.1: 在RingViewModel中删除submitCheckIn方法

**文件**: `app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt`

找到并**完全删除**以下方法：
- `submitCheckIn(itemId: String)` (第301-321行)
- `addLocalCheckIn(itemId: String)` (如果存在)

### 步骤3.2: 创建InterventionFeedbackViewModel

**文件**: `app/src/main/java/com/rehealth/genie/ui/InterventionFeedbackViewModel.kt` (新建)

```kotlin
package com.rehealth.genie.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.work.MeasurementSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val lastSubmittedId: String? = null,
)

class InterventionFeedbackViewModel(
    private val context: Context,
) : ViewModel() {
    
    private val app = context.applicationContext as ReHealthApplication
    private val feedbackRepo = app.interventionFeedbackRepository
    
    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()
    
    /**
     * Submit typed intervention feedback.
     * 
     * @param interventionId UUID of the intervention
     * @param status "completed", "partially_completed", "skipped", "not_applicable"
     * @param note Optional user note
     */
    fun submitFeedback(
        interventionId: String,
        status: String,
        note: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = FeedbackUiState(isSubmitting = true)
            
            try {
                // D3: Submit feedback locally (never fails)
                val feedbackId = feedbackRepo.submitFeedback(
                    interventionId = interventionId,
                    status = status,
                    note = note,
                )
                
                // D3: Trigger immediate upload
                MeasurementSyncWorker.triggerImmediate(context)
                
                _uiState.value = FeedbackUiState(
                    message = getSuccessMessage(status),
                    lastSubmittedId = feedbackId,
                )
            } catch (e: Exception) {
                _uiState.value = FeedbackUiState(
                    message = "反馈保存失败: ${e.message}",
                )
            }
        }
    }
    
    private fun getSuccessMessage(status: String): String {
        return when (status) {
            "completed" -> "已完成反馈，感谢您的坚持！"
            "partially_completed" -> "部分完成反馈已记录"
            "skipped" -> "已标记为稍后完成"
            "not_applicable" -> "已标记为不适用"
            else -> "反馈已记录"
        }
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InterventionFeedbackViewModel(context) as T
        }
    }
}
```

### 步骤3.3: 更新ReHealthApp.kt - 删除onCheckIn参数

**文件**: `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`

找到以下代码（第220行附近）：
```kotlin
onCheckIn = ringViewModel::submitCheckIn,
```

**删除这一行**，并找到所有使用`onCheckIn`参数的地方。

### 步骤3.4: 更新PatientPlanRow使用类型化反馈

**文件**: `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`

找到`PatientPlanRow`函数（第543行附近）：

**旧代码**:
```kotlin
private fun PatientPlanRow(item: PatientInterventionPayload, onCheckIn: (String) -> Unit) {
    // ... 现有UI
    Button(
        onClick = { item.id?.let(onCheckIn) },
    ) {
        Text("打卡")
    }
}
```

**新代码**:
```kotlin
@Composable
private fun PatientPlanRow(
    item: PatientInterventionPayload,
    feedbackViewModel: InterventionFeedbackViewModel,
) {
    val feedbackState by feedbackViewModel.uiState.collectAsState()
    
    // ... 保留现有UI代码（卡片、标题、描述等）
    
    // 替换单个"打卡"按钮为三个类型化反馈按钮
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 完成按钮
        Button(
            onClick = {
                item.id?.let { interventionId ->
                    feedbackViewModel.submitFeedback(
                        interventionId = interventionId,
                        status = "completed",
                        note = null,
                    )
                }
            },
            enabled = !feedbackState.isSubmitting,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("完成")
        }
        
        // 不适用按钮
        OutlinedButton(
            onClick = {
                item.id?.let { interventionId ->
                    feedbackViewModel.submitFeedback(
                        interventionId = interventionId,
                        status = "not_applicable",
                        note = null,
                    )
                }
            },
            enabled = !feedbackState.isSubmitting,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("不适用")
        }
        
        // 稍后按钮
        OutlinedButton(
            onClick = {
                item.id?.let { interventionId ->
                    feedbackViewModel.submitFeedback(
                        interventionId = interventionId,
                        status = "skipped",
                        note = null,
                    )
                }
            },
            enabled = !feedbackState.isSubmitting,
            modifier = Modifier.weight(1f),
        ) {
            Text("稍后")
        }
    }
    
    // 显示反馈消息
    feedbackState.message?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
```

### 步骤3.5: 更新所有调用PatientPlanRow的地方

在ReHealthApp.kt中找到所有调用`PatientPlanRow`的地方（第1156行附近），更新为：

**旧代码**:
```kotlin
PatientPlanRow(item = item, onCheckIn = onCheckIn)
```

**新代码**:
```kotlin
val context = LocalContext.current
val feedbackViewModel: InterventionFeedbackViewModel = viewModel(
    factory = InterventionFeedbackViewModel.Factory(context)
)
PatientPlanRow(item = item, feedbackViewModel = feedbackViewModel)
```

### 测试步骤3

```powershell
# 1. 编译（应该没有编译错误）
.\gradlew.bat assembleDebug

# 2. 检查是否还有submitCheckIn引用
findstr /s /i "submitCheckIn" app\src\main\java\*.kt
# 应该没有结果

# 3. 安装到设备
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 4. 测试反馈流程
# - 查看干预计划列表
# - 点击"完成"按钮
# - 检查logcat:
adb logcat | findstr "InterventionFeedback"
# 应该看到反馈已排队

# 5. 验证离线反馈
# - 断开网络
# - 提交反馈
# - 重新连接网络
# - 30分钟内应该自动上传（或立即触发）
```

### 回滚方案3

如果类型化反馈有问题：

```kotlin
// 临时恢复submitCheckIn方法到RingViewModel
fun submitCheckIn(itemId: String) {
    // 使用新的反馈API作为临时实现
    val app = application as ReHealthApplication
    viewModelScope.launch {
        app.interventionFeedbackRepository.submitFeedback(
            interventionId = itemId,
            status = "completed",
            note = "Legacy check-in migration",
        )
    }
}
```

---

## 任务4: 添加QueueStatusBanner (1小时)

### 目标
在ReHealthApp顶部显示队列状态横幅。

### 步骤4.1: 更新ReHealthApp.kt添加banner观察

**文件**: `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`

在ReHealthApp composable的顶部添加：

```kotlin
@Composable
fun ReHealthApp(
    ringViewModel: RingViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ReHealthApplication
    
    // D3: 观察队列状态
    val queueState by app.syncRepository.queueState.collectAsState()
    val pendingFeedback by app.interventionFeedbackRepository
        .observePendingFeedback()
        .collectAsState(initial = emptyList())
    
    Column(modifier = Modifier.fillMaxSize()) {
        // D3: 队列状态横幅
        QueueStatusBanner(
            queueState = queueState,
            pendingCount = pendingFeedback.size,
            onLoginClick = {
                // 导航到登录页面
                navController.navigate("login")
            }
        )
        
        // ... 现有UI内容
    }
}
```

### 步骤4.2: 添加必要的import

在ReHealthApp.kt文件顶部添加：

```kotlin
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.ui.components.QueueStatusBanner
```

### 测试步骤4

```powershell
# 1. 编译和安装
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 2. 测试banner显示
# - 登录应用
# - 提交反馈 → 应该看到蓝色"正在同步 1 条反馈..."
# - 强制401（修改token为无效值）→ 应该看到黄色"会话已过期"
# - 点击黄色banner → 应该导航到登录页面

# 3. 验证banner逻辑
# - 无待处理反馈 + 已授权 → banner隐藏
# - 有待处理反馈 + 已授权 → 显示"正在同步"
# - 队列暂停 → 显示"会话已过期"
```

### 回滚方案4

如果banner有问题，可以临时注释掉：

```kotlin
// QueueStatusBanner(...)  // 临时禁用
```

---

## 任务5: Worker初始化 (0.5小时)

### 目标
在应用启动时，如果已登录则调度worker。

### 步骤5.1: 更新ReHealthApplication.onCreate

**文件**: `app/src/main/java/com/rehealth/genie/ReHealthApplication.kt`

在`onCreate()`方法中添加：

```kotlin
override fun onCreate() {
    super.onCreate()
    RingNotificationChannels.ensure(this)
    if (RingBackgroundCollectionSettings.isActive(this)) {
        RingBackgroundRecoveryWorker.schedule(this)
    }
    
    // D3: 如果已登录，调度feedback sync worker
    if (sessionStore.isLoggedIn) {
        MeasurementSyncWorker.schedule(this)
    }
}
```

### 步骤5.2: 添加import

在ReHealthApplication.kt顶部添加：

```kotlin
import com.rehealth.genie.work.MeasurementSyncWorker
```

### 测试步骤5

```powershell
# 1. 编译和安装
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# 2. 测试worker自动调度
# - 登录应用
# - 完全关闭应用
# - 重新启动应用
# - 检查logcat:
adb logcat | findstr "MeasurementSyncWorker"
# 应该看到: "MeasurementSyncWorker scheduled"

# 3. 验证worker运行
# - 提交反馈
# - 等待30分钟（或手动触发）
# - 检查logcat是否有上传日志
```

---

## 任务6: 设备测试 (2小时)

### 完整测试清单

#### 6.1 登录流程测试

- [ ] 输入有效手机号和验证码 → 登录成功
- [ ] 输入无效验证码 → 显示错误信息
- [ ] 网络断开状态下登录 → 显示网络错误
- [ ] 登录后token已保存（检查logcat）
- [ ] 登录后worker已调度（检查logcat）
- [ ] 登录后队列状态为Active

#### 6.2 反馈提交测试

- [ ] 点击"完成"按钮 → 显示"已完成反馈，感谢您的坚持！"
- [ ] 点击"不适用"按钮 → 显示"已标记为不适用"
- [ ] 点击"稍后"按钮 → 显示"已标记为稍后完成"
- [ ] 离线提交反馈 → 反馈已排队（banner显示待处理数量）
- [ ] 重新上线 → 30分钟内自动上传或立即触发上传

#### 6.3 队列状态Banner测试

- [ ] 无待处理反馈 → banner隐藏
- [ ] 有待处理反馈 → 显示"正在同步 N 条反馈..."
- [ ] 队列暂停（401） → 显示"会话已过期，点击重新登录"
- [ ] 点击暂停banner → 导航到登录页面
- [ ] 重新登录后 → banner消失或显示同步状态

#### 6.4 登出流程测试

- [ ] 点击"退出登录" → 显示确认对话框
- [ ] 确认退出 → worker已取消（检查logcat）
- [ ] 退出后token已清除
- [ ] 退出后队列状态为Paused
- [ ] 退出后banner显示"会话已过期"

#### 6.5 Worker测试

- [ ] 应用启动时，如果已登录 → worker自动调度
- [ ] 提交反馈后 → worker立即触发上传
- [ ] Worker每30分钟运行一次（检查logcat）
- [ ] Worker上传成功 → 反馈标记为done
- [ ] Worker遇到401 → 停止运行，队列暂停

#### 6.6 边缘情况测试

- [ ] 快速点击多个反馈按钮 → 所有反馈都已排队
- [ ] 提交10条反馈 → 全部上传成功
- [ ] 长时间离线（超过1天）→ 反馈仍在队列中
- [ ] 应用崩溃重启 → 队列数据保留
- [ ] Token过期（401）→ 队列暂停，显示错误banner

### Logcat过滤命令

```powershell
# 查看认证相关日志
adb logcat | findstr "AuthenticatedApiClient"

# 查看反馈相关日志
adb logcat | findstr "InterventionFeedback"

# 查看Worker相关日志
adb logcat | findstr "MeasurementSyncWorker"

# 查看所有D3相关日志
adb logcat | findstr /i "D3 auth feedback sync queue"
```

---

## 故障排除

### 问题1: 编译错误 - Unresolved reference

**症状**: `Unresolved reference: AuthenticatedApiClient`

**解决**:
```powershell
# 清理构建
.\gradlew.bat clean

# 重新构建
.\gradlew.bat assembleDebug

# 如果仍然失败，检查import语句
```

### 问题2: Worker不运行

**症状**: Worker没有按期调度

**排查**:
```powershell
# 检查WorkManager状态
adb shell dumpsys jobscheduler | findstr measurement_sync

# 检查是否有pending work
# 在代码中添加日志
```

**解决**:
- 确保设备有网络连接
- 确保电池不是低电量
- 确保应用没有被系统电池优化杀死

### 问题3: 401不触发队列暂停

**症状**: 收到401但队列仍在运行

**排查**:
```kotlin
// 在AuthenticatedApiClient中添加日志
when (val error = outcome.error) {
    is RemotePhmError.HttpStatusError -> {
        if (error.statusCode == 401) {
            Log.w(TAG, "401 detected, pausing queue")
            authState = AuthState.Unauthorized
            // ...
        }
    }
}
```

### 问题4: Banner不显示

**症状**: QueueStatusBanner不显示

**排查**:
- 检查`collectAsState()`是否正确订阅
- 检查`queueState`和`pendingFeedback`的值
- 添加日志验证数据流

### 问题5: 反馈未上传

**症状**: 反馈留在队列中，未上传

**排查**:
```powershell
# 检查数据库
adb shell
run-as com.rehealth.genie
cd databases
sqlite3 rehealth-local.db
SELECT * FROM intervention_feedback_queue;
```

**解决**:
- 检查网络连接
- 检查后端是否返回intervention ID
- 检查是否有401错误

---

## 完成检查清单

### 代码变更

- [ ] LoginViewModel已创建
- [ ] LoginScreen已更新使用ViewModel
- [ ] ProfileScreen已创建（或登出逻辑已添加）
- [ ] RingViewModel.submitCheckIn已删除
- [ ] InterventionFeedbackViewModel已创建
- [ ] PatientPlanRow已更新为类型化反馈按钮
- [ ] QueueStatusBanner已添加到ReHealthApp
- [ ] ReHealthApplication.onCreate已添加worker调度
- [ ] 所有import语句已添加

### 测试验证

- [ ] 所有6个测试步骤已执行
- [ ] 登录流程正常工作
- [ ] 反馈提交正常工作
- [ ] Banner正确显示
- [ ] Worker正常运行
- [ ] 登出流程正常工作
- [ ] 边缘情况已测试

### 文档更新

- [ ] 已记录所有已知问题
- [ ] 已更新README（如果需要）
- [ ] 已创建测试报告

---

## 提交更改

### Git提交策略

建议分多个commit提交：

```powershell
# Commit 1: 登录集成
git add app/src/main/java/com/rehealth/genie/ui/LoginViewModel.kt
git add app/src/main/java/com/rehealth/genie/ui/LoginScreen.kt
git add app/src/main/java/com/rehealth/genie/network/AuthenticatedApiClient.kt
git add app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt
git add app/src/main/java/com/rehealth/genie/network/dto/AuthDto.kt
git commit -m "feat(android): D3 UI - implement login flow with auth hooks

Integrate D3 authentication into login flow:
- Add LoginViewModel with real JeecgBoot login
- Call authenticatedApiClient.onLoginSuccess() after login
- Call syncRepository.resumeQueue() to resume queue
- Schedule MeasurementSyncWorker after successful login

Tested: Login flow works, token saved, worker scheduled"

# Commit 2: 登出集成
git add app/src/main/java/com/rehealth/genie/ui/ProfileScreen.kt
git commit -m "feat(android): D3 UI - implement logout flow

Add ProfileScreen with D3 logout hooks:
- Cancel MeasurementSyncWorker on logout
- Call authenticatedApiClient.onLogout()
- Call syncRepository.pauseQueue()

Tested: Logout clears token, cancels worker, pauses queue"

# Commit 3: 类型化反馈
git add app/src/main/java/com/rehealth/genie/ui/InterventionFeedbackViewModel.kt
git add app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt
git add app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt
git commit -m "feat(android): D3 UI - replace submitCheckIn with typed feedback

Replace legacy submitCheckIn with typed intervention feedback:
- Remove RingViewModel.submitCheckIn()
- Add InterventionFeedbackViewModel
- Update PatientPlanRow with three feedback buttons (完成/不适用/稍后)
- Trigger immediate sync after feedback submission

Tested: Feedback buttons work, feedback queued, uploaded successfully"

# Commit 4: Banner和Worker
git add app/src/main/java/com/rehealth/genie/ReHealthApplication.kt
git commit -m "feat(android): D3 UI - add queue status banner and worker init

Complete D3 UI integration:
- Add QueueStatusBanner to ReHealthApp top
- Schedule worker on app startup if logged in
- Display sync status and session expired state

Tested: Banner shows correctly, worker auto-schedules on startup"

# Commit 5: 文档
git add codex-runs/2026-07-20/D3_MANUAL_INTEGRATION_GUIDE.md
git add codex-runs/2026-07-20/D3_INTEGRATION_TEST_REPORT.md
git commit -m "docs(android): D3 manual integration guide and test report

Add comprehensive D3 integration documentation:
- Step-by-step implementation guide
- Test procedures and results
- Troubleshooting guide
- Known issues and solutions"
```

### 推送到远程

```powershell
# 推送D3分支
git push -u origin work/D3_android_auth_typed_feedback
```

---

## 后续工作

### 立即跟进

1. **添加单元测试** (3-4小时)
   - AuthenticatedApiClient测试
   - InterventionFeedbackRepository测试
   - SyncRepository测试
   - LoginViewModel测试

2. **性能优化** (2-3小时)
   - Worker批量上传
   - 减少数据库查询
   - 优化UI重组

3. **用户体验改进** (2-3小时)
   - 添加上传进度通知
   - 改进错误消息
   - 添加重试按钮

### 可选增强

4. **高级功能**
   - 添加反馈备注输入框
   - 支持反馈编辑（上传前）
   - 添加反馈历史查看
   - 支持反馈统计

5. **监控和分析**
   - 添加反馈成功率统计
   - 添加网络质量监控
   - 添加崩溃报告

---

## 总结

本指南提供了D3 UI集成的完整实施路径。按照6个任务顺序执行，每个任务都有：

✅ **明确目标**  
✅ **详细步骤**  
✅ **完整代码**  
✅ **测试方法**  
✅ **回滚方案**

**估计时间**: 6-8小时  
**难度**: 中等  
**风险**: 低（有回滚方案）

**成功标准**:
- 所有测试通过
- 无编译错误
- 无运行时崩溃
- 反馈上传成功
- 队列正确暂停/恢复

祝实施顺利！如有问题，参考故障排除部分或查看已创建的文档。

---

**文档版本**: 1.0  
**创建日期**: 2026-07-20  
**最后更新**: 2026-07-20  
**作者**: Codex D3 Agent (Claude Opus 4.8)
