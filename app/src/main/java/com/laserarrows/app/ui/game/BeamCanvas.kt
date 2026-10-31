package com.laserarrows.app.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import com.laserarrows.app.R
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.ui.theme.EggTrail
import com.laserarrows.app.ui.theme.animalRes
import com.laserarrows.app.ui.theme.toComposeColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ParticleSpec(val angle: Float, val speed: Float, val size: Float)

/** ビームが1マス進む時間。走る子が跳ねながら付いていくのが目で追える速さにしている。 */
private const val MS_PER_SEGMENT = 64

/** 相手に届いてから、二人が駆け寄ってくっつき、ハートが弾けて消えるまでの長さ。 */
internal const val MEET_DURATION_MS = 880

/** 出会い演出の中で二人のほっぺがくっつく瞬間(到達音をここに合わせて鳴らす)。 */
internal const val MEET_BUMP_DELAY_MS = 190L

/** 相手に届く直前、走る子が手前で止まる距離(区間単位)。ビームだけが先に相手へ届き、そこから二人が駆け寄る。 */
private const val RUNNER_STOP_SHORT = 0.62f

/** マスに描かれている顔の大きさ(マスに対する比)。上に重ねて描くキャラクターも同じ大きさにする。 */
private const val RUNNER_FACE = 0.6f
private const val PARTNER_FACE = 0.56f

/** ビームが伸びていく先端から後方へ飛び散る火花(走る子が蹴り上げる土ぼこりのような質感を出す)。 */
private data class TrailSpark(val trailOffset: Float, val perp: Float, val size: Float, val twinklePhase: Float)

