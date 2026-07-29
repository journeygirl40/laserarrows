package com.laserarrows.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.Neon1
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.TextPrimary

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        text = text,
        color = TextPrimary.copy(alpha = if (enabled) 1f else 0.4f),
        fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Brush.linearGradient(listOf(Neon1, PurpleAccent)).let { if (enabled) it else Brush.linearGradient(listOf(GlassFill, GlassFill)) }, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 14.dp),
    )
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        text = text,
        color = TextPrimary.copy(alpha = if (enabled) 1f else 0.4f),
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(GlassFill, shape)
            .border(1.dp, GlassBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
    )
}
