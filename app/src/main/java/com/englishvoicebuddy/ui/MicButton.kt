package com.englishvoicebuddy.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.englishvoicebuddy.R
import com.englishvoicebuddy.ui.theme.MicDisabled
import com.englishvoicebuddy.ui.theme.Orange
import com.englishvoicebuddy.ui.theme.RecordingRed
import com.englishvoicebuddy.viewmodel.MicState

@Composable
fun MicButton(
    state: MicState,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = when (state) {
            MicState.IDLE -> Orange
            MicState.RECORDING -> RecordingRed
            MicState.DISABLED -> MicDisabled
        },
        label = "micColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val rippleScale by infiniteTransition.animateFloat(1f, 1.8f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Restart), label = "r1")
    val rippleAlpha by infiniteTransition.animateFloat(0.6f, 0f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Restart), label = "ra")
    val rippleScale2 by infiniteTransition.animateFloat(1f, 1.8f,
        animationSpec = infiniteRepeatable(animation = tween(1200, delayMillis = 400), repeatMode = RepeatMode.Restart), label = "r2")

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        if (state == MicState.RECORDING) {
            Box(Modifier.size(48.dp).scale(rippleScale).clip(CircleShape)
                .border(2.dp, RecordingRed.copy(alpha = rippleAlpha), CircleShape))
            Box(Modifier.size(48.dp).scale(rippleScale2).clip(CircleShape)
                .border(2.dp, RecordingRed.copy(alpha = rippleAlpha * 0.7f), CircleShape))
        }

        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(bgColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (state == MicState.RECORDING) {
                Box(Modifier.size(12.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(Color.White))
            } else if (state == MicState.DISABLED) {
                Icon(painter = painterResource(R.drawable.ic_mic), contentDescription = "麦克风",
                    tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
            } else {
                Icon(painter = painterResource(R.drawable.ic_mic), contentDescription = "麦克风",
                    tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}
