package com.laserarrows.app.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.R
import com.laserarrows.app.ui.components.ChunkySurface
import com.laserarrows.app.ui.theme.CardEdge
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GlassHi
import com.laserarrows.app.ui.theme.GoldColor
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
    /** ヒント(ハート1つで最高点になる次の1手を教える)を押せるか。 */
    hintEnabled: Boolean,
    hintLoading: Boolean,
    onHint: () -> Unit,
    /** 今の盤面がもうお気に入りに入っているか(入っていれば「登録済み」にして押せなくする)。 */
    favoriteDone: Boolean,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ToolbarButton(
            icon = R.drawable.icon_back,
            label = "戻す",
            cost = 1,
            enabled = undoEnabled,
            onClick = onUndo,
            modifier = Modifier.weight(1f),
        )
        ToolbarButton(
            icon = R.drawable.icon_undo,
            label = "やり直す",
            cost = 1,
            enabled = restartEnabled,
            onClick = onRestartLevel,
            modifier = Modifier.weight(1f),
        )
        ToolbarButton(
            icon = R.drawable.icon_paw,
            label = "呼ぶ",
            cost = 1,
            enabled = addLightEnabled,
            active = addLightActive,
            onClick = onToggleAddLight,
            modifier = Modifier.weight(1f),
        )
        ToolbarButton(
            icon = R.drawable.icon_bulb,
            label = if (hintLoading) "考え中…" else "ヒント",
            cost = 1,
            enabled = hintEnabled,
            onClick = onHint,
            modifier = Modifier.weight(1f),
        )
        // プレイ中は共有ではなくお気に入り登録(共有はお気に入り画面の各盤面からできる)。
        ToolbarButton(
            icon = R.drawable.icon_bookmark,
            label = if (favoriteDone) "登録済み" else "お気に入り",
            cost = 0,
            enabled = !favoriteDone,
            onClick = onFavorite,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToolbarButton(
    @DrawableRes icon: Int,
    label: String,
    cost: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    ChunkySurface(
        face = if (active) GoldColor else GlassFill,
        edge = if (active) lerp(GoldColor, Color.Black, 0.25f) else CardEdge,
        shape = RoundedCornerShape(18.dp),
        edgeHeight = 4.dp,
        borderColor = if (active) lerp(GoldColor, Color.White, 0.5f) else GlassHi,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier.heightIn(min = 70.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
            // 5つ並ぶと幅が狭いので、ラベルは折り返さず1行に収める。
            Text(
                label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = if (active) Color.White else TextPrimary,
                maxLines = 1,
                softWrap = false,
            )
            // ライフを使う操作は、ハートのコストを小さく添える。
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cost > 0) {
                    Image(painterResource(R.drawable.icon_life), contentDescription = null, modifier = Modifier.size(11.dp))
                    Text("-$cost", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else DangerColor)
                } else {
                    Text(" ", fontSize = 10.sp)
                }
            }
        }
    }
}
