package com.laserarrows.app.ui.game

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.theme.GoldColor
import kotlinx.coroutines.delay

@Composable
fun ComboPopupView(combo: Int, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.3f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
        scale.animateTo(1f, tween(110))
        delay(420)
        alpha.animateTo(0f, tween(260))
    }

    Box(modifier = modifier) {
        Text(
            "COMBO ×$combo!",
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = GoldColor,
            style = LocalTextStyle.current.copy(
                shadow = Shadow(color = GoldColor.copy(alpha = 0.8f), blurRadius = 24f),
            ),
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value),
        )
    }
}
