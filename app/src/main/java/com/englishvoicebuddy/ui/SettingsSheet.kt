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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishvoicebuddy.ui.theme.BgWarm
import com.englishvoicebuddy.ui.theme.BubbleUserText
import com.englishvoicebuddy.ui.theme.Orange
import com.englishvoicebuddy.ui.theme.OrangeLight
import com.englishvoicebuddy.ui.theme.RecordingRed
import com.englishvoicebuddy.ui.theme.TextPrimary
import com.englishvoicebuddy.ui.theme.TextSecondary

@Composable
fun SettingsSheet(
    prompt: String,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
) {
    var editingPrompt by remember { mutableStateOf(prompt) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
    ) {
        Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
            .background(TextSecondary.copy(alpha = 0.4f)).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Orange, OrangeLight))),
                contentAlignment = Alignment.Center
            ) { Text("E", color = BubbleUserText, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Emma", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("英语老师 · 系统提示词", color = TextSecondary, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = editingPrompt,
            onValueChange = { editingPrompt = it },
            modifier = Modifier.fillMaxWidth().height(400.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 22.sp, color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Orange, unfocusedBorderColor = Orange.copy(alpha = 0.4f),
                focusedContainerColor = BgWarm, unfocusedContainerColor = BgWarm,
            ),
            shape = RoundedCornerShape(10.dp),
        )

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onSave(prompt) }.padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("取消", fontSize = 12.sp, color = TextSecondary) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Orange)
                .clickable { onSave(editingPrompt) }.padding(horizontal = 20.dp, vertical = 7.dp)
            ) { Text("保存", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(8.dp))
                .clickable { onReset() }.padding(horizontal = 8.dp, vertical = 6.dp)
            ) { Text("恢复默认", fontSize = 10.sp, color = RecordingRed.copy(alpha = 0.5f)) }
        }
    }
}
