package com.rehealth.genie.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.R
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.Muted

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var showAgreementHint by remember { mutableStateOf(false) }
    var showLoginHint by remember { mutableStateOf(false) }
    val canLogin = username.isNotBlank() && password.isNotBlank() && agreed && !uiState.isLoading
    val brandGreen = Color(0xFF08A97B)
    val outlineGreen = Color(0xFFB9DDD5)
    val density = LocalDensity.current

    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
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
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = brandGreen)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.xiaohelin),
                    contentDescription = "小禾灵",
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                "睿禾精灵",
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text("ReHealth AI", color = Color(0xFF0A6D5B), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "欢迎回来，开启你的智能健康管理",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp, bottom = 12.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x330C806B))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Text("账号登录", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim().take(64) },
                    placeholder = { Text("请输入账号", fontSize = 14.sp, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Outlined.PersonOutline, null) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandGreen,
                        unfocusedBorderColor = outlineGreen,
                        focusedLeadingIconColor = brandGreen,
                        unfocusedLeadingIconColor = Color(0xFF15836E),
                        cursorColor = brandGreen,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(64) },
                    placeholder = { Text("请输入密码", fontSize = 14.sp, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandGreen,
                        unfocusedBorderColor = outlineGreen,
                        focusedLeadingIconColor = brandGreen,
                        unfocusedLeadingIconColor = Color(0xFF15836E),
                        cursorColor = brandGreen,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )
                Button(
                    onClick = {
                        if (canLogin) {
                            viewModel.login(username, password)
                        } else {
                            showLoginHint = true
                            if (!agreed) showAgreementHint = true
                        }
                    },
                    enabled = canLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandGreen),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("登录 / 注册", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                if (showLoginHint && (username.isBlank() || password.isBlank())) {
                    Text(
                        "请输入账号和密码",
                        color = Color(0xFFD94C4C),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8EC))
                    Text("其他登录方式", color = Muted, fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8EC))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            if (agreed) onLoginSuccess() else showAgreementHint = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF19B765),
                            containerColor = Color(0xFFF1FCF6),
                        ),
                        border = BorderStroke(1.dp, Color(0xFFBDEBD2)),
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(16.dp))
                        Text(
                            "微信一键登录",
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (agreed) onLoginSuccess() else showAgreementHint = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF536273),
                            containerColor = Color(0xFFF8FAFC),
                        ),
                        border = BorderStroke(1.dp, Color(0xFFDCE4EA)),
                    ) {
                        Icon(Icons.Outlined.PersonOutline, null, modifier = Modifier.size(16.dp))
                        Text(
                            "游客体验",
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("还没有账号？", color = Muted, fontSize = 13.sp)
                    Text(
                        "去注册",
                        color = brandGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onGoToRegister() }
                            .padding(start = 4.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable {
                            agreed = !agreed
                            if (agreed) showAgreementHint = false
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (agreed) brandGreen else Color.White)
                            .border(
                                1.5.dp,
                                if (agreed) brandGreen else Color(0xFF6C7880),
                                RoundedCornerShape(4.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (agreed) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Text(
                        "我已阅读并同意《用户协议》与《隐私政策》",
                        color = Muted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                if (showAgreementHint) {
                    Text(
                        "请先阅读并同意用户协议与隐私政策",
                        color = Color(0xFFD94C4C),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(17.dp))
                Text(
                    "个人健康数据已加密保护",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
        }
    }
    }
}
