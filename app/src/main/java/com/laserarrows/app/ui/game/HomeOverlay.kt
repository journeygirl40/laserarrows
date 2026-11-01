package com.laserarrows.app.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

private enum class ChallengeMode { HIGH_SCORE, LEVEL_UP }

@Composable
fun HomeOverlay(
    bestScore: Int,
    forestPopulation: Int,
    forestStageSeen: Int?,
    onForestStageCelebrated: (level: Int) -> Unit,
    forestExpansionSeen: Int?,
    onForestExpansionCelebrated: (level: Int) -> Unit,
    totalStars: Int,
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
    animalsUnlocked: Boolean,
    onShowAnimals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingMode by remember { mutableStateOf<ChallengeMode?>(null) }

    // 森の段階が上がっていたら、ホームに戻ってきた時に一度だけお祝いを見せる。
    // 集めた★で「にぎやかになった!」、森のなかまの累計で「森が広がった!」。両方ある時は順番に見せる。
    val forestLevel = ForestStage.of(totalStars).level
    val expansionLevel = ForestExpansion.of(forestPopulation).level
    val pending = remember { mutableStateListOf<ForestCelebration>() }
    var celebration by remember { mutableStateOf<ForestCelebration?>(null) }
    fun enqueue(c: ForestCelebration) {
        pending.removeAll { it.kind == c.kind && it != celebration }
        if (c !in pending) pending += c
    }
    LaunchedEffect(forestLevel, forestStageSeen) {
        if (forestStageSeen != null && forestLevel > forestStageSeen) enqueue(ForestCelebration(ForestGrowth.LIVELY, forestStageSeen + 1, forestLevel))
    }
    LaunchedEffect(expansionLevel, forestExpansionSeen) {
        if (forestExpansionSeen != null && expansionLevel > forestExpansionSeen) {
            enqueue(ForestCelebration(ForestGrowth.WIDE, forestExpansionSeen + 1, expansionLevel))
        }
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
        }
    }

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

    Box(modifier = modifier.fillMaxSize()) {
        // 縦向き: 森とタイトルを上に、遊ぶボタンを下に。横向き: 森とタイトルを左に、遊ぶボタンを右に並べる
        // (低い画面でもはみ出さないよう、それぞれ縦にスクロールできるようにしておく)。
        val landscape = isLandscape()
        val header = @Composable {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TitleForest(level = forestLevel, expansion = expansionLevel, celebration = celebration)
                GameText("アニマル・リンク", fontSize = if (landscape) 30.sp else 38.sp, color = Color(0xFFFFF6DE), outline = WoodDark)
                Spacer(modifier = Modifier.height(8.dp))
                ForestProgressPill(totalStars = totalStars, population = forestPopulation)
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ChallengeCard(
                        icon = R.drawable.icon_trophy,
                        title = "ハイスコア\nチャレンジ",
                        tagline = "コンボを繋いでベストを更新しよう",
                        badgeText = "ベスト ${bestScore}匹",
                        accentColor = PurpleAccent,
                        compact = landscape,
                        onClick = { onCardTap(ChallengeMode.HIGH_SCORE) },
                        modifier = Modifier.weight(1f),
                    )
                    ChallengeCard(
                        icon = R.drawable.icon_rocket,
                        title = "レベルアップ\nチャレンジ",
                        tagline = "どんどん先へ進んで森を広げよう",
                        badgeText = "Lv.$nextLevel から",
                        accentColor = Neon2,
                        compact = landscape,
                        onClick = { onCardTap(ChallengeMode.LEVEL_UP) },
                        modifier = Modifier.weight(1f),
                    )
                }

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
        confirmingMode?.let { mode ->
            val modeTitle = if (mode == ChallengeMode.HIGH_SCORE) "ハイスコアチャレンジ" else "レベルアップチャレンジ"
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
                            Text("${modeTitle}を始めますか？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                            Text(
                                "現在プレイ中のLv.${activeLevel}(なかま${activeScore}匹)の進行状況は消えます。",
                                fontSize = 12.5.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GhostButton("キャンセル", onClick = { confirmingMode = null }, modifier = Modifier.weight(1f))
                                NeonButton(
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

/** モード選択カード。関連する自己記録をバッジで見せ、「これを超えたい」という気持ちを起点にモードを選んでもらう。 */
@Composable
private fun ChallengeCard(
    @DrawableRes icon: Int,
    title: String,
    tagline: String,
    badgeText: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    CreamCard(modifier = modifier, onClick = onClick, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(44.dp))
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
            // 横向きの低い画面では、ひとこと説明を省いてカードを低くする。
            if (!compact) Text(tagline, fontSize = 10.sp, color = TextMuted, textAlign = TextAlign.Center)
            Text(
                badgeText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .background(accentColor, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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
