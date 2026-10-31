package com.laserarrows.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.laserarrows.app.R
import com.laserarrows.app.ui.theme.CardEdge
import com.laserarrows.app.ui.theme.GlassFill

@Composable
fun HomeIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChunkySurface(
        face = GlassFill,
        edge = CardEdge,
        shape = CircleShape,
        edgeHeight = 4.dp,
        onClick = onClick,
        modifier = modifier.size(50.dp),
    ) {
        Image(painterResource(R.drawable.icon_house), contentDescription = "ホーム", modifier = Modifier.size(28.dp))
    }
}
