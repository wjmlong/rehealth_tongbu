package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Muted

/**
 * 微信新建账号的强制绑定手机页：只有“绑定手机”与“退出登录”两个出口。
 * 绑定成功后回调 [onBound] 进入主页流程，退出登录回调 [onLoggedOut] 回登录页。
 */
@Composable
fun BindPhoneScreen(
    onBound: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: BindPhoneViewModel = viewModel(factory = BindPhoneViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.bound) {
        if (uiState.bound) onBound()
    }
    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) onLoggedOut()
    }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    val phoneValid = viewModel.isPhoneValid(phone)
    val canBind = phoneValid && smsCode.length == 6 && !uiState.isLoading
    val brandGreen = Color(0xFF08A97B)
    val outlineGreen = Color(0xFFB9DDD5)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE8FFF7), Color(0xFFF7FFFC), Color(0xFFEAF5FF)),
                ),
            )
            .statusBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text("绑定手机号", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "为使用保险等服务，请先绑定手机号",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x330C806B))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11) },
                    placeholder = { Text("请输入手机号", fontSize = 14.sp, maxLines = 1) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandGreen,
                        unfocusedBorderColor = outlineGreen,
                        cursorColor = brandGreen,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = smsCode,
                        onValueChange = { smsCode = it.filter { c -> c.isDigit() }.take(6) },
                        placeholder = { Text("6 位验证码", fontSize = 14.sp, maxLines = 1) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandGreen,
                            unfocusedBorderColor = outlineGreen,
                            cursorColor = brandGreen,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Button(
                        onClick = { viewModel.sendCode(phone) },
                        enabled = phoneValid && uiState.countdown == 0 && !uiState.isLoading,
                        modifier = Modifier.height(54.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandGreen,
                            disabledContainerColor = Color(0xFFD5E8E2),
                        ),
                    ) {
                        Text(
                            if (uiState.countdown > 0) "${uiState.countdown}s" else "获取验证码",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Button(
                    onClick = { viewModel.bind(phone, smsCode) },
                    enabled = canBind,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandGreen),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("确认绑定", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFD94C4C),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 6.dp),
                    )
                }
                Text(
                    "退出登录",
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(enabled = !uiState.isLoading) { viewModel.logout() }
                        .padding(top = 16.dp, bottom = 4.dp),
                )
            }
        }
    }
}
