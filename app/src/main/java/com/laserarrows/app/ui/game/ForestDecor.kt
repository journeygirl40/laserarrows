package com.laserarrows.app.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.CenteredScroll
import com.laserarrows.app.ui.components.CreamCard
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.WoodDark

/** 森の段階に届いた時、森の上でぴょこぴょこ跳ねて「タップで解放」を誘うごほうび。 */
@Composable
fun ForestRewardGift(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gift")
    val hop by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "hop")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .offset(y = (-6 * hop).dp)
            .scale(1f + 0.06f * hop)
            .background(Color(0xFFFFF6DE), RoundedCornerShape(18.dp))
            .border(2.dp, GoldColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text("🎁 森のごほうびが届いた!", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary)
        Text("タップで解放", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WoodDark)
    }
}

/** 解放した森のかざりを、1つずつつけたり外したりできる。にぎわいの1段目(なかまが目をさます)は森の基本なので外せない。 */
@Composable
fun ForestDecorDialog(
    livelyUnlocked: Int,
    wideUnlocked: Int,
    hidden: Set<String>,
    onToggle: (key: String, visible: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x993B2414))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() },
    ) {
        CenteredScroll(modifier = Modifier.fillMaxSize()) {
            CreamCard(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(horizontal = 20.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GameText("森のかざり", fontSize = 24.sp, color = Color(0xFFFFF1D6), outline = WoodDark)
                    Text("解放したかざりを、つけたり外したりできます", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    if (livelyUnlocked >= 2) {
                        SectionTitle("🎉 にぎわい")
                        for (stage in ForestStage.stages.filter { it.level in 2..livelyUnlocked }) {
                            val key = forestDecorKey(ForestGrowth.LIVELY, stage.level)
                            DecorRow(stage.name, stage.news, key !in hidden) { onToggle(key, it) }
                        }
                    }
                    if (wideUnlocked >= 1) {
                        SectionTitle("🌳 広さ")
                        for (stage in ForestExpansion.stages.filter { it.level in 1..wideUnlocked }) {
                            val key = forestDecorKey(ForestGrowth.WIDE, stage.level)
                            DecorRow(stage.name, stage.news, key !in hidden) { onToggle(key, it) }
                        }
                    }
                    NeonButton("とじる", onClick = onClose, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Black, color = TextPrimary, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
}

@Composable
private fun DecorRow(name: String, news: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 2.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = if (on) TextPrimary else TextMuted)
            Text(news, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }
        Spacer(modifier = Modifier.width(8.dp))
        OnOffPill(on)
    }
}

/** つける/外すの札。ONは森の緑、OFFは薄い灰色。 */
@Composable
private fun OnOffPill(on: Boolean) {
    Text(
        if (on) "ON" else "OFF",
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = if (on) Color.White else TextMuted,
        modifier = Modifier
            .background(if (on) ForestGreen else WoodDark.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
