package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.components.CenteredScroll
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.ui.components.CreamCard
import com.laserarrows.app.ui.components.GameText
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.EmberColor
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.WoodDark

/**
 * なかまの増え方の説明。最初のプレイで一度だけ出し、あとはプレイ画面の「?」からいつでも開ける。
 * 盤面では、曲がった場所に「+2」「+3」…、出会った場所に「+1」が出るので、ここではその意味を短く結び付ける。
 */
@Composable
fun ScoreHelpDialog(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x993B2414))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        CenteredScroll(modifier = Modifier.fillMaxSize()) {
            CreamCard(modifier = Modifier.widthIn(max = 340.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GameText("なかまの増え方", fontSize = 24.sp, color = Color(0xFFFFF1D6), outline = WoodDark)
                    RuleRow(R.drawable.icon_sparkling_heart, "出会うと なかま +1", null)
                    RuleRow(
                        R.drawable.icon_link,
                        "曲がるたび +2, +3, +4…",
                        "違う動物の矢印で曲がるほど増える\n例: 2回曲がって出会うと 1+2+3 = 6匹",
                    )
                    RuleRow(
                        R.drawable.icon_fire,
                        "コンボ1回ごとに +10%",
                        "続けて出会うとコンボが増える\nミスやぶつかりで0にもどる",
                    )
                    RuleRow(
                        R.drawable.icon_bulb,
                        "同じ組み合わせでも 順番で数が変わる",
                        "・曲がる回数は、撃つ時に残っている矢印で決まる\n  道案内してくれる矢印を先に消すと、曲がらなくなる\n・大きく増える手は、コンボが伸びた後に撃つと得",
                    )
                    RuleRow(
                        R.drawable.icon_star,
                        "目標に届くと ★3",
                        "目標 = いちばんうまく解いた時の数\n遠回りして出会う順番を探そう",
                    )
                    NeonButton("わかった!", onClick = onClose, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun RuleRow(@DrawableRes icon: Int, title: String, example: String?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(34.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            if (example != null) Text(example, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (icon == R.drawable.icon_link) EmberColor else TextMuted)
        }
    }
}
