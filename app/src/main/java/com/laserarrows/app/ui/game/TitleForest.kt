package com.laserarrows.app.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.GoldText
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.WoodDark
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 住人ひとりひとりの「くせ」。同じ動きの使い回しにせず、性格が見えるようにする。 */
private enum class Habit { BOB, WADDLE, HOP, FROG_HOP, SWAY, PROUD, TODDLE, SLEEP }

/**
 * 森の住人1人分。xf=丘の上の横位置(0..1)、depth=奥行き(0=奥, 1=手前。手前ほど大きく、後から描く)、
 * sizeU=基準の大きさ(デザイン上のdp)、intro=この住人が森に来る段階。
 */
private data class Resident(
    @DrawableRes val art: Int,
    val xf: Float,
    val depth: Float,
    val sizeU: Float,
    val intro: Int,
    val habit: Habit,
    val phase: Float,
)

private val RESIDENTS = listOf(
    Resident(R.drawable.animal_bear, 0.15f, 0.2f, 52f, 3, Habit.SWAY, 0.4f),
    Resident(R.drawable.animal_lion, 0.85f, 0.2f, 52f, 4, Habit.PROUD, 1.7f),
    Resident(R.drawable.animal_penguin, 0.3f, 0.5f, 56f, 1, Habit.WADDLE, 0.9f),
    Resident(R.drawable.animal_rabbit, 0.7f, 0.5f, 56f, 1, Habit.HOP, 2.3f),
    Resident(R.drawable.animal_penguin, 0.4f, 0.68f, 30f, 5, Habit.TODDLE, 0.2f),
    Resident(R.drawable.animal_fox, 0.5f, 0.78f, 66f, 1, Habit.BOB, 0f),
    Resident(R.drawable.animal_fox, 0.63f, 0.9f, 32f, 5, Habit.TODDLE, 3.1f),
    Resident(R.drawable.animal_frog, 0.5f, 1f, 40f, 2, Habit.FROG_HOP, 0.6f),
)

/** しずかな森(段階0)で丸くなって眠っている二人。ハイスコアが出ると目をさます。 */
private val SLEEPERS = listOf(
    Resident(R.drawable.animal_penguin, 0.38f, 0.6f, 52f, 0, Habit.SLEEP, 0f),
    Resident(R.drawable.animal_rabbit, 0.62f, 0.6f, 52f, 0, Habit.SLEEP, 1.6f),
)

private val FLOWER_COLORS = listOf(Color(0xFFFF8FB1), Color(0xFFFFD25E), Color(0xFFFFFFFF), Color(0xFFB79CFF), Color(0xFFFF9E5E))
private val FLAG_COLORS = listOf(Color(0xFFFF6F61), Color(0xFFFFC84A), Color(0xFF6CCB6A), Color(0xFF5DB8FF), Color(0xFFB98BFF), Color(0xFFFF9E4F))

/**
 * タイトルの森のジオラマ。ハイスコアの段階([ForestStage])が上がるほど住人・木・花・飾りが増え、
 * 住人の動きも大きく速く(=元気に)なる。段階が上がった直後(celebrateLevel)は、新しく来た住人がポンと現れ、
 * ハートが弾けて「森がにぎやかになった!」と知らせる(何段も一気に上がった時は、その間に来た住人がみんなポンと現れる)。
 */
