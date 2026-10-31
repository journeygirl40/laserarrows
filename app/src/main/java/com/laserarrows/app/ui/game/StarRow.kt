package com.laserarrows.app.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.TextMuted

/** ★評価(0〜3)を、獲得分は金色・未獲得分は薄い色で並べて表示する。 */
@Composable
fun StarRow(stars: Int, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(3) { i ->
            val lit = i < stars
            Text(
                if (lit) "★" else "☆",
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = if (lit) GoldColor else TextMuted.copy(alpha = 0.5f),
            )
        }
    }
}
