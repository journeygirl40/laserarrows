package com.laserarrows.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 横向き(横長)の画面か。画面を回してもActivityを作り直さない設定なので、回転に合わせてここが切り替わり、
 * 各画面は縦向き/横向きのレイアウトを選び直す(マルチウィンドウで横長になった時も横向き扱い)。
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

/**
 * 中身を上下中央に置き、入りきらない時だけ縦にスクロールできる入れ物。
 * 横向きの低い画面で、カードやダイアログが画面からはみ出して押せなくなるのを防ぐ。
 */
@Composable
fun CenteredScroll(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = modifier) {
        val minHeight = maxHeight
        Box(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
    }
}

/**
 * 「見出し → 一覧や盤面 → 下のボタン」でできた画面の共通の並べ方。
 * 縦向き: 上から見出し、残りの高さいっぱいに[main]、いちばん下にボタン。
 * 横向き: 左の列に見出しとボタン(見出しが入りきらなければスクロール)、右に[main]を高さいっぱいに。
 */
@Composable
fun AdaptiveScreen(
    modifier: Modifier = Modifier,
    portraitPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
    spacing: Dp = 14.dp,
    sideWidth: Dp = 300.dp,
    header: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
    main: @Composable (Modifier) -> Unit,
) {
    if (isLandscape()) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(modifier = Modifier.width(sideWidth).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    content = header,
                )
                footer()
            }
            main(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(portraitPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            header()
            main(Modifier.weight(1f).fillMaxWidth())
            footer()
        }
    }
}