@Composable
fun BeamCanvas(
    shot: ShotAnimation?,
    cellSizePx: Float,
    gapPx: Float,
    onRevealFinished: () -> Unit,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val growth = remember(shot?.token) { Animatable(0f) }
    val muzzleFlash = remember(shot?.token) { Animatable(0f) }
    val shockwave = remember(shot?.token) { Animatable(0f) }
    val burstProgress = remember(shot?.token) { Animatable(0f) }
    val meet = remember(shot?.token) { Animatable(0f) }
    val missFizzle = remember(shot?.token) { Animatable(0f) }
    val particles = remember(shot?.token) {
        val rnd = Random(shot?.token ?: 0L)
        List(28) {
            ParticleSpec(
                angle = rnd.nextFloat() * (2 * Math.PI).toFloat(),
                speed = 50f + rnd.nextFloat() * 100f,
                size = 1.4f + rnd.nextFloat() * 2.8f,
            )
        }
    }
    val loveHearts = remember(shot?.token) { randomLoveHearts(shot?.token ?: 0L) }
    val animalArt = LaserColor.entries.associateWith { ImageBitmap.imageResource(it.animalRes()) }
    val eggArt = ImageBitmap.imageResource(R.drawable.icon_egg)
    val heartArt = ImageBitmap.imageResource(R.drawable.icon_heart)
    // ビームの先端が伸びていく間、後方にこぼれ落ちる火花(花火のロケット軌道のような質感)。
    val trailSparks = remember(shot?.token) {
        val rnd = Random((shot?.token ?: 0L) xor 0x51ED2701)
        List(10) {
            TrailSpark(
                trailOffset = 2f + rnd.nextFloat() * 16f,
                perp = (rnd.nextFloat() - 0.5f) * 6f,
                size = 0.6f + rnd.nextFloat() * 1.1f,
                twinklePhase = rnd.nextFloat() * 6.28f,
            )
        }
    }

    LaunchedEffect(shot?.token) {
        val s = shot ?: return@LaunchedEffect
        val path = s.result.path
        val segCount = (path.size - 1).coerceAtLeast(0)

        growth.snapTo(0f)
        muzzleFlash.snapTo(0f)
        shockwave.snapTo(0f)
        launch { muzzleFlash.animateTo(1f, tween(220, easing = LinearEasing)) }
        launch { shockwave.animateTo(1f, tween(320, easing = EaseOutCubic)) }

        if (segCount > 0) {
            growth.animateTo(segCount.toFloat(), tween(segCount * MS_PER_SEGMENT, easing = LinearEasing))
        }
        onRevealFinished()
        if (s.result.hit != null && !s.hitIsShooterClash) {
            meet.snapTo(0f)
            meet.animateTo(1f, tween(MEET_DURATION_MS, easing = LinearEasing))
        } else if (s.result.hit != null) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, tween(620, easing = LinearEasing))
        } else {
            missFizzle.snapTo(0f)
            missFizzle.animateTo(1f, tween(280, easing = LinearEasing))
            delay(60)
        }
        onSettled()
    }

    val stridePx = cellSizePx + gapPx

    fun cellCenter(pos: CellPos): Offset =
        Offset(pos.col * stridePx + cellSizePx / 2f, pos.row * stridePx + cellSizePx / 2f)

    val scaleFactor = cellSizePx / 60f

    // マスの面は下側の厚みの分だけ上にずれているので、顔の中心もその分だけ持ち上げる(マスに描かれた顔とぴったり重ねるため)。
    val faceLift = cellSizePx * 0.035f

    Canvas(modifier = modifier) {
        val s = shot ?: return@Canvas
        val path = s.result.path
        if (path.isEmpty()) return@Canvas
        val colorTrail = s.result.colorTrail
        val maxSeg = (path.size - 1).coerceAtLeast(0)
        val g = growth.value.coerceIn(0f, maxSeg.toFloat())
        val fullSegs = floor(g).toInt().coerceIn(0, maxSeg)
        val frac = g - fullSegs
        val stillGrowing = g < maxSeg.toFloat()

        val m = meet.value
        val hitPos = s.result.hit
        val meetHit = hitPos != null && !s.hitIsShooterClash
        // 二人が出会えたら、主役を二人に譲るように道のりはゆっくり消えていく。
        val beamAlpha = 1f - clamp01((m - 0.45f) / 0.4f)

        // 明るい芝生の上でも映えるよう、白い縁取りの上に動物の色のリボンを重ね、中央に足あとのような白い点線を走らせる。
        val footprints = PathEffect.dashPathEffect(floatArrayOf(4f * scaleFactor, 9f * scaleFactor), 0f)
        fun drawBeamSegment(p1: Offset, p2: Offset, hex: Color) {
            drawLine(color = hex.copy(alpha = 0.25f * beamAlpha), start = p1, end = p2, strokeWidth = 26f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = Color.White.copy(alpha = beamAlpha), start = p1, end = p2, strokeWidth = 15f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = hex.copy(alpha = beamAlpha), start = p1, end = p2, strokeWidth = 10f * scaleFactor, cap = StrokeCap.Round)
            drawLine(color = Color.White.copy(alpha = 0.9f * beamAlpha), start = p1, end = p2, strokeWidth = 3.4f * scaleFactor, cap = StrokeCap.Round, pathEffect = footprints)
        }

        for (i in 0 until fullSegs) {
            val p1 = cellCenter(path[i])
            val p2 = cellCenter(path[i + 1])
            val hex = colorTrail.getOrNull(i)?.toComposeColor() ?: EggTrail
            drawBeamSegment(p1, p2, hex)
        }

        var tipPos: Offset? = null
        if (fullSegs < maxSeg && frac > 0f) {
            val p1 = cellCenter(path[fullSegs])
            val p2 = cellCenter(path[fullSegs + 1])
            val tip = Offset(p1.x + (p2.x - p1.x) * frac, p1.y + (p2.y - p1.y) * frac)
            val hex = colorTrail.getOrNull(fullSegs)?.toComposeColor() ?: EggTrail
            drawBeamSegment(p1, tip, hex)
            tipPos = tip
        } else if (fullSegs == maxSeg) {
            tipPos = cellCenter(path[fullSegs])
        }

        for (i in 0..fullSegs) {
            if (path[i] in s.bends) {
                val bendColor = colorTrail.getOrNull((i - 1).coerceAtLeast(0))?.toComposeColor() ?: EggTrail
                val center = cellCenter(path[i])
                drawCircle(color = bendColor.copy(alpha = 0.32f * beamAlpha), radius = 12f * scaleFactor, center = center)
                drawCircle(color = bendColor.copy(alpha = 0.6f * beamAlpha), radius = 7.5f * scaleFactor, center = center)
                drawCircle(color = Color.White.copy(alpha = 0.95f * beamAlpha), radius = 4f * scaleFactor, center = center)
            }
        }

        if (stillGrowing && tipPos != null) {
            val headColor = colorTrail.getOrNull(fullSegs)?.toComposeColor() ?: EggTrail
            drawCircle(color = headColor.copy(alpha = 0.28f), radius = 17f * scaleFactor, center = tipPos)
            drawCircle(color = headColor.copy(alpha = 0.85f), radius = 9f * scaleFactor, center = tipPos)
            drawCircle(color = Color.White, radius = 4f * scaleFactor, center = tipPos)

            // 先端から後方へ飛び散る火花(ロケット花火の軌道のような質感)。
            val segStart = cellCenter(path[fullSegs])
            val segEnd = cellCenter(path[fullSegs + 1])
            val segVec = segEnd - segStart
            val segLen = sqrt(segVec.x * segVec.x + segVec.y * segVec.y)
            if (segLen > 0.5f) {
                val dir = Offset(segVec.x / segLen, segVec.y / segLen)
                val perpDir = Offset(-dir.y, dir.x)
                val travelled = frac * segLen
                trailSparks.forEach { spark ->
                    val backDist = (spark.trailOffset * scaleFactor).coerceAtMost(travelled)
                    val gravityDrop = backDist * backDist * 0.0012f
                    val pos = tipPos - dir * backDist + perpDir * (spark.perp * scaleFactor) + Offset(0f, gravityDrop)
                    val twinkle = sin(frac * 8f + spark.twinklePhase).toFloat() * 0.5f + 0.5f
                    val fade = (1f - backDist / (16f * scaleFactor)).coerceIn(0f, 1f)
                    val alpha = fade * (0.35f + twinkle * 0.65f)
                    drawCircle(color = headColor.copy(alpha = alpha * 0.85f), radius = spark.size * scaleFactor, center = pos)
                    drawCircle(color = Color.White.copy(alpha = alpha * 0.75f), radius = spark.size * 0.5f * scaleFactor, center = pos)
                }
            }
        }

        val mf = muzzleFlash.value
        if (mf < 1f) {
            val originPos = cellCenter(path[0])
            val originColor = colorTrail.getOrNull(0)?.toComposeColor() ?: EggTrail
            drawCircle(
                color = originColor.copy(alpha = (1f - mf) * 0.35f),
                radius = (14f + mf * 36f) * scaleFactor,
                center = originPos,
            )
            drawCircle(
                color = originColor.copy(alpha = (1f - mf) * 0.85f),
                radius = (6f + mf * 22f) * scaleFactor,
                center = originPos,
            )
            drawCircle(
                color = Color.White.copy(alpha = (1f - mf)),
                radius = (3f + mf * 7f) * scaleFactor,
                center = originPos,
            )
        }

        val sw = shockwave.value
        if (sw < 1f) {
            val originPos = cellCenter(path[0])
            val originColor = colorTrail.getOrNull(0)?.toComposeColor() ?: EggTrail
            drawCircle(
                color = originColor.copy(alpha = (1f - sw) * 0.55f),
                radius = (10f + sw * 46f) * scaleFactor,
                center = originPos,
                style = Stroke(width = (3f * (1f - sw) + 0.6f) * scaleFactor),
            )
            val rayLen = (8f + sw * 30f) * scaleFactor
            val rayInner = 4f * scaleFactor
            val rayAlpha = (1f - sw) * 0.8f
            for (k in 0 until 8) {
                val ang = (Math.PI / 4 * k).toFloat()
                val dir = Offset(cos(ang), sin(ang))
                drawLine(
                    color = originColor.copy(alpha = rayAlpha),
                    start = originPos + dir * rayInner,
                    end = originPos + dir * rayLen,
                    strokeWidth = 2f * scaleFactor,
                    cap = StrokeCap.Round,
                )
            }
        }

        if (hitPos != null && s.hitIsShooterClash) {
            // 矢印同士の衝突: 命中はしているが狙い通りの接続ではないため、
            // 派手な虹色バーストではなく、くすんだ色味で控えめな「不発」寄りの見た目にする。
            val p = cellCenter(hitPos)
            val t = burstProgress.value
            val clashColor = Color(0xFFC97A3D)
            drawCircle(
                color = clashColor.copy(alpha = (1f - t * 1.3f).coerceIn(0f, 1f)),
                radius = (t * 28f * scaleFactor).coerceAtLeast(0f),
                center = p,
                style = Stroke(width = 3f * scaleFactor),
            )
            drawCircle(
                color = Color.White.copy(alpha = (0.35f - t * 0.6f).coerceIn(0f, 1f)),
                radius = (t * 15f * scaleFactor).coerceAtLeast(0f),
                center = p,
                style = Stroke(width = 2f * scaleFactor),
            )
            particles.take(10).forEach { particle ->
                val dist = particle.speed * 0.55f * t * scaleFactor
                drawCircle(
                    color = clashColor.copy(alpha = (1f - t * 1.2f).coerceIn(0f, 1f)),
                    radius = particle.size * 0.7f * scaleFactor,
                    center = p + Offset(cos(particle.angle) * dist, sin(particle.angle) * dist),
                )
            }
        } else if (missFizzle.value > 0f) {
            val p = cellCenter(path.last())
            val t = missFizzle.value
            drawCircle(
                color = Color(0xFF9691B8).copy(alpha = (1f - t) * 0.7f),
                radius = (6f + t * 16f) * scaleFactor,
                center = p,
                style = Stroke(width = 2f * scaleFactor),
            )
            particles.take(8).forEach { particle ->
                val dist = particle.speed * 0.45f * t * scaleFactor
                drawCircle(
                    color = Color.White.copy(alpha = (1f - t * 1.4f).coerceIn(0f, 1f)),
                    radius = particle.size * 0.7f * scaleFactor,
                    center = p + Offset(cos(particle.angle) * dist, sin(particle.angle) * dist),
                )
            }
        }

        drawCharacters(
            path = path,
            colorTrail = colorTrail,
            g = g,
            meetHit = meetHit,
            hitPos = hitPos,
            m = m,
            clash = if (hitPos != null && s.hitIsShooterClash) burstProgress.value else 0f,
            miss = if (hitPos == null) missFizzle.value else 0f,
            cellSizePx = cellSizePx,
            stridePx = stridePx,
            faceLift = faceLift,
            animalArt = animalArt,
            eggArt = eggArt,
            heartArt = heartArt,
            loveHearts = loveHearts,
        )
    }
}

