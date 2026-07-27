package com.rehealth.genie.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.R
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted

@Composable
internal fun InterviewScreen(onComplete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text("建立你的健康基线", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("我们一起聊聊你的健康", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.xiaohelin),
            contentDescription = null,
            modifier = Modifier.size(180.dp).padding(vertical = 8.dp),
            contentScale = ContentScale.Fit,
        )
        FeatureCard(Icons.Outlined.AutoAwesome, "AI 健康采访", "通过问答了解你，生成专属健康画像")
        FeatureCard(Icons.Outlined.Shield, "端侧模型守护隐私", "数据默认保存在本机，原始数据不上传")
        FeatureCard(Icons.Outlined.FavoriteBorder, "更懂你的健康伙伴", "越了解你，建议越精准，越有温度")
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint),
        ) {
            Text("开始健康采访", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("约 5 分钟", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp))
            .background(Color.White).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mint)
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
