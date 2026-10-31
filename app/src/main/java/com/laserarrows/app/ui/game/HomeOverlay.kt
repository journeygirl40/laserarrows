package com.laserarrows.app.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon1
import com.laserarrows.app.ui.theme.Neon1Edge
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.WoodDark

private enum class ChallengeMode { HIGH_SCORE, LEVEL_UP }

@Composable
fun HomeOverlay(
    bestScore: Int,
    forestPopulation: Int,
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TitleLogo()
                Spacer(modifier = Modifier.height(6.dp))
                GameText("アニマル・リンク", fontSize = 38.sp, color = Color(0xFFFFF6DE), outline = WoodDark)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "なかまを出会わせて、赤ちゃんを増やそう",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier
                        .background(GlassFill.copy(alpha = 0.85f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ForestCard(population = forestPopulation, totalStars = totalStars, modifier = Modifier.fillMaxWidth())

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
                        tagline = "コンボを繋いで自己ベスト更新",
                        badgeText = "ベスト ${bestScore}匹",
                        accentColor = PurpleAccent,
                        onClick = { onCardTap(ChallengeMode.HIGH_SCORE) },
                        modifier = Modifier.weight(1f),
                    )
                    ChallengeCard(
                        icon = R.drawable.icon_rocket,
                        title = "レベルアップ\nチャレンジ",
                        tagline = "コンボ気にせずどんどん先へ",
                        badgeText = "Lv.$nextLevel から",
                        accentColor = Neon2,
                        onClick = { onCardTap(ChallengeMode.LEVEL_UP) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MenuTile(R.drawable.icon_scroll, "レベル一覧", onShowList, Modifier.weight(1f))
                    MenuTile(R.drawable.icon_bookmark, "お気に入り", onShowFavorites, Modifier.weight(1f))
                    MenuTile(R.drawable.icon_tools, "自分で作る", onShowEditor, Modifier.weight(1f))
                }
            }
        }

        confirmingMode?.let { mode ->
            val modeTitle = if (mode == ChallengeMode.HIGH_SCORE) "ハイスコアチャレンジ" else "レベルアップチャレンジ"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x993B2414)),
                contentAlignment = Alignment.Center,
            ) {
                CreamCard(modifier = Modifier.widthIn(max = 310.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("${modeTitle}を始めますか？", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        Text(
                            "現在プレイ中のLv.${activeLevel}(赤ちゃん${activeScore}匹)の進行状況は消えます。",
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

/** これまでに生まれた赤ちゃんの累計を「森のなかま」として見せる(点数が積み上がって森が育つ実感を出す)。 */
@Composable
private fun ForestCard(population: Int, totalStars: Int, modifier: Modifier = Modifier) {
    CreamCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.icon_tree), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("森のなかま", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Text("${population}匹", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(GoldColor.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Image(painterResource(R.drawable.icon_star), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$totalStars", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }
        }
    }
}

/** タイトルロゴ: 3Dの動物たちがハートを挟んで寄り添い、ゆらゆら弾む(遊ぶ前から「出会い」の題材を伝える)。 */
@Composable
private fun TitleLogo() {
    val transition = rememberInfiniteTransition(label = "logo")
    val bob by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "logoBob",
    )
    val heartPulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(animation = tween(700, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "logoHeart",
    )
    Box(modifier = Modifier.size(width = 230.dp, height = 120.dp), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.animal_penguin),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 6.dp, y = (14 + bob).dp).size(72.dp).rotate(-10f),
        )
        Image(
            painterResource(R.drawable.animal_rabbit),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-6).dp, y = (14 - bob).dp).size(72.dp).rotate(10f),
        )
        Image(
            painterResource(R.drawable.icon_sparkling_heart),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 4.dp).size(54.dp).scale(heartPulse),
        )
        Image(
            painterResource(R.drawable.animal_fox),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-bob / 2).dp).size(84.dp),
        )
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
                    "Lv.$level ・ 赤ちゃん${score}匹 ・ ライフ×$lives",
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
            Text(tagline, fontSize = 10.sp, color = TextMuted, textAlign = TextAlign.Center)
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

/** ホーム下段の小さな入口(アイコン+名前の正方形タイル)。 */
@Composable
private fun MenuTile(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CreamCard(modifier = modifier, onClick = onClick, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }
    }
}
