package com.laserarrows.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.toComposeColor

@Composable
fun DirectionPickOverlay(
    color: LaserColor,
    onPick: (Direction) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("矢印の向きを選択", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text(
            "レーザーを発射する向きを選んでください",
            fontSize = 12.5.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DirectionButton(Direction.U, color, onPick)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DirectionButton(Direction.L, color, onPick)
                Box(modifier = Modifier.size(52.dp))
                DirectionButton(Direction.R, color, onPick)
            }
            DirectionButton(Direction.D, color, onPick)
        }
        GhostButton("キャンセル", onClick = onCancel)
    }
}

@Composable
private fun DirectionButton(dir: Direction, color: LaserColor, onPick: (Direction) -> Unit) {
    val glyph = when (dir) {
        Direction.U -> "↑"
        Direction.D -> "↓"
        Direction.L -> "←"
        Direction.R -> "→"
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(color.toComposeColor().copy(alpha = 0.14f), shape)
            .border(1.5.dp, GlassBorder, shape)
            .clickable { onPick(dir) },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = color.toComposeColor(), fontWeight = FontWeight.Black, fontSize = 26.sp)
    }
}
