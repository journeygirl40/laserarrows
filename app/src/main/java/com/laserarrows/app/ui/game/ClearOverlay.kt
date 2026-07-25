package com.laserarrows.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

@Composable
fun ClearOverlay(
    totalScore: Int,
    livesLost: Int,
    isEditorTest: Boolean,
    isImported: Boolean,
    onNext: () -> Unit,
    onHome: () -> Unit,
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

    Column(
        modifier = modifier.padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("✨ CLEAR!", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text("スコア ${totalScore}点", fontSize = 12.5.sp, color = TextMuted)
        Box(
            modifier = Modifier
                .background(badgeColor.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(badgeText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = badgeColor)
        }
        NeonButton("次のレベルへ", onClick = onNext)
        GhostButton(homeLabel, onClick = onHome)
    }
}
