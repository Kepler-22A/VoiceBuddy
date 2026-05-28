package com.englishvoicebuddy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishvoicebuddy.ui.theme.BgWarm
import com.englishvoicebuddy.ui.theme.BgWhite
import com.englishvoicebuddy.ui.theme.BubbleAiBg
import com.englishvoicebuddy.ui.theme.BubbleAiText
import com.englishvoicebuddy.ui.theme.BubbleUserBg
import com.englishvoicebuddy.ui.theme.BubbleUserText
import com.englishvoicebuddy.ui.theme.OnlineGreen
import com.englishvoicebuddy.ui.theme.Orange
import com.englishvoicebuddy.ui.theme.OrangeLight
import com.englishvoicebuddy.ui.theme.TextDim
import com.englishvoicebuddy.ui.theme.TextPrimary
import com.englishvoicebuddy.ui.theme.TextSecondary
import com.englishvoicebuddy.viewmodel.ChatMessage
import com.englishvoicebuddy.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    var showApiSettings by remember { mutableStateOf(!viewModel.hasApiKey()) }
    var showPromptSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = BgWarm,
        topBar = {
            TopBar(
                connected = viewModel.connected.collectAsState().value,
                onAvatarClick = { showPromptSettings = true },
                onSettingsClick = { showApiSettings = true },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ChatList(
                messages = viewModel.messages.collectAsState().value,
                showScrollBadge = viewModel.showScrollBadge.collectAsState().value,
                onScrolledUp = { viewModel.onUserScrolledUp() },
                onScrollToBottom = { viewModel.onScrollToBottom() },
                modifier = Modifier.weight(1f),
            )

            Box(
                modifier = Modifier.fillMaxWidth().border(1.dp, com.englishvoicebuddy.ui.theme.Divider),
                contentAlignment = Alignment.Center,
            ) {
                MicButton(
                    state = viewModel.micState.collectAsState().value,
                    onPressStart = { viewModel.startRecording() },
                    onPressEnd = { viewModel.stopRecording() },
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }

    if (showApiSettings) {
        ModalBottomSheet(
            onDismissRequest = { showApiSettings = false },
            sheetState = sheetState,
            containerColor = BgWhite,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            ApiSettingsSheet(
                apiKey = viewModel.getApiKey(),
                model = viewModel.getModel(),
                voices = viewModel.getVoices(),
                currentVoice = viewModel.getVoice(),
                onSave = { apiKey, model, voice ->
                    viewModel.saveSettings(apiKey, model, voice, viewModel.getPrompt())
                    showApiSettings = false
                },
            )
        }
    }

    if (showPromptSettings) {
        ModalBottomSheet(
            onDismissRequest = { showPromptSettings = false },
            sheetState = sheetState,
            containerColor = BgWhite,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            SettingsSheet(
                prompt = viewModel.getPrompt(),
                onSave = { prompt ->
                    viewModel.saveSettings(viewModel.getApiKey(), viewModel.getModel(), viewModel.getVoice(), prompt)
                    showPromptSettings = false
                },
                onReset = {
                    viewModel.resetPrompt()
                    showPromptSettings = false
                },
            )
        }
    }
}

@Composable
private fun TopBar(connected: Boolean, onAvatarClick: () -> Unit, onSettingsClick: () -> Unit) {
    Surface(color = BgWhite) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, com.englishvoicebuddy.ui.theme.Divider)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(40.dp).clickable { onAvatarClick() }) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Orange, OrangeLight))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("E", color = BubbleUserText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(OnlineGreen)
                        .align(Alignment.BottomEnd)
                )
                // ✎ 角标
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White, CircleShape)
                        .border(1.5.dp, Orange, CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✎", fontSize = 7.sp, color = Orange)
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Emma", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("在线 · 英语老师", color = TextSecondary, fontSize = 10.sp)
            }

            Text(
                if (connected) "● 已连接" else "● 未连接",
                color = if (connected) OnlineGreen else TextDim,
                fontSize = 10.sp,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙", color = TextDim, fontSize = 20.sp)
            }
        }
    }
    Divider(color = com.englishvoicebuddy.ui.theme.Divider, thickness = 1.dp)
}

@Composable
private fun ChatList(
    messages: List<ChatMessage>,
    showScrollBadge: Boolean,
    onScrolledUp: () -> Unit,
    onScrollToBottom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 2
        }
    }

    LaunchedEffect(messages.size) {
        if (isAtBottom) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            items(messages, key = { messages.indexOf(it) }) { msg ->
                when (msg) {
                    is ChatMessage.User -> UserBubble(msg.text)
                    is ChatMessage.AiStreaming -> AiBubble(msg.text)
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollBadge,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                Modifier
                    .padding(bottom = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Orange)
                    .clickable {
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                            onScrollToBottom()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("↓", color = BubbleUserText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    val isUserScrolling = !isAtBottom
    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) onScrolledUp()
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 16.dp,
                ))
                .background(BubbleUserBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text, color = BubbleUserText, fontSize = 13.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(TextDim),
            contentAlignment = Alignment.Center,
        ) {
            Text("我", color = BubbleUserText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AiBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Orange, OrangeLight))),
            contentAlignment = Alignment.Center,
        ) {
            Text("E", color = BubbleUserText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(
                    topStart = 6.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp,
                ))
                .background(BubbleAiBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text, color = BubbleAiText, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}