@Composable
fun TitleForest(level: Int, celebrateLevel: Int?, celebrateFrom: Int = 0, modifier: Modifier = Modifier) {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time = (it - start) / 1_000_000_000f }
    }
    val pop = remember(celebrateLevel) { Animatable(if (celebrateLevel == null) 1f else 0f) }
    val burst = remember(celebrateLevel) { Animatable(if (celebrateLevel == null) 1f else 0f) }
    val banner = remember(celebrateLevel) { Animatable(0f) }
    LaunchedEffect(celebrateLevel) {
        if (celebrateLevel == null) return@LaunchedEffect
        delay(350)
        launch { pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 260f)) }
        launch { burst.animateTo(1f, tween(1500, easing = LinearEasing)) }
        banner.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 320f))
        delay(2300)
        banner.animateTo(0f, tween(400))
    }

    val residentArt = (RESIDENTS + SLEEPERS).map { it.art }.distinct().associateWith { ImageBitmap.imageResource(it) }
    val tree = ImageBitmap.imageResource(R.drawable.icon_tree)
    val seedling = ImageBitmap.imageResource(R.drawable.icon_seedling)
    val house = ImageBitmap.imageResource(R.drawable.icon_house)
    val heart = ImageBitmap.imageResource(R.drawable.icon_heart)
    val loveHearts = remember(celebrateLevel) { randomLoveHearts(celebrateLevel?.toLong() ?: 0L) }

    Box(modifier = modifier.fillMaxWidth().height(172.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scene = SceneScope(this, level, time)
            scene.drawBackdrop()
            scene.drawMound()
            scene.drawTrees(tree, seedling, house)
            scene.drawFlowers()
            scene.drawBunting()
            scene.drawResidents(residentArt, celebrateLevel, celebrateFrom, pop.value)
            scene.drawButterflies()
            scene.drawFloatingHearts(heart)
            scene.drawSparkles()
            if (celebrateLevel != null && burst.value in 0.001f..0.999f) {
                drawLoveBurst(
                    center = Offset(size.width / 2f, scene.moundY - 40f * scene.u),
                    animalColor = GoldColor,
                    heart = heart,
                    hearts = loveHearts,
                    cellSizePx = 110f * scene.u,
                    m = burst.value,
                )
            }
        }
        if (celebrateLevel != null && banner.value > 0.01f) {
            val stage = ForestStage.stages[celebrateLevel.coerceIn(0, ForestStage.stages.lastIndex)]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp).scale(0.6f + 0.4f * banner.value).alpha(banner.value.coerceIn(0f, 1f)),
            ) {
                GameText("森がにぎやかになった!", fontSize = 20.sp, color = GoldColor, outline = WoodDark)
                Text(
                    "「${stage.name}」 ${stage.news}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier
                        .background(GlassFill.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/** タイトル下の「森のにぎわい」札。今の段階と、次の段階までハイスコアがあと何匹かを見せて、次の挑戦の目標にしてもらう。 */
@Composable
fun ForestProgressPill(bestScore: Int, modifier: Modifier = Modifier) {
    val stage = ForestStage.of(bestScore)
    val next = ForestStage.next(stage)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 320.dp)
            .background(GlassFill.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            if (stage.level == 0) "なかまを出会わせて、赤ちゃんを増やそう" else "森のにぎわい Lv.${stage.level}「${stage.name}」",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = if (stage.level >= 7) GoldText else TextPrimary,
        )
        if (next != null) {
            val progress = ((bestScore - stage.minScore).toFloat() / (next.minScore - stage.minScore)).coerceIn(0f, 1f)
            Canvas(modifier = Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp)) {
                val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                drawRoundRect(WoodDark.copy(alpha = 0.18f), cornerRadius = r)
                if (progress > 0f) {
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Color(0xFF8CD867), GoldColor)),
                        size = Size(size.width * progress, size.height),
                        cornerRadius = r,
                    )
                }
            }
            Text(
                "ハイスコアあと${next.minScore - bestScore}匹で「${next.name}」に",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        } else {
            Text("いちばんにぎやかな森になりました!", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GoldText)
        }
    }
}

/**
 * ジオラマ1コマ分の描画。u=デザイン上の1dpに当たるpx(高さ172dpを基準)。
 * 段階(level)ごとに、何が出るか・どれだけ元気に動くか(energy)を決める。
 */
private class SceneScope(val ds: DrawScope, val level: Int, val t: Float) {
    val w = ds.size.width
    val h = ds.size.height
    val u = h / 172f
    val cx = w / 2f
    val moundY = h - 36f * u
    val rx = minOf(w * 0.47f, 175f * u)
    val ry = 24f * u
    /** 住人の元気さ。段階が上がるほど跳ねる高さも速さも増す。 */
    val energy = 0.55f + 0.1f * level
    val piF = PI.toFloat()

    fun groundAt(xf: Float, depth: Float) = Offset(cx + (xf - 0.5f) * 2f * rx * 0.86f, moundY - ry * 0.55f + depth * ry * 0.95f)

