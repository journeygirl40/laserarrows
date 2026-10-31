package com.laserarrows.app.ui.game

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import com.laserarrows.app.ui.components.GameText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon2
import kotlinx.coroutines.delay

/** コンボ・ハイスコア更新・レベル記録更新で共通の「弾む→止まる→消える」バースト演出。 */
@Composable
private fun RecordBurst(
    mainText: String,
    subText: String?,
    color: Color,
    mainFontSize: androidx.compose.ui.unit.TextUnit,
    holdMillis: Long,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.15f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
        scale.animateTo(1f, tween(110))
        delay(holdMillis)
        alpha.animateTo(0f, tween(280))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.scale(scale.value).alpha(alpha.value),
    ) {
        GameText(mainText, fontSize = mainFontSize, color = color)
        if (subText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(color, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }
    }
}

/** プレイ中にハイスコアを初めて超えた瞬間の演出。以後もスコアが伸び続けることを煽る。 */
@Composable
fun HighScorePopupView(score: Int, modifier: Modifier = Modifier) {
    RecordBurst(
        mainText = "ハイスコア更新！",
        subText = "赤ちゃん${score}匹 まだ増える！",
        color = GoldColor,
        mainFontSize = 26.sp,
        holdMillis = 700,
        modifier = modifier,
    )
}

/** プレイ中にこれまでの最深到達レベルを初めて超えた瞬間の演出。 */
@Composable
fun LevelRecordPopupView(level: Int, modifier: Modifier = Modifier) {
    RecordBurst(
        mainText = "未踏エリア突入！",
        subText = "Lv.$level 自己記録更新中",
        color = Neon2,
        mainFontSize = 24.sp,
        holdMillis = 700,
        modifier = modifier,
    )
}
