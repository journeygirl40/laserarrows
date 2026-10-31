package com.laserarrows.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.theme.CardEdge
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GlassHi
import com.laserarrows.app.ui.theme.Neon1
import com.laserarrows.app.ui.theme.Neon1Edge
import com.laserarrows.app.ui.theme.TextPrimary

/**
 * 厚みのある「ぷっくり」した面。下側に側面(edge)が見え、押すと面が沈んで側面が隠れる。
 * ボタン・カード・タイル共通の土台。onClickがnullなら押せない飾りの面になる。
 */
@Composable
fun ChunkySurface(
    face: Color,
    edge: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    edgeHeight: Dp = 5.dp,
    borderColor: Color = lerp(face, Color.White, 0.45f),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(
        targetValue = if (pressed && onClick != null && enabled) edgeHeight * 0.8f else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "chunkySink",
    )
    // 外から大きさを決められた時(fillMaxSize/weight等)は、面もその大きさいっぱいに広げる。
    Box(modifier = modifier.alpha(if (enabled) 1f else 0.55f), propagateMinConstraints = true) {
        // 側面(厚み)と、その下のやわらかい落ち影。
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = edgeHeight)
                .shadow(6.dp, shape, ambientColor = edge, spotColor = edge.copy(alpha = 0.6f))
                .background(edge, shape),
        )
        var faceModifier = Modifier
            .padding(bottom = edgeHeight)
            .offset(y = sink)
            .background(Brush.verticalGradient(listOf(lerp(face, Color.White, 0.22f), face)), shape)
            .border(1.5.dp, borderColor, shape)
        if (onClick != null) {
            faceModifier = faceModifier.clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
        }
        Box(modifier = faceModifier, contentAlignment = contentAlignment, content = content)
    }
}

/** メインの行動ボタン(若葉の緑・白抜きの太字)。 */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    face: Color = Neon1,
    edge: Color = Neon1Edge,
) {
    ChunkySurface(face = face, edge = edge, modifier = modifier, enabled = enabled, onClick = onClick) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(shadow = Shadow(edge.copy(alpha = 0.8f), Offset(0f, 2.5f), 0f)),
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 13.dp),
        )
    }
}

/** サブの行動ボタン(クリーム色・こげ茶の文字)。 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ChunkySurface(
        face = GlassFill,
        edge = CardEdge,
        modifier = modifier,
        edgeHeight = 4.dp,
        borderColor = GlassHi,
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
        )
    }
}

/** 盤面外の情報を載せるクリーム色の札。 */
@Composable
fun CreamCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    face: Color = GlassFill,
    edge: Color = CardEdge,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    ChunkySurface(
        face = face,
        edge = edge,
        modifier = modifier,
        shape = shape,
        edgeHeight = 4.dp,
        borderColor = if (face == GlassFill) GlassBorder else lerp(face, Color.White, 0.45f),
        onClick = onClick,
        contentAlignment = contentAlignment,
        content = content,
    )
}
