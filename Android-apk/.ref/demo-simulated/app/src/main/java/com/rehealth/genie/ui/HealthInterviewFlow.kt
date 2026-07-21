package com.rehealth.genie.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rehealth.genie.R
import com.rehealth.genie.interview.HealthBaseline
import com.rehealth.genie.interview.InterviewAnswer
import com.rehealth.genie.interview.MockHealthInterviewModel
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import java.util.Locale

@Composable
fun HealthInterviewFlow(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    completionLabel: String = "进入睿禾精灵",
) {
    val context = LocalContext.current
    val model = remember { MockHealthInterviewModel() }
    val answers = remember { mutableStateListOf<InterviewAnswer>() }
    var input by remember { mutableStateOf("") }
    var baseline by remember { mutableStateOf<HealthBaseline?>(null) }
    var voiceMessage by remember { mutableStateOf<String?>(null) }
    val question = model.questions.getOrNull(answers.size)
    val listState = rememberLazyListState()
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.SIMPLIFIED_CHINESE
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    fun submitAnswer(content: String) {
        val current = question ?: return
        val answer = content.trim()
        if (answer.isEmpty()) return
        answers += InterviewAnswer(current, answer)
        input = ""
        if (answers.size == model.questions.size) {
            baseline = model.buildBaseline(answers)
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!recognized.isNullOrBlank()) submitAnswer(recognized)
            else voiceMessage = "没有听清，请再试一次或切换到文字回答"
        }
    }
    fun startVoiceAnswer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请回答小禾灵的问题")
        }
        runCatching { voiceLauncher.launch(intent) }
            .onFailure { voiceMessage = "当前设备没有可用的语音识别服务，请使用文字回答" }
    }
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startVoiceAnswer()
        else voiceMessage = "需要麦克风权限才能使用实时语音"
    }
    fun requestVoiceAnswer() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceAnswer()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(answers.size) {
        if (baseline == null) {
            listState.animateScrollToItem((answers.size * 2).coerceAtLeast(0))
        }
        question?.let { tts?.speak(it.text, TextToSpeech.QUEUE_FLUSH, null, it.id) }
    }

    if (baseline != null) {
        BaselineResultScreen(baseline!!, completionLabel, onComplete)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding().imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "返回", tint = Ink)
            }
            Column(Modifier.weight(1f)) {
                Text("健康初识", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("和小禾灵聊聊你的日常", color = Muted, fontSize = 11.sp)
            }
            Text(
                "${answers.size + 1}/${model.questions.size}",
                color = Mint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { answers.size.toFloat() / model.questions.size },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = Mint,
            trackColor = MintSoft,
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.xiaohelin),
                contentDescription = "小禾灵",
                modifier = Modifier.size(176.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(model.questions.take(answers.size + 1)) { index, item ->
                val focused = index == answers.size
                Column(
                    modifier = Modifier.fillMaxWidth().alpha(if (focused) 1f else 0.28f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        item.text,
                        color = Ink,
                        fontSize = if (focused) 20.sp else 14.sp,
                        lineHeight = if (focused) 30.sp else 21.sp,
                        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                    if (focused) {
                        Text(
                            item.helper,
                            color = Muted,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    } else {
                        Text(
                            answers[index].content,
                            color = Mint,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
        question?.quickReplies?.takeIf { it.isNotEmpty() }?.let { replies ->
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(replies) { reply ->
                    Text(
                        reply,
                        color = Mint,
                        fontSize = 12.sp,
                        modifier = Modifier.clip(CircleShape).background(MintSoft)
                            .clickable { submitAnswer(reply) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }
        voiceMessage?.let {
            Text(
                it,
                color = Color(0xFFD38B18),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("输入你的回答…", color = Muted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                maxLines = 3,
                trailingIcon = {
                    IconButton(onClick = ::requestVoiceAnswer) {
                        Icon(Icons.Outlined.MicNone, "语音回答", tint = Mint)
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { submitAnswer(input) }),
            )
            IconButton(
                onClick = { submitAnswer(input) },
                enabled = input.isNotBlank(),
                modifier = Modifier.padding(start = 7.dp).size(50.dp).clip(CircleShape).background(Mint),
            ) {
                Icon(Icons.Outlined.Send, "发送", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ChatBubble(text: String) {
    Text(
        text,
        color = Ink,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
            .background(Color.White).border(1.dp, Line, RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
            .padding(13.dp),
    )
}

@Composable
private fun UserBubble(text: String) {
    Text(
        text,
        color = Ink,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
            .background(MintSoft).padding(13.dp),
    )
}

@Composable
private fun BaselineResultScreen(
    baseline: HealthBaseline,
    completionLabel: String,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(66.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Mint, modifier = Modifier.size(38.dp))
                }
                Text("健康初识已完成", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Text("小禾灵会结合智能戒指数据继续了解你", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("你的健康画像", color = Ink, fontWeight = FontWeight.Bold)
                    baseline.items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(top = 13.dp), verticalAlignment = Alignment.Top) {
                            Text(item.label, color = Mint, fontSize = 12.sp, modifier = Modifier.size(width = 76.dp, height = 24.dp))
                            Text(item.value, color = Ink, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MintSoft),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("首批关注方向", color = Ink, fontWeight = FontWeight.Bold)
                    baseline.focusAreas.forEach {
                        Text("• $it", color = Mint, fontSize = 13.sp, modifier = Modifier.padding(top = 9.dp))
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(18.dp))
                Text(
                    "本次交流内容默认保存在本机，不替代医生诊断。",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        item {
            Button(
                onClick = {
                    val summary = baseline.items.joinToString("\n") { "${it.label}:${it.value}" }
                    context.getSharedPreferences("rehealth_profile", 0)
                        .edit()
                        .putString("health_baseline", summary)
                        .putLong("health_baseline_updated_at", baseline.generatedAt)
                        .apply()
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
            ) {
                Text(completionLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "你可以在“我的”中重新进行健康初识",
                color = Muted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
