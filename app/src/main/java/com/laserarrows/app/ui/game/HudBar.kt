package com.laserarrows.app.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.GlassSurface
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun HudBar(level: Int, score: Int, comboMultiplier: Double, lives: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HudBlock(label = "LEVEL", value = level.toString(), modifier = Modifier.weight(1f))
        HudBlock(label = "SCORE", value = score.toString(), modifier = Modifier.weight(1f))
        HudBlock(
            label = "COMBO",
            value = String.format(Locale.US, "×%.1f", comboMultiplier),
            valueColor = GoldColor,
            modifier = Modifier.weight(1f),
        )
        HudBlock(
            label = "LIFE",
            value = "×$lives",
            valueColor = DangerColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HudBlock(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    GlassSurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, fontSize = 9.5.sp, color = TextMuted, letterSpacing = 1.sp, maxLines = 1)
            Text(
                value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
