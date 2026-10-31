package com.laserarrows.app.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.ui.components.CreamCard
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.EmberColor
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.GoldText
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.PurpleAccentEdge
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.WoodDark
import kotlin.math.max

@Composable
fun GameOverOverlay(
    level: Int,
    score: Int,
    bestScoreBeforeRun: Int,
    beatBest: Boolean,
    crossedLevelRecord: Boolean,
    isEditorTest: Boolean,
    isImported: Boolean,
    onHome: () -> Unit,
    onAddToFavorites: (memo: String) -> Unit,
    onWatchAdToRevive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val homeLabel = when {
        isEditorTest -> "✏️ 編集に戻る"
        isImported -> "⭐ お気に入りに戻る"
        else -> "🏠 ホームへ"
    }
    var addingFavorite by remember { mutableStateOf(false) }
    var addedFavorite by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }

    // ニアミス訴求:僅差で更新を逃した時ほど「もう一回」を誘う効果が高い(ギャンブル心理学で言うニアミス効果)。
    val scoreGap = bestScoreBeforeRun - score
    val isNearMiss = !beatBest && bestScoreBeforeRun > 0 && scoreGap > 0 &&
        scoreGap <= max(15, (bestScoreBeforeRun * 0.2).toInt())

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GameOverHeader(beatBest)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 320.dp)) {
            StatTile(icon = R.drawable.icon_seedling, label = "到達レベル", value = "Lv.$level", modifier = Modifier.weight(1f))
            StatTile(icon = R.drawable.icon_paw, label = "なかま", value = "${score}匹", modifier = Modifier.weight(1f))
        }

        if (isNearMiss) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(EmberColor, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Image(painterResource(R.drawable.icon_fire), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("あと${scoreGap}匹でハイスコアだった！", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        if (crossedLevelRecord) {
            RecordChip("🚀 Lv.$level 到達レベル記録更新", Neon2)
        }

        // 主役は「続ける」。オレンジの大きなボタンで、もう一度挑戦したくなる気持ちを後押しする。
        NeonButton(
            "広告を見てライフ全回復",
            onClick = onWatchAdToRevive,
            face = PurpleAccent,
            edge = PurpleAccentEdge,
            modifier = Modifier.widthIn(min = 260.dp),
        )

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
                    },
                )
                GhostButton("キャンセル", onClick = { addingFavorite = false; memo = "" })
            }
        }
        if (addedFavorite) {
            Text("⭐ お気に入りに登録しました", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldText)
        }

        // お気に入り登録とホームは横並びにして、盤面パネルの高さに収める。
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 320.dp)) {
            if (!isImported && !addingFavorite && !addedFavorite) {
                GhostButton("⭐ お気に入り", onClick = { addingFavorite = true }, modifier = Modifier.weight(1f))
            }
            GhostButton(homeLabel, onClick = onHome, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * 見出し: 通常は割れたハートが「ぽとっ」と落ちてきて小さく揺れる(悔しさを可愛く見せる)。
 * 自己ベストを更新して終わった時は、トロフィーが弾んで登場する。
 */
@Composable
private fun GameOverHeader(beatBest: Boolean) {
    val drop = remember { Animatable(-40f) }
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        drop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    val transition = rememberInfiniteTransition(label = "gameOverWobble")
    val wobble by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "gameOverWobbleAngle",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painterResource(if (beatBest) R.drawable.icon_trophy else R.drawable.icon_broken_heart),
            contentDescription = null,
            modifier = Modifier
                .size(62.dp)
                .offset(y = drop.value.dp)
                .rotate(if (beatBest) 0f else wobble)
                .scale(pop.value),
        )
        if (beatBest) {
            GameText("自己ベスト更新！", fontSize = 30.sp, color = GoldColor, outline = WoodDark)
        } else {
            GameText("GAME OVER", fontSize = 32.sp, color = DangerColor, outline = Color.White)
            Text("ライフがなくなりました", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }
    }
}

/** 今回の成果を並べる小さな札(アイコン+見出し+数字)。 */
@Composable
private fun StatTile(@DrawableRes icon: Int, label: String, value: String, modifier: Modifier = Modifier) {
    CreamCard(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        }
    }
}
