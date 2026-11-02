package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.components.CenteredScroll
import com.laserarrows.app.ui.theme.WoodDark
import com.laserarrows.app.ui.theme.Neon1Edge
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.components.CreamCard
import com.laserarrows.app.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
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
import com.laserarrows.app.ui.components.AdaptiveScreen
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.theme.GlassFill
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
    levelStars: Map<Int, Int>,
    hasActiveRun: Boolean,
    activeLevel: Int,
    initialExcludePerfect: Boolean = false,
    sortDescending: Boolean,
    onToggleSort: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxLv = clearedLevels.maxOrNull() ?: 0
    // 選べるのは、クリア済みのレベルと次の1つ(nextLv)まで。その先は今の星座の終わりまで鍵つきで見せて、
    // 「ここを越えると次が開く」ことだけ伝える(未到達の先のレベルへ飛べないようにする)。
    val nextLv = maxLv + 1
    val upperBound = constellationLevels(constellationIndex(nextLv)).last
    var excludePerfect by remember { mutableStateOf(initialExcludePerfect) }
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
    // 5レベルごと(盤面サイズが同じ区間)を1つの星座としてまとめ、★を灯していく星図として見せる。
    val constellations = remember(visibleLevels) {
        val groups = linkedMapOf<Int, MutableList<Int>>()
        visibleLevels.forEach { lv -> groups.getOrPut(constellationIndex(lv)) { mutableListOf() } += lv }
        groups.toList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
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

        AdaptiveScreen(
            header = {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.icon_scroll), contentDescription = null, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        GameText("レベル一覧", fontSize = 28.sp, color = TextPrimary, textAlign = TextAlign.Start)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "クリアしたレベルの遊び直しと、次のレベルに挑戦できます。",
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
            },
            footer = { GhostButton("戻る", onClick = onBack, modifier = Modifier.fillMaxWidth()) },
        ) { modifier ->
            LazyColumn(
                modifier = modifier.widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                constellations.forEach { (index, levels) ->
                    item(key = "constellation-$index") {
                        ConstellationHeader(
                            index = index,
                            earnedStars = constellationLevels(index).sumOf { levelStars[it] ?: 0 },
                        )
                    }
                    items(levels, key = { it }) { lv ->
                        LevelListItem(
                            level = lv,
                            cleared = lv in clearedLevels,
                            bestLivesLost = levelBestLivesLost[lv],
                            bestScore = levelBestScore[lv],
                            stars = levelStars[lv] ?: 0,
                            isNext = lv == nextLv,
                            locked = lv > nextLv,
                            unlockHint = "Lv.${lv - 1}をクリアすると解放",
                            onClick = {
                                if (lv > nextLv) return@LevelListItem
                                if (hasActiveRun) {
                                    confirmingLevel = lv
                                } else {
                                    onSelectLevel(lv)
                                }
                            },
                        )
                    }
                }
            }
        }

        confirmingLevel?.let { lv ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x993B2414)),
            ) {
                CenteredScroll(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .background(GlassFill, RoundedCornerShape(20.dp))
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
}

@Composable
private fun LevelListItem(
    level: Int,
    cleared: Boolean,
    bestLivesLost: Int?,
    bestScore: Int?,
    stars: Int,
    isNext: Boolean,
    locked: Boolean,
    unlockHint: String,
    onClick: () -> Unit,
) {
    val perfect = cleared && (bestLivesLost == null || bestLivesLost == 0)
    val badgeColor = when {
        isNext -> Neon1
        cleared -> GoldColor
        else -> TextMuted.copy(alpha = 0.5f)
    }
    CreamCard(
        modifier = Modifier.fillMaxWidth().alpha(if (locked) 0.55f else 1f),
        onClick = if (locked) null else onClick,
        face = if (isNext) Color(0xFFF0FBE6) else GlassFill,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // レベル番号の丸バッジ(クリア済みは金、次の挑戦は緑、未挑戦は灰)。
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(badgeColor, CircleShape)
                    .border(2.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$level", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                if (locked) {
                    Image(
                        painterResource(R.drawable.icon_lock),
                        contentDescription = "未解放",
                        modifier = Modifier.size(20.dp).align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        locked -> unlockHint
                        isNext -> "次の挑戦！"
                        perfect -> "パーフェクト"
                        cleared -> "クリア(ライフ-$bestLivesLost)"
                        else -> "まだ未挑戦"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isNext) Neon1Edge else if (cleared && !perfect) DangerColor else TextPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.icon_paw), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ベスト ${bestScore?.toString() ?: "―"}匹", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                repeat(3) { i ->
                    val lit = i < stars
                    Image(
                        painterResource(R.drawable.icon_star),
                        contentDescription = null,
                        colorFilter = if (lit) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                        modifier = Modifier.size(26.dp).alpha(if (lit) 1f else 0.3f),
                    )
                }
            }
        }
    }
}

private val CONSTELLATION_NAMES = listOf(
    "オリオン座", "カシオペヤ座", "はくちょう座", "こと座", "さそり座",
    "わし座", "ペガスス座", "アンドロメダ座", "ふたご座", "しし座",
    "おおぐま座", "いて座",
)

private fun constellationIndex(level: Int): Int = (level - 1) / 5

private fun constellationLevels(index: Int): IntRange = (index * 5 + 1)..(index * 5 + 5)

private fun constellationName(index: Int): String {
    val base = CONSTELLATION_NAMES[index % CONSTELLATION_NAMES.size]
    val lap = index / CONSTELLATION_NAMES.size
    return if (lap == 0) base else "$base ${lap + 1}"
}

@Composable
private fun ConstellationHeader(index: Int, earnedStars: Int) {
    val levels = constellationLevels(index)
    val maxStars = levels.count() * 3
    val complete = earnedStars >= maxStars
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .background(if (complete) GoldColor else WoodDark, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "✦ ${constellationName(index)}  Lv.${levels.first}–${levels.last}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.icon_star), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("$earnedStars / $maxStars", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}
