package com.laserarrows.app.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.ui.components.ChunkySurface
import com.laserarrows.app.ui.components.CreamCard
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.components.CenteredScroll
import com.laserarrows.app.ui.components.isLandscape
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon1
import com.laserarrows.app.ui.theme.Neon1Edge
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.WoodDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@Composable
fun HomeOverlay(
    bestScore: Int,
    forestPopulation: Int,
    forestStageSeen: Int?,
    onForestStageCelebrated: (level: Int) -> Unit,
    forestExpansionSeen: Int?,
    onForestExpansionCelebrated: (level: Int) -> Unit,
    forestHiddenDecor: Set<String>,
    onForestDecorToggle: (key: String, visible: Boolean) -> Unit,
    totalStars: Int,
    nextLevel: Int,
    hasActiveRun: Boolean,
    activeLevel: Int,
    activeScore: Int,
    activeLives: Int,
    onStartNext: () -> Unit,
    onResume: () -> Unit,
    onShowList: () -> Unit,
    onShowFavorites: () -> Unit,
    onShowEditor: () -> Unit,
    animalsUnlocked: Boolean,
    onShowAnimals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingStart by remember { mutableStateOf(false) }

    // 森の段階は、届いただけでは育たない。届いた分は「ごほうび」として森の上に出し、タップした時に解放してお祝いを見せる。
    // 集めた★で「にぎやかになった!」、森のなかまの累計で「森が広がった!」。両方ある時は順番に見せる。
    val reachedLively = ForestStage.of(totalStars).level
    val reachedWide = ForestExpansion.of(forestPopulation).level
    // 森に見せるのは、解放済みの段階まで(読み込み前は届いた段階のまま)。
    val forestLevel = forestStageSeen?.coerceAtMost(reachedLively) ?: reachedLively
    val expansionLevel = forestExpansionSeen?.coerceAtMost(reachedWide) ?: reachedWide
    val pendingLively = forestStageSeen != null && reachedLively > forestStageSeen
    val pendingWide = forestExpansionSeen != null && reachedWide > forestExpansionSeen
    val pending = remember { mutableStateListOf<ForestCelebration>() }
    var celebration by remember { mutableStateOf<ForestCelebration?>(null) }
    var showDecor by remember { mutableStateOf(false) }
    fun unlockRewards() {
        if (pending.isNotEmpty()) return
        if (pendingLively) pending += ForestCelebration(ForestGrowth.LIVELY, forestStageSeen!! + 1, reachedLively)
        if (pendingWide) pending += ForestCelebration(ForestGrowth.WIDE, forestExpansionSeen!! + 1, reachedWide)
    }
    LaunchedEffect(Unit) {
        while (true) {
            val next = snapshotFlow { pending.firstOrNull() }.filterNotNull().first()
            celebration = next
            when (next.kind) {
                ForestGrowth.LIVELY -> onForestStageCelebrated(next.to)
                ForestGrowth.WIDE -> onForestExpansionCelebrated(next.to)
            }
            delay(FOREST_CELEBRATION_MS)
            pending.remove(next)
            if (pending.isEmpty()) celebration = null
        }
    }

    // 遊ぶ入口は「続きから」と「Lv.○から遊ぶ」の2つだけ。ラン中に新しく始めると今の進行状況が消えるため、その時だけ一度確認を挟む。
    fun onStartTap() {
        if (hasActiveRun) confirmingStart = true else onStartNext()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 縦向き: 森とタイトルを上に、遊ぶボタンを下に。横向き: 森とタイトルを左に、遊ぶボタンを右に並べる
        // (低い画面でもはみ出さないよう、それぞれ縦にスクロールできるようにしておく)。
        val landscape = isLandscape()
        val header = @Composable {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    TitleForest(level = forestLevel, expansion = expansionLevel, celebration = celebration, hidden = forestHiddenDecor)
                    if ((pendingLively || pendingWide) && pending.isEmpty()) {
                        ForestRewardGift(onClick = { unlockRewards() }, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                GameText("アニマル・リンク", fontSize = if (landscape) 30.sp else 38.sp, color = Color(0xFFFFF6DE), outline = WoodDark)
                Spacer(modifier = Modifier.height(8.dp))
                ForestProgressPill(totalStars = totalStars, population = forestPopulation)
                // 解放したかざりが1つでもあれば、つける/外すを選べる入口を出す。
                if (forestLevel >= 2 || expansionLevel >= 1) {
                    Text(
                        "🎨 森のかざりを選ぶ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(GlassFill.copy(alpha = 0.88f), RoundedCornerShape(999.dp))
                            .clickable { showDecor = true }
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    )
                }
            }
        }
        val actions = @Composable {
            Column(
                verticalArrangement = Arrangement.spacedBy(if (landscape) 8.dp else 12.dp),
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
                }

                StartCard(
                    nextLevel = nextLevel,
                    bestScore = bestScore,
                    // 続きがある時は「続きから」が主役なので、こちらは「新しく始める」と分かる言い方にして一段控えめにする。
                    secondary = hasActiveRun,
                    compact = landscape,
                    onClick = { onStartTap() },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MenuTile(R.drawable.icon_scroll, "レベル一覧", onShowList, Modifier.weight(1f), badge = if (totalStars > 0) "★$totalStars" else null)
                    MenuTile(R.drawable.icon_bookmark, "お気に入り", onShowFavorites, Modifier.weight(1f))
                    MenuTile(R.drawable.icon_tools, "自分で作る", onShowEditor, Modifier.weight(1f))
                    // どうぶつの設定は最初から見えているが、Lv.20に到達するまでは鍵がかかっていて開けない(押すと解放条件を知らせる)。
                    MenuTile(R.drawable.icon_paw, "どうぶつ", onShowAnimals, Modifier.weight(1f), locked = !animalsUnlocked)
                }
            }
        }
        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CenteredScroll(modifier = Modifier.weight(1f).fillMaxHeight()) { header() }
                CenteredScroll(modifier = Modifier.weight(1f).fillMaxHeight()) { actions() }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                header()
                Spacer(modifier = Modifier.weight(1f))
                actions()
            }
        }
        if (showDecor) {
            ForestDecorDialog(
                livelyUnlocked = forestLevel,
                wideUnlocked = expansionLevel,
                hidden = forestHiddenDecor,
                onToggle = onForestDecorToggle,
                onClose = { showDecor = false },
            )
        }
        if (confirmingStart) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x993B2414)),
            ) {
                CenteredScroll(modifier = Modifier.fillMaxSize()) {
                    CreamCard(modifier = Modifier.widthIn(max = 310.dp)) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Lv.${nextLevel}から新しく始めますか？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                            Text(
                                "現在プレイ中のLv.${activeLevel}(なかま${activeScore}匹)の進行状況は消えます。",
                                fontSize = 12.5.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GhostButton("キャンセル", onClick = { confirmingStart = false }, modifier = Modifier.weight(1f))
                                NeonButton(
                                    "始める",
                                    onClick = {
                                        onStartNext()
                                        confirmingStart = false
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
    ChunkySurface(face = Neon1, edge = Neon1Edge, modifier = modifier, onClick = onClick, shape = RoundedCornerShape(22.dp), edgeHeight = 6.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("▶", fontSize = 20.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                GameText("続きから", fontSize = 22.sp, color = Color.White, outline = Neon1Edge, textAlign = TextAlign.Start)
                Text(
                    // ゲームオーバー中のランは、広告を見て復活しないと遊べないことを先に伝える。
                    if (lives <= 0) "Lv.$level ・ なかま${score}匹 ・ 💔 広告を見て復活" else "Lv.$level ・ なかま${score}匹 ・ ライフ×$lives",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * 新しく遊び始める入口(ひとつだけ)。まだクリアしていない次のレベルから始まる。
 * ハイスコアもここで伸ばすので自己ベストを札で見せ、遊び直したいレベルは「レベル一覧」へ案内する。
 * 続きがある時(secondary)は「続きから」が主役なので、「新しく始める」と分かる言い方で一段控えめに見せる。
 */
@Composable
private fun StartCard(
    nextLevel: Int,
    bestScore: Int,
    secondary: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CreamCard(modifier = modifier, onClick = onClick, contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = if (compact || secondary) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.icon_rocket), contentDescription = null, modifier = Modifier.size(if (secondary) 34.dp else 46.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (secondary) "Lv.${nextLevel}から新しく始める" else "Lv.${nextLevel}から遊ぶ",
                    fontSize = if (secondary) 15.sp else 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                )
                // 横向きの低い画面では、ひとこと説明を省いてカードを低くする。
                if (!compact) {
                    Text("まだクリアしていない次のレベルから。遊び直しは「レベル一覧」で", fontSize = 10.5.sp, color = TextMuted)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "ベスト\n${bestScore}匹",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                modifier = Modifier
                    .background(PurpleAccent, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * ホーム下段の小さな入口(アイコン+名前の正方形タイル)。badgeは右上の小さな札(★の数など)。
 * lockedの時はアイコンを薄くして鍵をつける(押せるが、開く代わりに解放条件を知らせる)。
 */
@Composable
private fun MenuTile(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    locked: Boolean = false,
) {
    CreamCard(modifier = modifier, onClick = onClick, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp).alpha(if (locked) 0.4f else 1f),
                )
                if (locked) {
                    Image(
                        painterResource(R.drawable.icon_lock),
                        contentDescription = "未解放",
                        modifier = Modifier.size(20.dp).align(Alignment.BottomEnd),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (locked) TextMuted else TextPrimary, maxLines = 1)
        }
        if (badge != null) {
            Text(
                badge,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(GoldColor, RoundedCornerShape(999.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}
