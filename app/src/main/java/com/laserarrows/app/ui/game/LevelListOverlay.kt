package com.laserarrows.app.ui.game

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.engine.PuzzleGenerator
import com.laserarrows.app.engine.ScoreRules
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.theme.Bg1
import com.laserarrows.app.ui.theme.Bg3
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon1
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

@Composable
fun LevelListOverlay(
    clearedLevels: Set<Int>,
    levelBestLivesLost: Map<Int, Int>,
    levelBestScore: Map<Int, Int>,
    hasActiveRun: Boolean,
    activeLevel: Int,
    initialExcludePerfect: Boolean = false,
    sortDescending: Boolean,
    onToggleSort: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    onResetRatings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxLv = clearedLevels.maxOrNull() ?: 0
    val upperBound = maxOf(maxLv + 1, 30)
    var excludePerfect by remember { mutableStateOf(initialExcludePerfect) }
    var confirmingReset by remember { mutableStateOf(false) }
    // レベル一覧からの選択は必ず新規ランを開始する(newRun)ため、再開可能なランが残っていると気付かず上書きされてしまう。
    // そのため選択前に一度だけ確認を挟み、誤タップで進行中のランを失わないようにする。
    var confirmingLevel by remember { mutableStateOf<Int?>(null) }
    val visibleLevels = remember(upperBound, excludePerfect, sortDescending, clearedLevels, levelBestLivesLost) {
        val base = (1..upperBound).filter { lv ->
            val isPerfect = lv in clearedLevels && (levelBestLivesLost[lv] == null || levelBestLivesLost[lv] == 0)
            !(excludePerfect && isPerfect)
        }
        if (sortDescending) base.sortedDescending() else base
    }
    // スコア・コンボはレベルをまたいで引き継がれる仕様なので、レベルNの理論上の最高得点は
    // 「レベル1からノーミスで積み上げたコンボのままレベルNもノーミスだった場合の累積点」で計算する。
    val cumulativeMaxScore = remember(upperBound) {
        var pairs = 0
        (1..upperBound).associateWith { lv ->
            pairs += PuzzleGenerator.levelSize(lv).numPairs
            ScoreRules.maxScoreForPairs(pairs)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg1, Bg3))),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 20.dp)
                .background(
                    Brush.radialGradient(listOf(Neon1.copy(alpha = 0.28f), Color.Transparent)),
                    RoundedCornerShape(999.dp),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("🎢 レベル一覧", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "次の一手を選ぶたび、少しだけ未来が開いていく。",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Start,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onToggleSort)
                        .background(Neon2.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .border(1.dp, Neon2, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (sortDescending) "⬇ 新しい順" else "⬆ 古い順",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Neon2,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { excludePerfect = !excludePerfect }
                        .background(
                            if (excludePerfect) GoldColor.copy(alpha = 0.18f) else Color.Transparent,
                            RoundedCornerShape(999.dp),
                        )
                        .border(1.dp, if (excludePerfect) GoldColor else GlassBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "✨ パーフェクトを除外",
                        fontSize = 12.sp,
                        fontWeight = if (excludePerfect) FontWeight.Bold else FontWeight.Normal,
                        color = if (excludePerfect) GoldColor else TextMuted,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).widthIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibleLevels) { lv ->
                    LevelListItem(
                        level = lv,
                        cleared = lv in clearedLevels,
                        bestLivesLost = levelBestLivesLost[lv],
                        bestScore = levelBestScore[lv],
                        maxScore = cumulativeMaxScore[lv] ?: 0,
                        isNext = lv == maxLv + 1,
                        onClick = {
                            if (hasActiveRun) {
                                confirmingLevel = lv
                            } else {
                                onSelectLevel(lv)
                            }
                        },
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                GhostButton(
                    text = if (confirmingReset) "本当にリセットする" else "🗑 評価をリセット",
                    onClick = {
                        if (confirmingReset) {
                            onResetRatings()
                            confirmingReset = false
                        } else {
                            confirmingReset = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                GhostButton("戻る", onClick = onBack, modifier = Modifier.weight(1f))
            }
        }

        confirmingLevel?.let { lv ->
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
                    Text("Lv.$lv に挑戦しますか？", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "現在プレイ中のLv.${activeLevel}の進行状況は消えます。",
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("キャンセル", onClick = { confirmingLevel = null }, modifier = Modifier.weight(1f))
                        GhostButton(
                            "挑戦する",
                            onClick = {
                                onSelectLevel(lv)
                                confirmingLevel = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelListItem(
    level: Int,
    cleared: Boolean,
    bestLivesLost: Int?,
    bestScore: Int?,
    maxScore: Int,
    isNext: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = when {
        isNext -> Neon2
        cleared -> GoldColor
        else -> GlassBorder
    }
    val fillBrush = when {
        cleared && (bestLivesLost == null || bestLivesLost == 0) -> Brush.linearGradient(listOf(Neon1.copy(alpha = 0.24f), PurpleAccent.copy(alpha = 0.24f)))
        cleared -> Brush.linearGradient(listOf(GoldColor.copy(alpha = 0.16f), DangerColor.copy(alpha = 0.12f)))
        isNext -> Brush.linearGradient(listOf(Neon2.copy(alpha = 0.16f), Neon1.copy(alpha = 0.16f)))
        else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.01f)))
    }
    val badge = when {
        cleared && (bestLivesLost == null || bestLivesLost == 0) -> "✨ パーフェクト"
        cleared -> "💔×$bestLivesLost"
        isNext -> "➡️ 次の挑戦"
        else -> "まだ未挑戦"
    }
    val badgeColor = when {
        cleared && bestLivesLost != null && bestLivesLost > 0 -> DangerColor
        cleared -> GoldColor
        isNext -> Neon2
        else -> TextMuted
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(fillBrush, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Lv.$level", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Text(
                    if (cleared) "クリア済み" else if (isNext) "次のステージ" else "未挑戦",
                    fontSize = 12.sp,
                    color = TextMuted,
                )
                Text(
                    "🏆 ${bestScore?.toString() ?: "―"} / ${maxScore}点",
                    fontSize = 11.sp,
                    color = TextMuted,
                )
            }
            Text(badge, fontSize = 12.5.sp, color = badgeColor, fontWeight = FontWeight.SemiBold)
        }
    }
}
