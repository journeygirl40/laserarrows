package com.laserarrows.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

@Composable
fun ClearOverlay(
    totalScore: Int,
    livesLost: Int,
    isEditorTest: Boolean,
    isImported: Boolean,
    crossedHighScore: Boolean,
    crossedLevelRecord: Boolean,
    level: Int,
    onNext: () -> Unit,
    onHome: () -> Unit,
    onAddToFavorites: (memo: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val perfect = livesLost == 0
    val badgeColor = if (perfect) GoldColor else DangerColor
    val badgeText = if (perfect) "✨ パーフェクトクリア!" else "💔×$livesLost 消費してクリア"
    val homeLabel = when {
        isEditorTest -> "✏️ 編集に戻る"
        isImported -> "⭐ お気に入りに戻る"
        else -> "🏠 ホームへ"
    }
    var addingFavorite by remember { mutableStateOf(false) }
    var addedFavorite by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("✨ CLEAR!", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text("スコア ${totalScore}点", fontSize = 12.5.sp, color = TextMuted)
        Box(
            modifier = Modifier
                .background(badgeColor.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(badgeText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = badgeColor)
        }
        if (!isImported && (crossedHighScore || crossedLevelRecord)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (crossedHighScore) {
                    RecordChip("🏆 ハイスコア更新", GoldColor)
                }
                if (crossedLevelRecord) {
                    RecordChip("🚀 Lv.$level 到達レベル記録更新", Neon2)
                }
            }
        }

        if (!isImported) {
            when {
                addedFavorite -> {
                    Text("⭐ お気に入りに登録しました", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldColor)
                }
                addingFavorite -> {
                    OutlinedTextField(
                        value = memo,
                        onValueChange = { memo = it },
                        placeholder = { Text("メモ(任意・例:誰との対戦か)", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.widthIn(max = 280.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhostButton(
                            "⭐ 登録する",
                            onClick = {
                                onAddToFavorites(memo)
                                addedFavorite = true
                            },
                        )
                        GhostButton("キャンセル", onClick = { addingFavorite = false; memo = "" })
                    }
                }
                else -> {
                    GhostButton("⭐ お気に入りへ登録", onClick = { addingFavorite = true })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("次のレベルへ", onClick = onNext, modifier = Modifier.weight(1f))
                GhostButton(homeLabel, onClick = onHome, modifier = Modifier.weight(1f))
            }
        } else {
            GhostButton(homeLabel, onClick = onHome)
        }
    }
}

@Composable
fun RecordChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