/**
 * 走る子と、待っている相手を盤面の上に描く(マスに描かれた顔は、その間だけ隠してある)。
 * 走る子はビームの先端に乗ってぴょこぴょこ跳ねながら進み、相手は近づいてくる走る子のほうへ身を乗り出して待つ。
 * 届いたら二人が駆け寄ってほっぺをくっつけ、相手の頭上のハートが大きく膨らんで弾け、ふたりで嬉しそうに跳ねてから消える。
 * ぶつかった時(clash)は跳ね返されて、出会えなかった時(miss)は目を回しながら消える。
 */
private fun DrawScope.drawCharacters(
    path: List<CellPos>,
    colorTrail: List<LaserColor?>,
    g: Float,
    meetHit: Boolean,
    hitPos: CellPos?,
    m: Float,
    clash: Float,
    miss: Float,
    cellSizePx: Float,
    stridePx: Float,
    faceLift: Float,
    animalArt: Map<LaserColor, ImageBitmap>,
    eggArt: ImageBitmap,
    heartArt: ImageBitmap,
    loveHearts: List<LoveHeartSpec>,
) {
    val maxSeg = path.size - 1
    val piF = PI.toFloat()
    fun face(pos: CellPos) = Offset(pos.col * stridePx + cellSizePx / 2f, pos.row * stridePx + cellSizePx / 2f - faceLift)
    fun pointAt(arc: Float): Offset {
        if (maxSeg <= 0) return face(path[0])
        val c = arc.coerceIn(0f, maxSeg.toFloat())
        val i = floor(c).toInt().coerceAtMost(maxSeg - 1)
        return mix(face(path[i]), face(path[i + 1]), c - i)
    }
    fun segDir(i: Int): Offset {
        if (maxSeg <= 0) return Offset(1f, 0f)
        val k = i.coerceIn(0, maxSeg - 1)
        return Offset((path[k + 1].col - path[k].col).toFloat(), (path[k + 1].row - path[k].row).toFloat())
    }

    // ---- 走る子: 1マスごとに1回跳ね、着地のたびに少しつぶれ、左右に体を揺らして駆けていく ----
    val stop = if (meetHit) (maxSeg - RUNNER_STOP_SHORT).coerceAtLeast(0f) else maxSeg.toFloat()
    val rs = g.coerceAtMost(stop)
    val seg = floor(rs).toInt().coerceIn(0, (maxSeg - 1).coerceAtLeast(0))
    val runnerIsEgg = colorTrail.firstOrNull() == null
    val runnerColor = colorTrail.getOrNull(seg)
    val runnerArt = runnerColor?.let { animalArt[it] } ?: eggArt
    // たまごから生まれた赤ちゃんは一回り小さく描く。
    val runnerSize = cellSizePx * if (runnerIsEgg && runnerColor != null) 0.46f else RUNNER_FACE
    val dir = segDir(seg)
    val hopWave = sin(rs * piF)
    val land = (1f - abs(hopWave)).pow(3)
    val runGround = pointAt(rs)
    var runner = SpritePose(
        center = runGround - Offset(0f, abs(hopWave) * cellSizePx * 0.09f),
        ground = runGround,
        scaleX = 1f + 0.1f * land,
        scaleY = 1f - 0.1f * land,
        rotation = hopWave * 9f + dir.x * 6f,
    )
    if (clash > 0f) {
        val back = -dir * (cellSizePx * 0.25f * easeOutCubic(clash))
        runner = runner.copy(
            center = runner.center + back,
            ground = runner.ground + back,
            rotation = runner.rotation + sin(clash * 3f * piF) * 14f,
            alpha = 1f - clash,
        )
    }
    if (miss > 0f) {
        runner = runner.copy(rotation = runner.rotation + sin(miss * 4f * piF) * 16f * (1f - miss), alpha = 1f - miss)
    }

    val partnerArt = colorTrail.lastOrNull()?.let { animalArt[it] }
    if (!meetHit || hitPos == null || partnerArt == null) {
        drawGroundShadow(runner, cellSizePx)
        drawSprite(runnerArt, runner, runnerSize)
        return
    }

    // ---- 待っている相手: 走る子が近づくほど、そちらへ身を乗り出してそわそわ跳ね、頭上のハートが膨らむ ----
    val partnerSize = cellSizePx * PARTNER_FACE
    val home = face(hitPos)
    val dLast = segDir(maxSeg - 1)
    val toward = -dLast
    val near = clamp01(1f - (maxSeg - g) / 2.5f)
    val leanOut = home + toward * (cellSizePx * 0.07f * near)
    val waiting = SpritePose(
        center = leanOut - Offset(0f, abs(sin(g * piF * 1.3f)) * cellSizePx * 0.07f * near),
        ground = leanOut,
        rotation = toward.x * 8f * near,
    )
    val heartSlot = Offset(hitPos.col * stridePx + cellSizePx * 0.89f, hitPos.row * stridePx + cellSizePx * 0.11f)
    val slotHeartSize = cellSizePx * 0.34f * (1f + 0.35f * near) * (1f + 0.06f * sin(g * piF * 2f))

    if (m <= 0f) {
        drawGroundShadow(waiting, cellSizePx)
        drawGroundShadow(runner, cellSizePx)
        drawSprite(partnerArt, waiting, partnerSize)
        drawSprite(runnerArt, runner, runnerSize)
        drawSprite(heartArt, SpritePose(heartSlot, heartSlot), slotHeartSize)
        return
    }

    // ---- 出会い: 二人が真ん中へぴょんと駆け寄り、ほっぺをくっつけて傾け合い、そろって2回跳ねてから消える ----
    // 縦に出会った時も、二人が並んで見えるよう左右に並べる(横から来た時は来た向きのまま、走る子が手前側)。
    val side = if (dLast.x != 0f) dLast.x else 1f
    val mid = home - dLast * (stridePx * RUNNER_STOP_SHORT / 2f)
    val sep = cellSizePx * 0.21f
    val j = easeOutCubic(m / 0.24f)
    val arc = sin(j * piF) * cellSizePx * 0.2f
    val squish = if (m in 0.2f..0.38f) sin((m - 0.2f) / 0.18f * piF) else 0f
    val lean = 12f * clamp01((m - 0.2f) / 0.12f) * (1f - clamp01((m - 0.8f) / 0.2f))
    val hopT = clamp01((m - 0.4f) / 0.5f)
    val hop = abs(sin(hopT * 2f * piF)) * cellSizePx * 0.1f
    val fade = 1f - clamp01((m - 0.84f) / 0.16f)
    val shrink = 0.85f + 0.15f * fade
    fun together(start: SpritePose, end: Offset, tilt: Float, arcScale: Float): SpritePose {
        val ground = mix(start.ground, end, j)
        val startLift = (start.ground.y - start.center.y) * (1f - j)
        return SpritePose(
            center = ground - Offset(0f, arc * arcScale + hop + startLift),
            ground = ground,
            scaleX = (1f - 0.1f * squish) * shrink,
            scaleY = (1f + 0.06f * squish) * shrink,
            rotation = tilt,
            alpha = fade,
        )
    }
    val runnerMet = together(runner, mid - Offset(side * sep, 0f), side * lean, 1f)
    val partnerMet = together(waiting, mid + Offset(side * sep, 0f), -side * lean, 0.8f)

    drawGroundShadow(runnerMet, cellSizePx)
    drawGroundShadow(partnerMet, cellSizePx)
    drawLoveBurst(
        center = mid,
        animalColor = colorTrail.lastOrNull()?.toComposeColor() ?: EggTrail,
        heart = heartArt,
        hearts = loveHearts,
        cellSizePx = cellSizePx,
        m = m,
    )
    drawSprite(partnerArt, partnerMet, partnerSize)
    drawSprite(runnerArt, runnerMet, runnerSize)

    // 相手の頭上にあったハートが二人の真上へ飛んできて、ぽよんと大きく膨らみ、どきどき脈打ちながら昇っていく。
    val heartTop = mid - Offset(0f, cellSizePx * 0.5f)
    val heartPos: Offset
    val heartSize: Float
    if (m < 0.2f) {
        val e = easeOutCubic(m / 0.2f)
        heartPos = mix(heartSlot, heartTop, e)
        heartSize = slotHeartSize + (cellSizePx * 0.4f - slotHeartSize) * e
    } else {
        val pop = easeOutBack((m - 0.2f) / 0.22f)
        val beat = if (m > 0.42f) 1f + 0.07f * sin((m - 0.42f) / 0.58f * 4f * piF) else 1f
        heartSize = cellSizePx * (0.4f + 0.28f * pop) * beat
        heartPos = heartTop - Offset(0f, clamp01((m - 0.5f) / 0.5f) * cellSizePx * 0.18f)
    }
    drawSprite(heartArt, SpritePose(heartPos, heartPos, rotation = sin(m * 10f) * 6f, alpha = fade), heartSize)
}
