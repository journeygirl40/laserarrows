package com.laserarrows.app.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.laserarrows.app.ui.theme.EmberColor
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon2
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary
import kotlin.math.max

@Composable
fun GameOverOverlay(
    level: Int,
    score: Int,
    bestScoreBeforeRun: Int,
    beatBest: Boolean,
    crossedLevelRecord: Boolean,
    isEditorTest: Boolean,
    isImported: Boolean,
    onHome: () -> Unit,
    onAddToFavorites: (memo: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val homeLabel = when {
        isEditorTest -> "✏️ 編集に戻る"
        isImported -> "⭐ お気に入りに戻る"
        else -> "🏠 ホームへ"
    }
    var addingFavorite by remember { mutableStateOf(false) }
    var addedFavorite by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }

    // ニアミス訴求:僅差で更新を逃した時ほど「もう一回」を誘う効果が高い(ギャンブル心理学で言うニアミス効果)。
    val scoreGap = bestScoreBeforeRun - score
    val isNearMiss = !beatBest && bestScoreBeforeRun > 0 && scoreGap > 0 &&
        scoreGap <= max(15, (bestScoreBeforeRun * 0.2).toInt())

    Column(
        modifier = modifier.padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (beatBest) "🏆 自己ベスト更新！" else "GAME OVER",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
        )
        Text("到達レベル: $level ・ スコア: ${score}点", fontSize = 12.5.sp, color = TextMuted)
        if (isNearMiss) {
            Text(
                "あと${scoreGap}点でハイスコア更新だった！惜しい！",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = EmberColor,
            )
        }
        if (crossedLevelRecord) {
            RecordChip("🚀 Lv.$level 到達レベル記録更新", Neon2)
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
        }

        GhostButton(homeLabel, onClick = onHome)
    }
}
