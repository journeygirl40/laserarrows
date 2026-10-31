package com.laserarrows.app.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.ui.components.CreamCard
import com.laserarrows.app.ui.theme.BoardGrassDark
import com.laserarrows.app.ui.theme.Neon1
import com.laserarrows.app.ui.theme.Neon1Edge
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

/**
 * 盤面づくりに時間がかかっている時の読み込み画面。キツネがぴょこぴょこ跳ねながら、実際の進み具合をバーで見せる。
 * 待ち時間が短い時は出さない(呼び出し側で1秒以上かかった時だけ表示する)。
 */
@Composable
fun LoadingOverlay(progress: Float, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(250), label = "loadingProgress")
    val transition = rememberInfiniteTransition(label = "loadingHop")
    val hop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "loadingHopHeight",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x663B2414))
            // 読み込み中は下の画面を触れないようにする(二重にレベルを始めてしまうのを防ぐ)。
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        CreamCard(modifier = Modifier.widthIn(max = 300.dp).padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Image(
                        painterResource(R.drawable.animal_fox),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp).offset(y = (-14 * hop).dp).rotate(-8f + 16f * hop),
                    )
                    Image(painterResource(R.drawable.icon_paw), contentDescription = null, modifier = Modifier.size(22.dp))
                }
                Text("森を準備しています…", fontSize = 15.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(BoardGrassDark)
                        .border(1.5.dp, Neon1Edge.copy(alpha = 0.35f), RoundedCornerShape(999.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(shown.coerceAtLeast(0.06f))
                            .fillMaxHeight()
                            .background(Brush.verticalGradient(listOf(Color(0xFF8BE06A), Neon1)), RoundedCornerShape(999.dp)),
                    )
                }
                Text("${(shown * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            }
        }
    }
}
