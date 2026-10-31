package com.laserarrows.app.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GlassHi
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.GoldText
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.WoodDark
import com.laserarrows.app.ui.theme.animalRes
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * クリア画面の下(ツールバーの場所)に出る、このステージで出会えたふたり組のパレード。
 * 1組ずつ「ポン」と現れ、左の数が数え上がっていく。盤面やボタンには重ならない。
 * ★が多い(高得点な)ほど盛り上がる: ★1はそっと並ぶだけ、★2はそろって弾み、
 * ★3は波のように踊って帯が金色にきらめき、ハートがたくさん舞い上がる。
 */
@Composable
fun MeetParade(
    couples: List<ClearCouple>,
    stars: Int,
    levelScore: Int,
    par: Int,
    onCouplePop: (index: Int) -> Unit,
    onAllMet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = couples.size
    val tier = stars.coerceIn(1, 3)
    // 同じ★でも、目標(パー)に近い・超えているほど少しだけ大きく踊る。
    val ratio = if (par > 0) (levelScore.toFloat() / par).coerceIn(0f, 1.3f) else 1f
    val appear = remember(couples) { List(n) { Animatable(0f) } }
    var shown by remember(couples) { mutableIntStateOf(0) }
    val counterPop = remember(couples) { Animatable(1f) }

    LaunchedEffect(couples) {
        // CLEAR!の★が灯り終わるころから並び始める。組が多くても全体で1.4秒ほどに収める。
        delay(700)
        val interval = (1400L / n.coerceAtLeast(1)).coerceIn(70L, 180L)
        appear.forEachIndexed { i, a ->
            launch { a.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f)) }
            shown = i + 1
            onCouplePop(i)
            launch {
                counterPop.snapTo(1.35f)
                counterPop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
            }
            delay(interval)
        }
        if (n > 0) onAllMet()
    }

    val transition = rememberInfiniteTransition(label = "parade")
    val beat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (tier == 3) 700 else 1000, easing = LinearEasing)),
        label = "paradeBeat",
    )
    val allIn = n > 0 && shown >= n
    val danceAmp = when (tier) {
        3 -> 6f
        2 -> 3f
        else -> 0f
    } * (0.7f + 0.3f * ratio)
    val accent = if (tier == 3) GoldText else LovePink
    val shape = RoundedCornerShape(18.dp)
    val heartArt = ImageBitmap.imageResource(R.drawable.icon_heart)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(GlassFill, Color.White, 0.3f), GlassFill)), shape)
            .border(if (tier == 3) 2.dp else 1.5.dp, if (tier == 3) GoldColor else GlassHi, shape),
    ) {
        if (tier == 3 && allIn) {
            // ★3: 帯の上を光の筋がくり返し横切る。
            Canvas(modifier = Modifier.matchParentSize()) {
                val x = (beat * 1.6f - 0.3f) * size.width
                val w = size.height * 1.4f
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color.Transparent, GoldColor.copy(alpha = 0.28f), Color.White.copy(alpha = 0.5f), Color.Transparent),
                        start = Offset(x - w, 0f),
                        end = Offset(x + w, size.height),
                    ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.icon_sparkling_heart), contentDescription = null, modifier = Modifier.size(22.dp))
                    GameText("$shown", fontSize = 26.sp, color = accent, outline = WoodDark, modifier = Modifier.scale(counterPop.value))
                    Text("組", fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                }
                val cheer = when {
                    !allIn -> ""
                    tier == 3 -> " 大盛り上がり!"
                    tier == 2 -> " いいね!"
                    else -> ""
                }
                Text("${shown * 2}匹が出会えた$cheer", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (tier == 3 && allIn) GoldText else TextMuted)
            }
            Spacer(modifier = Modifier.width(10.dp))
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                // 組が多い時は2段に並べ、1組の大きさを帯に収まるよう縮める。
                val rows = if (n > 7) 2 else 1
                val perRow = ceil(n / rows.toFloat()).toInt().coerceAtLeast(1)
                val face = minOf(30.dp, maxWidth / (perRow * 1.95f), maxHeight / (rows * 1.45f))
                Column(verticalArrangement = Arrangement.spacedBy(face * 0.1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    couples.chunked(perRow).forEachIndexed { r, line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(face * 0.2f)) {
                            line.forEachIndexed { k, couple ->
                                val i = r * perRow + k
                                val wave = sin(2f * PI.toFloat() * (beat + i * 0.09f))
                                CoupleSprite(
                                    couple = couple,
                                    face = face,
                                    appear = appear[i].value,
                                    lift = if (allIn) (danceAmp * abs(wave)).dp else 0.dp,
                                    sway = if (allIn && tier == 3) wave * 8f else 0f,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (tier >= 2 && allIn) {
            // ★2以上: 帯の中をハートがふわふわ昇っていく(★3はたくさん)。帯の外にははみ出さない。
            val count = if (tier == 3) 12 else 5
            Canvas(modifier = Modifier.matchParentSize()) {
                for (k in 0 until count) {
                    val phase = (beat * (if (k % 2 == 0) 1f else 0.7f) + k * 0.37f) % 1f
                    val baseX = (0.32f + 0.66f * ((k * 0.618f) % 1f)) * size.width
                    val p = Offset(
                        baseX + sin(phase * 6f + k) * size.height * 0.08f,
                        size.height * (1.05f - phase * 1.1f),
                    )
                    drawSprite(
                        heartArt,
                        SpritePose(center = p, ground = p, rotation = sin(phase * 5f + k) * 14f, alpha = sin(phase * PI.toFloat()) * 0.75f),
                        size.height * (0.16f + 0.05f * (k % 3)),
                    )
                }
            }
        }
    }
}

/** 出会えたふたり組: 同じ動物が寄り添って頭を傾け合い、間に小さなハート。たまご生まれの子は一回り小さな赤ちゃん。 */
@Composable
private fun CoupleSprite(couple: ClearCouple, face: Dp, appear: Float, lift: Dp, sway: Float) {
    val art = painterResource(couple.color.animalRes())
    Box(
        modifier = Modifier
            .width(face * 1.75f)
            .height(face * 1.3f)
            .graphicsLayer {
                scaleX = appear
                scaleY = appear
                alpha = appear.coerceIn(0f, 1f)
                translationY = -lift.toPx()
                rotationZ = sway
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
    ) {
        Image(
            art,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(if (couple.babyRunner) face * 0.8f else face)
                .graphicsLayer { rotationZ = 10f },
        )
        Image(
            art,
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomEnd).size(face).graphicsLayer { rotationZ = -10f },
        )
        Image(
            painterResource(R.drawable.icon_heart),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).size(face * 0.45f),
        )
    }
}
