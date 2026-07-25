package com.laserarrows.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.data.ImportHistoryEntry
import com.laserarrows.app.engine.BoardCode
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.Bg1
import com.laserarrows.app.ui.theme.Bg3
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FavoritesOverlay(
    history: List<ImportHistoryEntry>,
    onSelect: (ImportHistoryEntry) -> Unit,
    onShare: (ImportHistoryEntry) -> Unit,
    onDelete: (ImportHistoryEntry) -> Unit,
    onAddNew: () -> Unit,
    onExportAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg1, Bg3))),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp, start = 8.dp)
                .size(240.dp)
                .background(
                    Brush.radialGradient(listOf(GoldColor.copy(alpha = 0.24f), Color.Transparent)),
                    CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 40.dp, end = 8.dp)
                .size(220.dp)
                .background(
                    Brush.radialGradient(listOf(PurpleAccent.copy(alpha = 0.20f), Color.Transparent)),
                    CircleShape,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("⭐ お気に入り", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "気に入ったパズルをいつでも呼び出せる、あなただけのコレクション。",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Start,
                )
            }

            NeonButton("＋ 新しいコードを追加", onClick = onAddNew, modifier = Modifier.fillMaxWidth())

            if (history.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌟", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "まだお気に入りがありません",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "友達からもらったコードを追加してみましょう",
                            fontSize = 12.5.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Text(
                    "タップするとその盤面をもう一度プレイできます",
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Start,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f).widthIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(history, key = { it.importedAt }) { entry ->
                        FavoriteItem(
                            entry = entry,
                            onClick = { onSelect(entry) },
                            onShare = { onShare(entry) },
                            onDelete = { onDelete(entry) },
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                GhostButton(
                    text = "📤 全て書き出す",
                    onClick = onExportAll,
                    modifier = Modifier.weight(1f),
                )
                GhostButton("戻る", onClick = onBack, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    entry: ImportHistoryEntry,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val dateText = remember(entry.importedAt) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(entry.importedAt))
    }
    val board = remember(entry.code) { BoardCode.decode(entry.code) }
    var confirmingDelete by remember(entry.importedAt) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(GoldColor.copy(alpha = 0.14f), PurpleAccent.copy(alpha = 0.12f))),
                shape,
            )
            .border(1.dp, GoldColor.copy(alpha = 0.4f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                    .padding(5.dp),
            ) {
                if (board != null) {
                    BoardPreview(board = board, modifier = Modifier.fillMaxSize())
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.memo.ifBlank { "(メモなし)" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                )
                Text(dateText, fontSize = 11.5.sp, color = TextMuted)
            }
            Text(
                "📤",
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable { onShare() }
                    .padding(6.dp),
            )
            Text(
                if (confirmingDelete) "削除？" else "🗑",
                fontSize = if (confirmingDelete) 12.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (confirmingDelete) DangerColor else TextPrimary,
                modifier = Modifier
                    .clickable {
                        if (confirmingDelete) onDelete() else confirmingDelete = true
                    }
                    .padding(6.dp),
            )
        }
    }
}
