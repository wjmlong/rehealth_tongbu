package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.R
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.Muted
import kotlinx.coroutines.delay

@Composable
internal fun SplashScreen(onStart: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3_000)
        onStart()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8FDFB), Color(0xFFDCF8F2), Color(0xFFBEEFEA)),
                ),
            )
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            Text("睿禾精灵", color = Color(0xFF08765D), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("你的端侧 AI 健康伙伴", color = Ink, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(36.dp))
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.xiaohelin),
                contentDescription = "小禾灵",
                modifier = Modifier.size(260.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("健康数据默认保存在本机", color = Ink, fontSize = 13.sp)
            }
            Text("原始健康数据不会自动上传", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(38.dp))
        }
    }
}
