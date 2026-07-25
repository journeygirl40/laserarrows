package com.laserarrows.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        shape = CircleShape,
        modifier = modifier
            .size(44.dp)
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Text("⌂", fontSize = 19.sp)
        }
    }
}
