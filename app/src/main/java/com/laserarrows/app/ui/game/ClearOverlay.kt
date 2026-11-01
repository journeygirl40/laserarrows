package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.components.isLandscape
import androidx.compose.foundation.layout.fillMaxHeight
import com.laserarrows.app.ui.components.CenteredScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
    population: Int,
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

    // 「次のレベルへ」がいちばん押されるボタンなので、ボタンの先頭に幅いっぱいの大きなボタンで置き、
    // ボタン群は枠の下に固定して、結果の情報がどれだけ長くてもスクロールせずに押せるようにする。ホーム・お気に入りはその下に小さく並べる。
    // 最後の1組をタップした指のまま押してしまわないよう、出てから少しの間は押せないようにしておく。
    var actionsReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ACTION_GUARD_MS)
        actionsReady = true
    }

    // 結果の要約(CLEAR!・★・スコア・記録)。
    val summary = @Composable {
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
    }
    // 森のなかまと、お気に入りのメモ入力。
    val extras = @Composable {
            if (populationGain > 0) {
                ForestGainCard(gain = populationGain, population = population)
            }

            if (!isImported && addingFavorite && !addedFavorite) {
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
                            addingFavorite = false
                        },
                    )
                    GhostButton("キャンセル", onClick = { addingFavorite = false; memo = "" })
                }
            }
    }
    // ボタン。メインの「次のレベルへ」が先頭、その下にサブ。
    val actions = @Composable {
        if (!isImported) {
            // メインの行き先: 次のレベルへ(いちばん大きく、ボタンの先頭)。
            NeonButton("次のレベルへ ▶", onClick = onNext, enabled = actionsReady, modifier = Modifier.fillMaxWidth())
            // サブの行き先: ホームへ / お気に入りへ登録(小さめ、次へのすぐ下)。
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton(homeLabel, onClick = onHome, enabled = actionsReady, compact = true, modifier = Modifier.weight(1f))
                GhostButton(
                    when {
                        addedFavorite -> "⭐ 登録済み"
                        else -> "⭐ お気に入り"
                    },
                    onClick = { addingFavorite = true },
                    enabled = actionsReady && !addedFavorite && !addingFavorite,
                    compact = true,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            // 「次」が無い盤面(自作のテスト・お気に入り)では、戻るボタンが主役。
            NeonButton(homeLabel, onClick = onHome, enabled = actionsReady, modifier = Modifier.fillMaxWidth())
        }
    }

    if (isLandscape()) {
        // 横向き: 左に要約、右に森のなかまとボタン(低い画面でもスクロールせずに全部見えるように)。
        Row(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CenteredScroll(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { summary() }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CenteredScroll(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { extras() }
                }
                actions()
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 結果の情報。記録更新チップやメモ入力が増えて入りきらない時は、ここだけスクロールする。
            CenteredScroll(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary()
                    extras()
                }
            }
            actions()
        }
    }
}

/** クリア画面が出てから、ボタンを押せるようになるまでの時間(最後のタップのまま押してしまうのを防ぐ)。 */
private const val ACTION_GUARD_MS = 600L

/** このクリアで森のなかまに加わった赤ちゃんと、森が次に広がるまでの残り。 */
@Composable
private fun ForestGainCard(gain: Int, population: Int) {
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
        Text("🌳 森のなかま +${gain}匹", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        if (next != null) {
            ForestProgressBar(((population - stage.minScore).toFloat() / (next.minScore - stage.minScore)).coerceIn(0f, 1f), ForestGreen)
            Text("あと${next.minScore - population}匹で森が広がる(「${next.name}」)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        } else {
            Text("いちばん広い森です", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldText)
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
