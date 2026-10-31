package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.theme.EmberColor
import com.laserarrows.app.ui.components.GameText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

/** そのショットで加算されたスコアを、スコア表示の上にふわっと出す(何点入ったか一目でわかるように)。 */
@Composable
fun ScoreBonusPopupView(gained: Int, relays: Int = 0, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.15f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
        scale.animateTo(1f, tween(90))
        delay(500)
        alpha.animateTo(0f, tween(300))
    }

    Box(modifier = modifier) {
        GameText(
            "+$gained",
            fontSize = 14.sp,
            color = EmberColor,
            singleLine = true,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value),
        )
    }
}
