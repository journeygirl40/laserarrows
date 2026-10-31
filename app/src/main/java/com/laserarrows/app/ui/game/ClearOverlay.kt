package com.laserarrows.app.ui.game

import com.laserarrows.app.R
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.laserarrows.app.ui.theme.WoodDark
import com.laserarrows.app.ui.components.GameText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.GoldText
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun ClearOverlay(
    totalScore: Int,
    livesLost: Int,
    levelScore: Int,
    par: Int,
    stars: Int,
    isEditorTest: Boolean,
    isImported: Boolean,
    crossedHighScore: Boolean,
    crossedLevelRecord: Boolean,
    level: Int,
    onNext: () -> Unit,
    onHome: () -> Unit,
    onAddToFavorites: (memo: String) -> Unit,
    populationGain: Int,
    doubled: Boolean,
    population: Int,
    onWatchAdToDouble: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val perfect = livesLost == 0
    val badgeColor = if (perfect) GoldText else DangerColor
    val badgeText = if (perfect) "✨ パーフェクトクリア!" else "💔×$livesLost 消費してクリア"
    val homeLabel = when {
        isEditorTest -> "✏️ 編集に戻る"
        isImported -> "⭐ お気に入りに戻る"
        else -> "🏠 ホームへ"
    }
    var addingFavorite by remember { mutableStateOf(false) }
    var addedFavorite by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }

    Column(
        // 記録更新チップや★が増えても盤面パネル内に収まりきらない場合に備え、スクロール可能にしておく。
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GameText("CLEAR!", fontSize = 34.sp, color = GoldColor, outline = WoodDark)
        AnimatedStars(stars)
        Text(
            if (levelScore >= par) "目標達成! なかま${levelScore}匹 / 目標${par}匹" else "なかま${levelScore}匹 / 目標${par}匹 (★3まであと${par - levelScore}匹)",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (levelScore >= par) GoldText else TextPrimary,
        )
        Text("ここまでの合計 なかま${totalScore}匹", fontSize = 12.sp, color = TextMuted)

        Box(
            modifier = Modifier
                .background(badgeColor.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(badgeText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = badgeColor)
        }
        if (!isImported && (crossedHighScore || crossedLevelRecord)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (crossedHighScore) {
                    RecordChip("🏆 ハイスコア更新", GoldText)
                }
                if (crossedLevelRecord) {
                    RecordChip("🚀 Lv.$level 到達レベル記録更新", Neon2)
                }
            }
        }

        if (populationGain > 0) {
            ForestGainCard(gain = populationGain, doubled = doubled, population = population, onWatchAdToDouble = onWatchAdToDouble)
        }

        if (!isImported) {
            when {
                addedFavorite -> {
                    Text("⭐ お気に入りに登録しました", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldText)
                }
                addingFavorite -> {
                    OutlinedTextField(
                        value = memo,
                        onValueChange = { memo = it },
                        placeholder = { Text("メモ(任意・例:誰との対戦か)", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.widthIn(max = 280.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            "⭐ 登録する",
                            onClick = {
                                onAddToFavorites(memo)
                                addedFavorite = true
                            },
                        )
                        GhostButton("キャンセル", onClick = { addingFavorite = false; memo = "" })
                    }
                }
                else -> {
                    GhostButton("⭐ お気に入りへ登録", onClick = { addingFavorite = true })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("次のレベルへ", onClick = onNext, modifier = Modifier.weight(1f))
                GhostButton(homeLabel, onClick = onHome, modifier = Modifier.weight(1f))
            }
        } else {
            GhostButton(homeLabel, onClick = onHome)
        }
    }
}

/**
 * このクリアで森のなかまに加わった赤ちゃんと、森が次に広がるまでの残り。
 * 広告を見ると同じ数をもう一度もらえる(2倍)ことを、森が早く広がるご褒美として見せる。
 */
@Composable
private fun ForestGainCard(gain: Int, doubled: Boolean, population: Int, onWatchAdToDouble: () -> Unit) {
    val stage = ForestExpansion.of(population)
    val next = ForestExpansion.next(stage)
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .widthIn(max = 300.dp)
            .background(ForestGreen.copy(alpha = 0.16f), shape)
            .border(1.dp, ForestGreen.copy(alpha = 0.6f), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🌳 森のなかま +${if (doubled) gain * 2 else gain}匹", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            if (doubled) RecordChip("2倍!", GoldText)
        }
        if (next != null) {
            ForestProgressBar(((population - stage.minScore).toFloat() / (next.minScore - stage.minScore)).coerceIn(0f, 1f), ForestGreen)
            Text("あと${next.minScore - population}匹で森が広がる(「${next.name}」)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        } else {
            Text("いちばん広い森です", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldText)
        }
        if (!doubled) {
            GhostButton("📺 広告を見て2倍に(+${gain}匹)", onClick = onWatchAdToDouble)
        }
    }
}

@Composable
fun RecordChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/** ★を1つずつ順番に「ポン」と点灯させる(獲得★の手応えを強調する)。 */
@Composable
private fun AnimatedStars(stars: Int) {
    val scales = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(stars) {
        scales.forEachIndexed { i, anim ->
            delay(if (i == 0) 250L else 220L)
            anim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        scales.forEachIndexed { i, anim ->
            val lit = i < stars
            // 獲得した★は3Dの星、未獲得はグレーの影だけを見せる(真ん中を少し大きく、王冠のように並べる)。
            Image(
                painterResource(R.drawable.icon_star),
                contentDescription = null,
                colorFilter = if (lit) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                modifier = Modifier
                    .size(if (i == 1) 58.dp else 46.dp)
                    .offset(y = if (i == 1) (-6).dp else 0.dp)
                    .alpha(if (lit) 1f else 0.35f)
                    .scale(if (lit) anim.value else 1f),
            )
        }
    }
}
