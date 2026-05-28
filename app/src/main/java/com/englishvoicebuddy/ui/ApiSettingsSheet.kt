package com.englishvoicebuddy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishvoicebuddy.data.VoiceConfig
import com.englishvoicebuddy.ui.theme.BgWarm
import com.englishvoicebuddy.ui.theme.Orange
import com.englishvoicebuddy.ui.theme.TextPrimary
import com.englishvoicebuddy.ui.theme.TextSecondary

private val MODELS = listOf(
    "qwen3.5-omni-plus-realtime",
    "qwen3.5-omni-flash-realtime",
)

@Composable
fun ApiSettingsSheet(
    apiKey: String,
    model: String,
    voices: List<VoiceConfig>,
    currentVoice: String,
    onSave: (apiKey: String, model: String, voice: String) -> Unit,
) {
    var editingKey by remember { mutableStateOf(apiKey) }
    var editingModel by remember { mutableStateOf(model) }
    var modelExpanded by remember { mutableStateOf(false) }
    var editingVoice by remember { mutableStateOf(currentVoice) }
    var voiceExpanded by remember { mutableStateOf(false) }
    val selectedVoice = voices.find { it.voice == editingVoice }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
    ) {
        Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
            .background(TextSecondary.copy(alpha = 0.4f)).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))

        Text("API 设置", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(14.dp))

        Text("DashScope API Key", fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = editingKey,
            onValueChange = { editingKey = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange, unfocusedBorderColor = TextSecondary.copy(alpha = 0.2f),
                focusedContainerColor = BgWarm, unfocusedContainerColor = BgWarm,
            ),
            shape = RoundedCornerShape(10.dp),
        )

        Spacer(Modifier.height(14.dp))

        Text("模型", fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Column {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(BgWarm).border(1.dp, TextSecondary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .clickable { modelExpanded = !modelExpanded }
                    .padding(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    Text(editingModel, fontSize = 12.sp, color = TextPrimary)
                    Text("▼", fontSize = 10.sp, color = TextSecondary)
                }
            }
            if (modelExpanded) {
                MODELS.forEach { m ->
                    Box(
                        Modifier.fillMaxWidth()
                            .background(if (m == editingModel) BgWarm else BgWarm.copy(alpha = 0.5f))
                            .clickable { editingModel = m; modelExpanded = false }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(m, fontSize = 11.sp, color = if (m == editingModel) TextPrimary else TextSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text("音色", fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(BgWarm).border(1.dp, if (voiceExpanded) Orange else TextSecondary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { voiceExpanded = !voiceExpanded }
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selectedVoice?.name ?: editingVoice, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    selectedVoice?.description?.let {
                        Text(it, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                    }
                }
                Text("▼", fontSize = 10.sp, color = TextSecondary)
            }
        }
        if (voiceExpanded) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(10.dp))
                    .background(BgWarm).border(1.dp, Orange.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                LazyColumn {
                    items(voices) { v ->
                        val isSelected = v.voice == editingVoice
                        Box(
                            Modifier.fillMaxWidth()
                                .background(if (isSelected) BgWarm else BgWarm.copy(alpha = 0.5f))
                                .clickable { editingVoice = v.voice; voiceExpanded = false }
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(v.name, fontSize = 12.sp, color = TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    Row {
                                        Text(v.description, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                                        v.accent?.let { acc ->
                                            Spacer(Modifier.width(4.dp))
                                            Text("· $acc", fontSize = 10.sp, color = Orange.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                                if (isSelected) Text("✓", fontSize = 12.sp, color = Orange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(androidx.compose.ui.graphics.Color.Transparent)
                .border(1.dp, TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onSave(apiKey, model, currentVoice) }.padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) { Text("取消", fontSize = 12.sp, color = TextSecondary) }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(2f).clip(RoundedCornerShape(8.dp)).background(Orange)
                .clickable { onSave(editingKey, editingModel, editingVoice) }.padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) { Text("保存", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
}
