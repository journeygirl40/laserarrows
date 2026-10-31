package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.theme.animalRes
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.laserarrows.app.engine.CellPos
import kotlinx.coroutines.launch

/** カップル成立したマスから、💕と赤ちゃん(その動物の小さな顔)がふわっと浮かび上がる誕生演出。 */
@Composable
fun BirthPopupView(at: CellPos, color: LaserColor, babies: Int, cellSizeDp: Dp, gapDp: Dp) {
    val rise = remember { Animatable(0f) }
    val pop = remember { Animatable(0.3f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
        launch { rise.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        kotlinx.coroutines.delay(550)
        fade.animateTo(0f, tween(350))
    }
    val stride = cellSizeDp + gapDp
    val babySize = cellSizeDp * 0.3f
    Box(
        modifier = Modifier
            .offset(x = stride * at.col, y = stride * at.row - cellSizeDp * 0.6f * rise.value)
            .size(cellSizeDp)
            .scale(pop.value)
            .alpha(fade.value),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.icon_sparkling_heart), contentDescription = null, modifier = Modifier.size(cellSizeDp * 0.42f))
            // 生まれた赤ちゃんの数だけ小さな顔を並べる(多すぎる時は数字で見せる)。
            Row(modifier = Modifier.wrapContentWidth(unbounded = true), verticalAlignment = Alignment.CenterVertically) {
                repeat(babies.coerceAtMost(4)) {
                    Image(painterResource(color.animalRes()), contentDescription = null, modifier = Modifier.size(babySize))
                }
                if (babies > 4) GameText("×$babies", fontSize = 13.sp, color = TextPrimary, singleLine = true)
            }
        }
    }
}
