package com.laserarrows.app.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.GlassSurface
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

/** アプリ更新の状態(Google Playの更新チェック結果に応じてMainActivityから渡される)。 */
sealed class AppUpdateUiState {
    data object None : AppUpdateUiState()
    data object Available : AppUpdateUiState()
    data object ReadyToInstall : AppUpdateUiState()
}

@Composable
fun AppUpdateOverlay(
    state: AppUpdateUiState,
    onUpdateClick: () -> Unit,
    onOpenStoreClick: () -> Unit,
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        AppUpdateUiState.None -> Unit
        AppUpdateUiState.Available -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x993B2414)),
                contentAlignment = Alignment.Center,
            ) {
                GlassSurface(shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier.widthIn(max = 320.dp).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("🚀 新しいバージョンがあります", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("更新しますか？", fontSize = 13.sp, color = TextMuted)
                        NeonButton("更新する", onClick = onUpdateClick, modifier = Modifier.fillMaxWidth())
                        GhostButton("🛒 ストアで開く", onClick = onOpenStoreClick, modifier = Modifier.fillMaxWidth())
                        GhostButton("あとで", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        AppUpdateUiState.ReadyToInstall -> {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(GlassFill, RoundedCornerShape(16.dp))
                        .border(1.dp, GoldColor, RoundedCornerShape(16.dp))
                        .clickable(onClick = onInstallClick)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("ダウンロード完了。タップして更新を適用", fontSize = 12.5.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("再起動 ▶", fontSize = 12.5.sp, color = GoldColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
