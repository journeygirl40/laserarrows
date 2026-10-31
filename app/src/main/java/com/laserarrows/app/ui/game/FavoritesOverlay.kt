package com.laserarrows.app.ui.game

import com.laserarrows.app.ui.theme.BoardGrass
import com.laserarrows.app.ui.theme.CardEdge
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.data.FavoriteSortKey
import com.laserarrows.app.data.ImportHistoryEntry
import com.laserarrows.app.engine.BoardCode
import com.laserarrows.app.engine.PuzzleGenerator
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FavoritesOverlay(
    history: List<ImportHistoryEntry>,
    sortKey: FavoriteSortKey,
    sortDescending: Boolean,
    hasActiveRun: Boolean,
    activeLevel: Int,
    onSetSortKey: (FavoriteSortKey) -> Unit,
    onSetSortDescending: (Boolean) -> Unit,
    onSelect: (ImportHistoryEntry) -> Unit,
    onShare: (ImportHistoryEntry) -> Unit,
    onDelete: (ImportHistoryEntry) -> Unit,
    onEditMemo: (ImportHistoryEntry, String) -> Unit,
    onOpenReplay: (ImportHistoryEntry) -> Unit,
    onAddNew: () -> Unit,
    onExportAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingEntry by remember { mutableStateOf<ImportHistoryEntry?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
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

            val sortedHistory = remember(history, sortKey, sortDescending) {
                sortFavorites(history, sortKey, sortDescending)
            }

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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    SortKeyChip("🗓 日付", sortKey == FavoriteSortKey.DATE) { onSetSortKey(FavoriteSortKey.DATE) }
                    SortKeyChip("🎯 レベル", sortKey == FavoriteSortKey.LEVEL) { onSetSortKey(FavoriteSortKey.LEVEL) }
                    SortKeyChip("💔 消費ハート", sortKey == FavoriteSortKey.LIVES_LOST) { onSetSortKey(FavoriteSortKey.LIVES_LOST) }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onSetSortDescending(!sortDescending) }
                        .background(Neon2.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .border(1.dp, Neon2, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(sortDirectionLabel(sortKey, sortDescending), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Neon2)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).widthIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sortedHistory, key = { it.importedAt }) { entry ->
                        FavoriteItem(
                            entry = entry,
                            onClick = {
                                if (hasActiveRun) confirmingEntry = entry else onSelect(entry)
                            },
                            onShare = { onShare(entry) },
                            onDelete = { onDelete(entry) },
                            onEditMemo = { newMemo -> onEditMemo(entry, newMemo) },
                            onOpenReplay = { onOpenReplay(entry) },
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

        confirmingEntry?.let { entry ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x993B2414)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .background(GlassFill, RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("このお気に入りをプレイしますか？", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "現在プレイ中のLv.${activeLevel}の進行状況は消えます。",
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton("キャンセル", onClick = { confirmingEntry = null }, modifier = Modifier.weight(1f))
                        GhostButton(
                            "プレイする",
                            onClick = {
                                onSelect(entry)
                                confirmingEntry = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** レベルは登録時のもの、無ければ盤面から逆算した推定値で並び替える。値が無いエントリは常に末尾に置く(方向を反転しても末尾のまま)。 */
private fun sortFavorites(history: List<ImportHistoryEntry>, key: FavoriteSortKey, descending: Boolean): List<ImportHistoryEntry> =
    when (key) {
        FavoriteSortKey.DATE -> {
            val ordered = history.sortedBy { it.importedAt }
            if (descending) ordered.reversed() else ordered
        }
        FavoriteSortKey.LEVEL -> {
            val withLevel = history.map { entry ->
                entry to (entry.level ?: BoardCode.decode(entry.code)?.let { PuzzleGenerator.estimateLevelForBoard(it) })
            }
            val (known, unknown) = withLevel.partition { it.second != null }
            val orderedKnown = known.sortedBy { it.second }
            (if (descending) orderedKnown.reversed() else orderedKnown).map { it.first } + unknown.map { it.first }
        }
        FavoriteSortKey.LIVES_LOST -> {
            val (known, unknown) = history.partition { it.bestLivesLost != null }
            val orderedKnown = known.sortedBy { it.bestLivesLost }
            (if (descending) orderedKnown.reversed() else orderedKnown) + unknown
        }
    }

private fun sortDirectionLabel(key: FavoriteSortKey, descending: Boolean): String = when (key) {
    FavoriteSortKey.DATE -> if (descending) "⬇ 新しい順" else "⬆ 古い順"
    FavoriteSortKey.LEVEL -> if (descending) "⬇ レベル高い順" else "⬆ レベル低い順"
    FavoriteSortKey.LIVES_LOST -> if (descending) "⬇ 消費ハート多い順" else "⬆ 消費ハート少ない順"
}

@Composable
private fun SortKeyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) GoldColor.copy(alpha = 0.18f) else Color.Transparent,
                RoundedCornerShape(999.dp),
            )
            .border(1.dp, if (selected) GoldColor else TextMuted.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) GoldColor else TextMuted,
        )
    }
}

@Composable
private fun FavoriteItem(
    entry: ImportHistoryEntry,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onEditMemo: (String) -> Unit,
    onOpenReplay: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val dateText = remember(entry.importedAt) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(entry.importedAt))
    }
    val board = remember(entry.code) { BoardCode.decode(entry.code) }
    val displayLevel = remember(entry.level, board) {
        entry.level ?: board?.let { PuzzleGenerator.estimateLevelForBoard(it) }
    }
    var confirmingDelete by remember(entry.importedAt) { mutableStateOf(false) }
    var editingMemo by remember(entry.importedAt) { mutableStateOf(false) }
    var memoText by remember(entry.importedAt) { mutableStateOf(entry.memo) }

    fun saveMemo() {
        onEditMemo(memoText.trim())
        editingMemo = false
    }

    fun cancelMemo() {
        memoText = entry.memo
        editingMemo = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, shape, ambientColor = CardEdge, spotColor = CardEdge)
            .background(GlassFill, shape)
            .border(1.5.dp, GlassBorder, shape)
            .then(if (editingMemo) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // サムネイル+メモの行は、操作アイコンや日付・バッジと幅を取り合わないようにして
            // メモがどんな長さでも(悪条件が重なっても)確実に表示領域を確保できるようにする。
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BoardGrass, RoundedCornerShape(10.dp))
                        .padding(5.dp),
                ) {
                    if (board != null) {
                        BoardPreview(board = board, modifier = Modifier.fillMaxSize())
                    }
                }
                if (editingMemo) {
                    OutlinedTextField(
                        value = memoText,
                        onValueChange = { memoText = it },
                        placeholder = { Text("メモ", fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { saveMemo() }),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "✓",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .clickable { saveMemo() }
                            .padding(6.dp),
                    )
                    Text(
                        "✕",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier
                            .clickable { cancelMemo() }
                            .padding(6.dp),
                    )
                } else {
                    Text(
                        entry.memo.ifBlank { "(メモなし)" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (!editingMemo) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(dateText, fontSize = 11.5.sp, color = TextMuted)
                    Spacer(modifier = Modifier.weight(1f))
                    // Lv./クリア状態のバッジは内容によらず常に同じ幅を確保し、内容が変わっても位置がずれないようにする。
                    Box(
                        modifier = Modifier
                            .widthIn(min = 50.dp)
                            .then(
                                if (displayLevel != null) {
                                    Modifier.background(Neon2.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (displayLevel != null) {
                            Text(
                                "Lv.$displayLevel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Neon2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    val perfect = entry.bestLivesLost == 0
                    val clearColor = if (perfect) GoldColor else DangerColor
                    Box(
                        modifier = Modifier
                            .widthIn(min = 104.dp)
                            .then(
                                if (entry.bestLivesLost != null) {
                                    Modifier.background(clearColor.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (entry.bestLivesLost != null) {
                            Text(
                                if (perfect) "✨ パーフェクト" else "💔×${entry.bestLivesLost}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = clearColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                ) {
                    Text(
                        "✏️",
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable {
                                memoText = entry.memo
                                editingMemo = true
                            }
                            .padding(6.dp),
                    )
                    if (entry.replayLog != null) {
                        Text(
                            "📖",
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable(onClick = onOpenReplay)
                                .padding(6.dp),
                        )
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
    }
}
