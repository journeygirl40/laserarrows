package com.laserarrows.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.laserarrows.app.ui.theme.CardEdge
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GlassFill

/** クリーム色のやわらかい面(影つき)。小さな情報ブロックやパネルの下地に使う。 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(4.dp, shape, ambientColor = CardEdge, spotColor = CardEdge)
            .clip(shape)
            .background(GlassFill, shape)
            .border(1.5.dp, GlassBorder, shape),
    ) {
        content()
    }
}
