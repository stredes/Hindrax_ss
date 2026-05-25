package com.hindrax.ss.features.ascii

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.hindrax.ss.domain.ascii.AsciiAnimationSpec
import kotlinx.coroutines.delay

@Composable
fun AsciiAnimationPlayer(
    spec: AsciiAnimationSpec,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    var frameIndex by remember(spec.key) { mutableIntStateOf(0) }

    LaunchedEffect(spec.key, spec.frameMillis) {
        frameIndex = 0
        while (true) {
            delay(spec.frameMillis)
            frameIndex = (frameIndex + 1) % spec.frames.size
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        val fontSp = when {
            widthDp >= 720 -> 13
            widthDp >= 420 -> 12
            else -> 10
        }
        Text(
            text = spec.frames[frameIndex],
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSp.sp,
            lineHeight = (fontSp + 2).sp,
            textAlign = textAlign,
            softWrap = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AsciiAnimationStrip(
    spec: AsciiAnimationSpec,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center
) {
    var frameIndex by remember(spec.key) { mutableIntStateOf(0) }

    LaunchedEffect(spec.key, spec.frameMillis) {
        frameIndex = 0
        while (true) {
            delay(spec.frameMillis)
            frameIndex = (frameIndex + 1) % spec.frames.size
        }
    }

    Text(
        text = spec.frames[frameIndex].lineSequence().joinToString("  ") { it.trim() },
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 8.sp,
        lineHeight = 9.sp,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.fillMaxWidth()
    )
}
