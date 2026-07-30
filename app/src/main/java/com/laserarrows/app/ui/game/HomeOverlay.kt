package com.laserarrows.app.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.theme.Bg1
import com.laserarrows.app.ui.theme.Bg3
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon1
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

private enum class ChallengeMode { HIGH_SCORE, LEVEL_UP }

@Composable
fun HomeOverlay(
    bestScore: Int,
    nextLevel: Int,
    hasActiveRun: Boolean,
    activeLevel: Int,
    activeScore: Int,
    activeLives: Int,
    onHighScoreChallenge: () -> Unit,
    onLevelUpChallenge: () -> Unit,
    onResume: () -> Unit,
    onShowList: () -> Unit,
    onShowFavorites: () -> Unit,
    onShowEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingMode by remember { mutableStateOf<ChallengeMode?>(null) }

    fun startMode(mode: ChallengeMode) {
        when (mode) {
            ChallengeMode.HIGH_SCORE -> onHighScoreChallenge()
            ChallengeMode.LEVEL_UP -> onLevelUpChallenge()
        }
    }

    // ラン中に別モードを選ぶと今の進行状況が消えるため、その時だけ一度確認を挟む。
    fun onCardTap(mode: ChallengeMode) {
        if (hasActiveRun) confirmingMode = mode else startMode(mode)
    }

    val bgTransition = rememberInfiniteTransition(label = "homeBg")
    val topGlowAlpha by bgTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.36f,
        animationSpec = infiniteRepeatable(animation = tween(2600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "topGlowAlpha",
    )
    val bottomGlowAlpha by bgTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(animation = tween(3200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "bottomGlowAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg1, Bg3))),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(listOf(Neon1.copy(alpha = topGlowAlpha), Color.Transparent)),
                    CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 4.dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(listOf(PurpleAccent.copy(alpha = bottomGlowAlpha), Color.Transparent)),
                    CircleShape,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 28.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TitleLogo()
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    "レーザー・リンク",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(color = Neon1.copy(alpha = 0.75f), blurRadius = 28f),
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, Neon2, PurpleAccent, Color.Transparent)),
                            RoundedCornerShape(999.dp),
                        ),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "色を読んで、光をつなげ。",
                    fontSize = 13.sp,
                    color = TextMuted,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (hasActiveRun) {
                    ContinueButton(
                        level = activeLevel,
                        score = activeScore,
                        lives = activeLives,
                        onClick = onResume,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "新しいチャレンジを始める",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ChallengeCard(
                        emoji = "🏆",
                        title = "ハイスコア\nチャレンジ",
                        tagline = "コンボを繋いで自己ベスト更新",
                        badgeText = "ベスト ${bestScore}点",
                        accentColor = GoldColor,
                        onClick = { onCardTap(ChallengeMode.HIGH_SCORE) },
                        modifier = Modifier.weight(1f),
                    )
                    ChallengeCard(
                        emoji = "🚀",
                        title = "レベルアップ\nチャレンジ",
                        tagline = "コンボ気にせずどんどん先へ",
                        badgeText = "Lv.$nextLevel から挑戦",
                        accentColor = Neon2,
                        onClick = { onCardTap(ChallengeMode.LEVEL_UP) },
                        modifier = Modifier.weight(1f),
                    )
                }

                GhostButton("📜 レベル一覧", onClick = onShowList, modifier = Modifier.fillMaxWidth())
                GhostButton("⭐ お気に入り", onClick = onShowFavorites, modifier = Modifier.fillMaxWidth())
                GhostButton("🛠 自分で作る", onClick = onShowEditor, modifier = Modifier.fillMaxWidth())
            }
        }

        confirmingMode?.let { mode ->
            val modeTitle = if (mode == ChallengeMode.HIGH_SCORE) "ハイスコアチャレンジ" else "レベルアップチャレンジ"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0A0814)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .background(Bg3, RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("${modeTitle}を始めますか？", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "現在プレイ中のLv.${activeLevel}(スコア${activeScore}点)の進行状況は消えます。",
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("キャンセル", onClick = { confirmingMode = null }, modifier = Modifier.weight(1f))
                        GhostButton(
                            "始める",
                            onClick = {
                                startMode(mode)
                                confirmingMode = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** タイトルロゴ: 回転する軌道リング+軌道パーティクル+パルスするコアで、ゲームのレーザー演出を静かに予感させる。 */
@Composable
private fun TitleLogo() {
    val transition = rememberInfiniteTransition(label = "logo")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing)),
        label = "logoRotation",
    )
    val corePulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "logoCorePulse",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "logoGlowAlpha",
    )

    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .background(Brush.radialGradient(listOf(Neon1.copy(alpha = glowAlpha * 0.5f), Color.Transparent))),
        )
        Canvas(
            modifier = Modifier
                .size(92.dp)
                .rotate(rotation),
        ) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Neon2, PurpleAccent, Neon1, Neon2)),
                startAngle = -40f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(center.x, center.y - radius))
            drawCircle(color = Neon2, radius = 2.2.dp.toPx(), center = Offset(center.x + radius * 0.87f, center.y + radius * 0.5f))
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(corePulse)
                .background(Brush.linearGradient(listOf(Neon1, PurpleAccent)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚡", fontSize = 30.sp)
        }
    }
}

/** 進行中のランへ戻る、最も押しやすい主役ボタン(今どこまで進んでいるかを見せて「再開したい」気持ちを後押しする)。 */
@Composable
private fun ContinueButton(
    level: Int,
    score: Int,
    lives: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .background(Brush.linearGradient(listOf(Neon1, PurpleAccent)), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 16.dp),
    ) {
        Text("▶ 続きから", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text(
            "Lv.$level ・ スコア${score}点 ・ ライフ×$lives",
            fontSize = 12.5.sp,
            color = TextPrimary.copy(alpha = 0.85f),
        )
    }
}

/** モード選択カード。関連する自己記録をバッジで見せ、「これを超えたい」という気持ちを起点にモードを選んでもらう。 */
@Composable
private fun ChallengeCard(
    emoji: String,
    title: String,
    tagline: String,
    badgeText: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.22f), accentColor.copy(alpha = 0.06f))),
                shape,
            )
            .border(1.5.dp, accentColor.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, fontSize = 28.sp)
        Text(
            title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
        )
        Text(tagline, fontSize = 10.sp, color = TextMuted, textAlign = TextAlign.Center)
        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}
