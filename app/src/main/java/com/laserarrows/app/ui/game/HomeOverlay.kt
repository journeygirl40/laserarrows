package com.laserarrows.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.NeonButton
import com.laserarrows.app.ui.theme.Bg1
import com.laserarrows.app.ui.theme.Bg3
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.Neon1
import com.laserarrows.app.ui.theme.PurpleAccent
import com.laserarrows.app.ui.theme.TextMuted
import com.laserarrows.app.ui.theme.TextPrimary

@Composable
fun HomeOverlay(
    bestLevel: Int,
    bestScore: Int,
    hasProgress: Boolean,
    nextLevel: Int,
    hasActiveRun: Boolean,
    activeLevel: Int,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onResume: () -> Unit,
    onShowList: () -> Unit,
    onShowFavorites: () -> Unit,
    onShowEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg1, Bg3))),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .size(280.dp)
                .background(
                    Brush.radialGradient(listOf(Neon1.copy(alpha = 0.30f), Color.Transparent)),
                    CircleShape,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 28.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Brush.linearGradient(listOf(Neon1, PurpleAccent)), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚡", fontSize = 30.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "レーザー・リンク",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "色を読んで、光をつなげ。",
                    fontSize = 13.sp,
                    color = TextMuted,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text(
                        "自己ベスト  Lv.$bestLevel ・ ${bestScore}点",
                        fontSize = 12.5.sp,
                        color = GoldColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    hasActiveRun -> {
                        NeonButton(
                            "▶ 再開する (Lv.$activeLevel)",
                            onClick = onResume,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        GhostButton("🔄 最初から", onClick = onStart, modifier = Modifier.fillMaxWidth())
                    }
                    hasProgress -> {
                        NeonButton(
                            "▶ 続きから (Lv.$nextLevel)",
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        GhostButton("🔄 最初から", onClick = onStart, modifier = Modifier.fillMaxWidth())
                    }
                    else -> {
                        NeonButton("▶ 挑戦する", onClick = onStart, modifier = Modifier.fillMaxWidth())
                    }
                }
                GhostButton("📜 レベル一覧", onClick = onShowList, modifier = Modifier.fillMaxWidth())
                GhostButton("⭐ お気に入り", onClick = onShowFavorites, modifier = Modifier.fillMaxWidth())
                GhostButton("🛠 自分で作る", onClick = onShowEditor, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
