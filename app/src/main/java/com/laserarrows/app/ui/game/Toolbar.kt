package com.laserarrows.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

@Composable
fun Toolbar(
    undoEnabled: Boolean,
    restartEnabled: Boolean,
    addLightActive: Boolean,
    addLightEnabled: Boolean,
    onUndo: () -> Unit,
    onRestartLevel: () -> Unit,
    onToggleAddLight: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolbarButton(
            icon = "↺",
            label = "戻す",
            cost = 1,
            enabled = undoEnabled,
            onClick = onUndo,
            modifier = Modifier.weight(1f),
        )
        ToolbarButton(
            icon = "🔄",
            label = "やり直す",
            cost = 1,
            enabled = restartEnabled,
            onClick = onRestartLevel,
            modifier = Modifier.weight(1f),
        )
        ToolbarButton(
            icon = "➕",
            label = "光源",
            cost = 1,
            enabled = addLightEnabled,
            active = addLightActive,
            onClick = onToggleAddLight,
            modifier = Modifier.weight(1f),
        )
        ToolbarButton(
            icon = "🔗",
            label = "共有",
            cost = 0,
            enabled = true,
            onClick = onShare,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: String,
    label: String,
    cost: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val shape = RoundedCornerShape(14.dp)
    val accent = if (active) GoldColor else Neon2
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 64.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .background(if (active) GoldColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f), shape)
            .border(1.dp, if (active) GoldColor else GlassBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(icon, fontSize = 18.sp, color = accent)
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) GoldColor else TextPrimary,
        )
        if (cost > 0) {
            Text("💔$cost", fontSize = 9.5.sp, color = DangerColor.copy(alpha = 0.85f))
        } else {
            Text(" ", fontSize = 9.5.sp, color = TextMuted)
        }
    }
}