    fun drawBackdrop() = with(ds) {
        // 丘の後ろの柔らかな光。しずかな森は朝もやの青白さ、育つほど暖かく、黄金の森は金色に。
        val glow = when {
            level >= 7 -> Color(0xFFFFD86B).copy(alpha = 0.6f)
            level == 0 -> Color(0xFFE3EEF7).copy(alpha = 0.5f)
            else -> Color(0xFFFFF4D6).copy(alpha = 0.35f + 0.04f * level)
        }
        val c = Offset(cx, moundY - 50f * u)
        drawCircle(Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f)), center = c, radius = 150f * u), radius = 150f * u, center = c)
        if (level >= 7) {
            // 黄金の森: 上から差し込む光の筋がゆっくり回る。
            clipRect {
                val origin = Offset(cx, -20f * u)
                rotate(sin(t * 0.25f) * 8f, origin) {
                    for (k in -3..3) {
                        val a = (90f + k * 16f) * piF / 180f
                        val len = 230f * u
                        val spread = 0.07f
                        val path = Path().apply {
                            moveTo(origin.x, origin.y)
                            lineTo(origin.x + cos(a - spread) * len, origin.y + sin(a - spread) * len)
                            lineTo(origin.x + cos(a + spread) * len, origin.y + sin(a + spread) * len)
                            close()
                        }
                        val shimmer = 0.06f + 0.05f * (0.5f + 0.5f * sin(t * 1.3f + k))
                        drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFE9A0).copy(alpha = shimmer), Color.Transparent), startY = origin.y, endY = origin.y + len))
                    }
                }
            }
        }
    }

    fun drawMound() = with(ds) {
        // 落ち影 → 土の側面 → 芝の天面 → つやのハイライト、の順に重ねて、手のひらに乗るジオラマのような厚みを出す。
        drawOval(Color.Black.copy(alpha = 0.13f), topLeft = Offset(cx - rx * 1.02f, moundY + 4f * u), size = Size(rx * 2.04f, ry * 2.1f))
        drawOval(
            Brush.verticalGradient(listOf(Color(0xFFB98352), Color(0xFF8E5A2C)), startY = moundY, endY = moundY + ry * 2.4f),
            topLeft = Offset(cx - rx, moundY - ry + 9f * u),
            size = Size(rx * 2f, ry * 2f),
        )
        val lush = level / 7f
        val top = lerp(Color(0xFFB9D49A), Color(0xFF8EDB5E), lush)
        val bottom = lerp(Color(0xFF93B877), Color(0xFF5DB041), lush)
        drawOval(
            Brush.verticalGradient(listOf(top, bottom), startY = moundY - ry, endY = moundY + ry),
            topLeft = Offset(cx - rx, moundY - ry),
            size = Size(rx * 2f, ry * 2f),
        )
        drawOval(Color.White.copy(alpha = 0.2f), topLeft = Offset(cx - rx * 0.78f, moundY - ry * 0.9f), size = Size(rx * 1.56f, ry * 0.8f))
        if (level >= 7) {
            drawOval(
                GoldColor.copy(alpha = 0.55f + 0.2f * sin(t * 2f)),
                topLeft = Offset(cx - rx, moundY - ry),
                size = Size(rx * 2f, ry * 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * u),
            )
        }
    }

    fun drawTrees(tree: ImageBitmap, seedling: ImageBitmap, house: ImageBitmap) = with(ds) {
        // (横位置, 大きさ, この段階から)。奥の木ほど小さく薄くして遠近感を出す。
        val trees = listOf(
            Triple(0.34f, 46f, 3),
            Triple(0.66f, 42f, 4),
            Triple(0.08f, if (level == 0) 54f else 74f, 0),
            Triple(0.92f, 66f, 2),
        )
        trees.forEach { (xf, sizeU, from) ->
            if (level < from) return@forEach
            val far = sizeU < 50f
            val base = groundAt(xf, if (far) -0.15f else 0.05f)
            val s = sizeU * u
            val sway = sin(t * 0.9f + xf * 7f) * (1.5f + level * 0.3f)
            ds.drawSprite(tree, SpritePose(center = base - Offset(0f, s * 0.42f), ground = base, rotation = sway, alpha = if (far) 0.85f else 1f), s)
        }
        if (level >= 5) {
            val base = groundAt(0.76f, -0.1f)
            val s = 40f * u
            ds.drawSprite(house, SpritePose(center = base - Offset(0f, s * 0.4f), ground = base), s)
        }
        if (level <= 1) {
            val base = groundAt(0.5f, 0.15f)
            val s = 30f * u
            ds.drawSprite(seedling, SpritePose(center = base - Offset(0f, s * 0.4f), ground = base, rotation = sin(t * 1.2f) * 5f), s)
        }
    }

    fun drawFlowers() = with(ds) {
        if (level < 2) return@with
        val count = (3 + (level - 2) * 3).coerceAtMost(16)
        val rnd = Random(7)
        repeat(count) { i ->
            val a = rnd.nextFloat() * 2f * piF
            val r = 0.45f + rnd.nextFloat() * 0.5f
            val p = Offset(cx + cos(a) * rx * r * 0.92f, moundY + sin(a) * ry * r * 0.75f)
            val color = FLOWER_COLORS[i % FLOWER_COLORS.size]
            val sway = sin(t * 1.6f + i) * 2f * u
            val head = p + Offset(sway, -7f * u)
            drawLine(Color(0xFF4E9A3A), p, head, strokeWidth = 1.4f * u, cap = StrokeCap.Round)
            for (k in 0 until 5) {
                val pa = k * 2f * piF / 5f + t * 0.2f
                drawCircle(color, radius = 2.3f * u, center = head + Offset(cos(pa) * 2.4f * u, sin(pa) * 2.4f * u))
            }
            drawCircle(Color(0xFFFFB84A), radius = 1.6f * u, center = head)
        }
    }

    fun drawBunting() = with(ds) {
        if (level < 6) return@with
        // おまつりの森: 木から木へ旗飾りを渡す。旗は風にはためく。
        val left = Offset(w * 0.06f, 30f * u)
        val right = Offset(w * 0.94f, 30f * u)
        val sag = 22f * u
        fun point(f: Float) = Offset(left.x + (right.x - left.x) * f, left.y + 4f * sag * f * (1f - f))
        val rope = Path().apply {
            moveTo(left.x, left.y)
            quadraticTo(cx, left.y + sag * 2f, right.x, right.y)
        }
        drawPath(rope, Color(0xFF8E5A2C), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f * u))
        val flags = 13
        for (k in 1 until flags) {
            val f = k / flags.toFloat()
            val p = point(f)
            val flutter = sin(t * 3f + k * 0.8f) * 3f * u
            val flag = Path().apply {
                moveTo(p.x - 5f * u, p.y)
                lineTo(p.x + 5f * u, p.y)
                lineTo(p.x + flutter * 0.4f, p.y + 11f * u)
                close()
            }
            drawPath(flag, FLAG_COLORS[k % FLAG_COLORS.size])
        }
    }

    fun drawResidents(art: Map<Int, ImageBitmap>, celebrateLevel: Int?, celebrateFrom: Int, pop: Float) {
        val cast = if (level == 0) SLEEPERS else RESIDENTS.filter { it.intro <= level }
        cast.sortedBy { it.depth }.forEach { r ->
            val image = art[r.art] ?: return@forEach
            val isNew = celebrateLevel != null && r.intro in celebrateFrom..celebrateLevel
            val appear = if (isNew) pop else 1f
            if (appear <= 0.01f) return@forEach
            val pose = poseOf(r)
            val s = r.sizeU * u * (0.8f + 0.2f * r.depth)
            drawShadow(pose, s, appear.coerceIn(0f, 1f))
            ds.drawSprite(image, pose.copy(scaleX = pose.scaleX * appear, scaleY = pose.scaleY * appear, alpha = appear.coerceIn(0f, 1f)), s)
            if (r.habit == Habit.SLEEP) drawZzz(pose.center + Offset(s * 0.3f, -s * 0.45f), r.phase)
        }
    }

    /** 足元の丸い影。跳ねて高く上がっているほど小さく薄くなる。 */
    private fun drawShadow(pose: SpritePose, s: Float, alpha: Float) {
        val height = (pose.ground.y - pose.center.y - s * 0.42f).coerceAtLeast(0f) / s
        val shrink = clamp01(1f - height * 2f)
        val sw = s * 0.62f * (0.6f + 0.4f * shrink)
        val sh = s * 0.14f * (0.6f + 0.4f * shrink)
        ds.drawOval(
            Color.Black.copy(alpha = 0.18f * (0.5f + 0.5f * shrink) * alpha),
            topLeft = Offset(pose.ground.x - sw / 2f, pose.ground.y - sh / 2f),
            size = Size(sw, sh),
        )
    }

    private fun poseOf(r: Resident): SpritePose {
        val e = energy
        val ph = r.phase
        var ground = groundAt(r.xf, r.depth)
        var lift = 0f
        var rot = 0f
        var sx = 1f
        var sy = 1f
        when (r.habit) {
            Habit.BOB -> {
                val b = abs(sin(t * 2.4f * e + ph))
                lift = b * 7f * u * e
                rot = sin(t * 1.2f + ph) * 5f
                val land = (1f - b).let { it * it * it }
                sx = 1f + 0.06f * land
                sy = 1f - 0.06f * land
            }
            Habit.WADDLE -> {
                val wv = sin(t * 4.2f * e + ph)
                rot = wv * 11f * e
                lift = abs(wv) * 3f * u
                ground += Offset(sin(t * 0.7f + ph) * 7f * u, 0f)
            }
            Habit.HOP -> {
                // ウサギ: ためて、ぴょん、着地でむにっ。
                val k = (t * e / 1.7f + ph) % 1f
                when {
                    k < 0.12f -> { val c = k / 0.12f; sx = 1f + 0.08f * c; sy = 1f - 0.1f * c }
                    k < 0.5f -> {
                        val j = (k - 0.12f) / 0.38f
                        lift = sin(j * piF) * 20f * u * e
                        sx = 0.94f
                        sy = 1.08f
                    }
                    k < 0.6f -> { val c = 1f - (k - 0.5f) / 0.1f; sx = 1f + 0.1f * c; sy = 1f - 0.12f * c }
                }
            }
            Habit.FROG_HOP -> {
                // カエル: 手前を左右に、ぴょこぴょこ渡り歩く。進む向きに合わせて体の向きを変える。
                val cycle = (t * 0.09f * e + ph) % 2f
                val p = if (cycle < 1f) cycle else 2f - cycle
                ground = groundAt(0.14f + 0.72f * p, r.depth)
                lift = abs(sin(t * piF * 2.4f * e)) * 11f * u
                sx = if (cycle < 1f) 1f else -1f
            }
            Habit.SWAY -> {
                rot = sin(t * 1.3f + ph) * 7f
                val breath = sin(t * 2f + ph) * 0.035f
                sx = 1f + breath
                sy = 1f - breath
            }
            Habit.PROUD -> {
                lift = abs(sin(t * 1.6f * e + ph)) * 4f * u
                // ときどき胸を張って大きくなる。
                val k = (t / 4f + ph) % 1f
                val puff = if (k < 0.12f) sin(k / 0.12f * piF) * 0.1f else 0f
                sx = 1f + puff
                sy = 1f + puff
            }
            Habit.TODDLE -> {
                // 赤ちゃん: 親のまわりをよちよち行ったり来たり。
                val v = sin(t * 0.8f * e + ph)
                ground += Offset(v * 22f * u, 0f)
                lift = abs(sin(t * 7f * e + ph)) * 3.5f * u
                rot = sin(t * 7f * e + ph) * 6f
                sx = if (cos(t * 0.8f * e + ph) >= 0f) 1f else -1f
            }
            Habit.SLEEP -> {
                // しずかな森: 横になって、すうすう寝息を立てる。
                rot = if (r.xf < 0.5f) -78f else 78f
                val breath = sin(t * 1.4f + ph) * 0.04f
                sx = 1f + breath
                sy = 1f + breath
                ground += Offset(0f, 6f * u)
            }
        }
        return SpritePose(center = ground - Offset(0f, lift + r.sizeU * u * 0.42f), ground = ground, scaleX = sx, scaleY = sy, rotation = rot)
    }

    private fun drawZzz(at: Offset, ph: Float) {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            color = TextMuted.toArgb()
        }
        for (k in 0 until 3) {
            val f = (t * 0.35f + ph + k / 3f) % 1f
            paint.textSize = (9f + 5f * f) * u
            paint.alpha = (sin(f * piF) * 200).toInt().coerceIn(0, 255)
            ds.drawContext.canvas.nativeCanvas.drawText("z", at.x + f * 14f * u + sin(f * 6f) * 3f * u, at.y - f * 26f * u, paint)
        }
    }

    fun drawButterflies() = with(ds) {
        if (level < 3) return@with
        val colors = listOf(Color(0xFFFFA6C9), Color(0xFFFFE07A), Color(0xFFA9D8FF), Color(0xFFD3B5FF), Color(0xFFFFC08A))
        repeat((level - 2).coerceAtMost(5)) { i ->
            val fi = i.toFloat()
            val p = Offset(
                cx + sin(t * 0.45f + fi * 1.9f) * rx * 0.85f,
                h * 0.36f + sin(t * 0.9f + fi * 2.7f) * 18f * u + cos(t * 1.7f + fi) * 6f * u,
            )
            val flap = 0.25f + 0.75f * abs(sin(t * 13f + fi * 3f))
            val c = colors[i % colors.size]
            val wing = 5f * u
            drawOval(c, topLeft = Offset(p.x - wing * flap * 1.1f, p.y - wing), size = Size(wing * flap * 1.1f, wing * 1.3f))
            drawOval(c, topLeft = Offset(p.x, p.y - wing), size = Size(wing * flap * 1.1f, wing * 1.3f))
            drawOval(c.copy(alpha = 0.85f), topLeft = Offset(p.x - wing * flap * 0.8f, p.y + wing * 0.1f), size = Size(wing * flap * 0.8f, wing * 0.8f))
            drawOval(c.copy(alpha = 0.85f), topLeft = Offset(p.x, p.y + wing * 0.1f), size = Size(wing * flap * 0.8f, wing * 0.8f))
            drawLine(Color(0xFF5A3A22), p - Offset(0f, wing * 0.9f), p + Offset(0f, wing * 0.8f), strokeWidth = 1.2f * u, cap = StrokeCap.Round)
        }
    }

    fun drawFloatingHearts(heart: ImageBitmap) = with(ds) {
        if (level < 1) return@with
        val count = (1 + level).coerceAtMost(8)
        repeat(count) { i ->
            val k = (t / 3.4f + i * 0.37f) % 1f
            val baseX = cx + (((i * 0.618f) % 1f) - 0.5f) * rx * 1.5f
            val p = Offset(baseX + sin(k * 5f + i) * 6f * u, moundY - 30f * u - k * 70f * u)
            ds.drawSprite(heart, SpritePose(center = p, ground = p, rotation = sin(k * 4f + i) * 12f, alpha = sin(k * piF) * 0.8f), (10f + (i % 3) * 2f) * u)
        }
    }

    fun drawSparkles() = with(ds) {
        if (level < 6) return@with
        val rnd = Random(11)
        val count = if (level >= 7) 18 else 9
        val color = if (level >= 7) Color(0xFFFFE27A) else Color.White
        repeat(count) { i ->
            val p = Offset(rnd.nextFloat() * w, rnd.nextFloat() * (moundY - 10f * u))
            val tw = abs(sin(t * (1.6f + rnd.nextFloat()) + i))
            val r = (2f + 3f * tw) * u
            val a = 0.25f + 0.75f * tw
            drawLine(color.copy(alpha = a), p - Offset(r, 0f), p + Offset(r, 0f), strokeWidth = 1.3f * u, cap = StrokeCap.Round)
            drawLine(color.copy(alpha = a), p - Offset(0f, r), p + Offset(0f, r), strokeWidth = 1.3f * u, cap = StrokeCap.Round)
            drawCircle(Color.White.copy(alpha = a), radius = 1.1f * u, center = p)
        }
    }
}
