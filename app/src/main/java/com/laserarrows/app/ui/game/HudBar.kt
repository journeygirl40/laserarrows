package com.laserarrows.app.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.GlassSurface
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

@Composable
fun HudBar(
    levelLabel: String,
    score: Int,
    combo: Int,
    lives: Int,
    bestScore: Int,
    crossedHighScore: Boolean,
    inLevelRecordTerritory: Boolean,
    scoreBonusPopup: ScoreBonusPopup?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudBlock(
                label = "LEVEL",
                value = levelLabel,
                valueColor = if (inLevelRecordTerritory) Neon2 else TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(1f)) {
                HudBlock(
                    label = "SCORE",
                    value = score.toString(),
                    valueColor = if (crossedHighScore) GoldColor else TextPrimary,
                )
                scoreBonusPopup?.let { popup ->
                    key(popup.token) {
                        ScoreBonusPopupView(
                            gained = popup.gained,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-16).dp),
                        )
                    }
                }
            }
            HudBlock(
                label = "COMBO",
                value = "×$combo",
                valueColor = GoldColor,
                modifier = Modifier.weight(1f),
            )
            HudBlock(
                label = "LIFE",
                value = "×$lives",
                valueColor = DangerColor,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        HighScoreProgressBar(score = score, bestScore = bestScore, crossedHighScore = crossedHighScore)
    }
}

/** 現在のスコアがハイスコアにどれだけ迫っているかを常に見せ、「あと少し」で更新を後押しする(ゴール勾配効果)。 */
@Composable
private fun HighScoreProgressBar(score: Int, bestScore: Int, crossedHighScore: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hudPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse),
        label = "hudPulseAlpha",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        val caption = when {
            bestScore <= 0 -> "初めての記録に挑戦中！"
            crossedHighScore -> "🏆 ハイスコア更新中！ +${score - bestScore}"
            else -> "ベストまであと ${(bestScore - score).coerceAtLeast(0)}点"
        }
        Text(
            caption,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (crossedHighScore) GoldColor else TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (crossedHighScore) Modifier.alpha(pulse) else Modifier,
        )
        Spacer(modifier = Modifier.height(3.dp))
        val fraction = if (bestScore <= 0) 1f else (score.toFloat() / bestScore).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(GlassFill),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            if (crossedHighScore) {
                                listOf(GoldColor.copy(alpha = 0.8f), GoldColor)
                            } else {
                                listOf(GoldColor.copy(alpha = 0.35f), GoldColor.copy(alpha = 0.7f))
                            },
                        ),
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}

@Composable
private fun HudBlock(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    GlassSurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, fontSize = 9.5.sp, color = TextMuted, letterSpacing = 1.sp, maxLines = 1)
            Text(
                value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
