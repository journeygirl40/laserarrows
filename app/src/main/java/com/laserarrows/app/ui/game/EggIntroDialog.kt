package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.components.CenteredScroll
import androidx.compose.ui.graphics.painter.Painter
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.ui.theme.animalPainter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.ui.components.CreamCard
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.BoardGrass
import com.laserarrows.app.ui.theme.EmberColor
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.WoodDark

/** なぞのたまごが初めて盤面に出た時に一度だけ出す説明(たまご→ぶつかる→赤ちゃんに変わる→同じ種類と出会う、を絵で見せる)。 */
@Composable
fun EggIntroDialog(onClose: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "eggIntro")
    val wobble by transition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eggWobble",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x993B2414))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        CenteredScroll(modifier = Modifier.fillMaxSize()) {
            CreamCard(modifier = Modifier.widthIn(max = 330.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("新しいなかま！", fontSize = 13.sp, fontWeight = FontWeight.Black, color = EmberColor)
                    Image(
                        painterResource(R.drawable.icon_egg),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).rotate(wobble),
                    )
                    GameText("なぞのたまご", fontSize = 26.sp, color = Color(0xFFFFF1D6), outline = WoodDark)

                    // ルールの絵解き: たまご → キツネにぶつかる → キツネの赤ちゃんに → キツネと出会う
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BoardGrass, RoundedCornerShape(16.dp))
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Step(painterResource(R.drawable.icon_egg), "たまご")
                        StepArrow()
                        Step(animalPainter(LaserColor.RED), "ぶつかる")
                        StepArrow()
                        Step(animalPainter(LaserColor.RED), "赤ちゃんに", small = true)
                        StepArrow()
                        Step(painterResource(R.drawable.icon_heart), "出会い！")
                    }

                    Text(
                        "ぶつかった動物の赤ちゃんに変わって、\nその向きへ曲がるよ。\n" +
                            "最後に変わった動物と\n同じ子に届ければ、出会い成立！",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                    )
                    Text(
                        "まだ何にも変わっていないたまごは、\n誰とも出会えないので注意。",
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    NeonButton("わかった！", onClick = onClose, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Step(icon: Painter, label: String, small: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Image(icon, contentDescription = null, modifier = Modifier.size(if (small) 26.dp else 36.dp).offset(y = if (small) 4.dp else 0.dp))
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextPrimary)
    }
}

@Composable
private fun StepArrow() {
    Text("›", fontSize = 18.sp, fontWeight = FontWeight.Black, color = WoodDark, modifier = Modifier.offset(y = (-6).dp))
}
