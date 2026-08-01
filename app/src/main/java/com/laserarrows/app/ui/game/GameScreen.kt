package com.laserarrows.app.ui.game

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laserarrows.app.LaserArrowsApplication
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.ui.components.GhostButton
import com.laserarrows.app.ui.components.GlassSurface
import com.laserarrows.app.ui.components.HomeIconButton
import com.laserarrows.app.ui.theme.DangerColor
import com.laserarrows.app.ui.theme.GlassBorder
import com.laserarrows.app.ui.theme.GoldColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toast?.token) {
        if (uiState.toast != null) {
            delay(1300)
            viewModel.consumeToast()
        }
    }

    LaunchedEffect(uiState.comboPopup?.token) {
        if (uiState.comboPopup != null) {
            delay(900)
            viewModel.consumeComboPopup()
        }
    }

    LaunchedEffect(uiState.scoreBonusPopup?.token) {
        if (uiState.scoreBonusPopup != null) {
            delay(800)
            viewModel.consumeScoreBonusPopup()
        }
    }

    LaunchedEffect(uiState.highScorePopup?.token) {
        if (uiState.highScorePopup != null) {
            delay(1200)
            viewModel.consumeHighScorePopup()
        }
    }

    LaunchedEffect(uiState.levelRecordPopup?.token) {
        if (uiState.levelRecordPopup != null) {
            delay(1200)
            viewModel.consumeLevelRecordPopup()
        }
    }

    val nextLevel = (uiState.clearedLevels.maxOrNull() ?: 0) + 1
    val context = LocalContext.current

    // ホーム画面(overlayスタックの一番外側)以外では、Android標準の「戻る」ボタンでアプリごと終了させず、
    // 各画面のonBack相当の処理に委ねる。ホーム画面ではfalseにしてシステム標準の挙動(アプリ終了)に任せる。
    BackHandler(enabled = uiState.overlay != Overlay.Home) {
        when (uiState.overlay) {
            Overlay.Home -> Unit
            Overlay.Editor -> viewModel.editorBack()
            Overlay.LevelList -> viewModel.listBack()
            Overlay.Favorites -> viewModel.favoritesBack()
            Overlay.Import -> viewModel.importBack()
            is Overlay.ColorPick, is Overlay.DirectionPick -> viewModel.cancelLightPick()
            is Overlay.Clear -> viewModel.clearHome()
            is Overlay.GameOver -> viewModel.gameOverHome()
            is Overlay.Replay -> viewModel.closeReplay()
            Overlay.None -> viewModel.goHome()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.overlay == Overlay.Home -> HomeOverlay(
                bestScore = uiState.bestScore,
                nextLevel = nextLevel,
                hasActiveRun = uiState.hasActiveRun,
                activeLevel = uiState.level,
                activeScore = uiState.score,
                activeLives = uiState.lives,
                onHighScoreChallenge = { viewModel.goStart() },
                onLevelUpChallenge = { viewModel.goContinue() },
                onResume = { viewModel.resume() },
                onShowList = { viewModel.goList() },
                onShowFavorites = { viewModel.goFavorites() },
                onShowEditor = { viewModel.goEditor() },
            )

            uiState.overlay == Overlay.Editor -> EditorScreen(
                editorState = uiState.editorState,
                onBack = { viewModel.editorBack() },
                onSetSize = { r, c -> viewModel.editorSetSize(r, c) },
                onSelectTool = { viewModel.editorSelectTool(it) },
                onSelectColor = { viewModel.editorSelectColor(it) },
                onSelectDirection = { viewModel.editorSelectDirection(it) },
                onTapCell = { viewModel.editorTapCell(it) },
                onClearAll = { viewModel.editorClearAll() },
                onTestPlay = { viewModel.editorTestPlay() },
                onShare = {
                    val code = viewModel.editorExportCode()
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, code)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
            )

            uiState.overlay == Overlay.LevelList -> LevelListOverlay(
                clearedLevels = uiState.clearedLevels,
                levelBestLivesLost = uiState.levelBestLivesLost,
                levelBestScore = uiState.levelBestScore,
                hasActiveRun = uiState.hasActiveRun,
                activeLevel = uiState.level,
                initialExcludePerfect = uiState.levelListExcludePerfect,
                sortDescending = uiState.levelListSortDescending,
                onToggleSort = { viewModel.setLevelListSortDescending(!uiState.levelListSortDescending) },
                onSelectLevel = { viewModel.selectLevel(it) },
                onResetRatings = { viewModel.resetLevelRatings() },
                onBack = { viewModel.listBack() },
            )

            uiState.overlay == Overlay.Favorites -> FavoritesOverlay(
                history = uiState.importHistory,
                sortKey = uiState.favoriteSortKey,
                sortDescending = uiState.favoriteSortDescending,
                hasActiveRun = uiState.hasActiveRun,
                activeLevel = uiState.level,
                onSetSortKey = { viewModel.setFavoriteSortKey(it) },
                onSetSortDescending = { viewModel.setFavoriteSortDescending(it) },
                onSelect = { viewModel.selectFavorite(it) },
                onShare = { entry ->
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, entry.code)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
                onDelete = { viewModel.removeFavorite(it) },
                onEditMemo = { entry, newMemo -> viewModel.updateFavoriteMemo(entry, newMemo) },
                onOpenReplay = { viewModel.openReplay(it) },
                onAddNew = { viewModel.goImport() },
                onExportAll = {
                    val text = viewModel.exportFavorites()
                    if (text.isNotBlank()) {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                },
                onBack = { viewModel.favoritesBack() },
            )

            uiState.overlay == Overlay.Import -> ImportOverlay(
                onImport = { code, memo -> viewModel.importBoard(code, memo) },
                onCancel = { viewModel.importBack() },
            )

            uiState.overlay is Overlay.Replay -> ReplayOverlay(
                replay = uiState.overlay as Overlay.Replay,
                onStepBack = { viewModel.replayStep(-1) },
                onStepForward = { viewModel.replayStep(1) },
                onClose = { viewModel.closeReplay() },
            )

            else -> GameplayContent(uiState, viewModel, context)
        }

        uiState.toast?.let { toast ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 8.dp)
                    .background(Color(0xE0140A20), RoundedCornerShape(20.dp))
                    .border(1.dp, if (toast.isDanger) DangerColor else GoldColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 7.dp),
            ) {
                Text(
                    toast.text,
                    color = if (toast.isDanger) DangerColor else GoldColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun GameplayContent(uiState: GameUiState, viewModel: GameViewModel, context: android.content.Context) {
    val scope = rememberCoroutineScope()
    var shakeCell by remember { mutableStateOf<CellPos?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.isEditorTest) {
                GhostButton("✏️ 編集に戻る", onClick = { viewModel.goHome() })
            } else {
                HomeIconButton(onClick = { viewModel.goHome() })
            }
            HudBar(
                levelLabel = if (uiState.isEditorTest) "テスト" else uiState.level.toString(),
                score = uiState.score,
                combo = uiState.combo,
                lives = uiState.lives,
                bestScore = uiState.bestScoreAtRunStart,
                crossedHighScore = uiState.crossedHighScoreThisRun,
                inLevelRecordTerritory = uiState.crossedLevelRecordThisRun,
                scoreBonusPopup = uiState.scoreBonusPopup,
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val board = uiState.board
                val hasBoard = board.cols > 0 && board.rows > 0
                val gapDp = 5.dp
                val marginDp = 10.dp

                val cellSizeDp: androidx.compose.ui.unit.Dp
                val boardWidthDp: androidx.compose.ui.unit.Dp
                val boardHeightDp: androidx.compose.ui.unit.Dp
                if (hasBoard) {
                    val availW = maxWidth - marginDp * 2
                    val availH = maxHeight - marginDp * 2
                    val cellFromW = (availW - gapDp * (board.cols - 1)) / board.cols
                    val cellFromH = (availH - gapDp * (board.rows - 1)) / board.rows
                    cellSizeDp = if (cellFromW < cellFromH) cellFromW else cellFromH
                    boardWidthDp = cellSizeDp * board.cols + gapDp * (board.cols - 1)
                    boardHeightDp = cellSizeDp * board.rows + gapDp * (board.rows - 1)
                } else {
                    val side = (minOf(maxWidth, maxHeight) - marginDp * 2).coerceAtLeast(0.dp)
                    cellSizeDp = side
                    boardWidthDp = side
                    boardHeightDp = side
                }
                val panelWidthDp = boardWidthDp + marginDp * 2
                val panelHeightDp = boardHeightDp + marginDp * 2
                val cellSizePx = with(LocalDensity.current) { cellSizeDp.toPx() }
                val gapPx = with(LocalDensity.current) { gapDp.toPx() }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(panelWidthDp, panelHeightDp)
                        .background(Color(0x0EFFFFFF), RoundedCornerShape(22.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (hasBoard) {
                        Box(modifier = Modifier.size(boardWidthDp, boardHeightDp)) {
                            BoardView(
                                board = board,
                                cellSizeDp = cellSizeDp,
                                shakeCell = shakeCell,
                                onTapCell = { viewModel.handleTap(it) },
                                emptyCellsTappable = uiState.addingLight,
                                firingCell = uiState.activeShot?.originKey,
                            )
                            BeamCanvas(
                                shot = uiState.activeShot,
                                cellSizePx = cellSizePx,
                                gapPx = gapPx,
                                onRevealFinished = {
                                    viewModel.onShotRevealFinished()
                                    val shot = uiState.activeShot
                                    if (shot != null && shot.result.hit == null) {
                                        shakeCell = shot.originKey
                                        scope.launch {
                                            delay(300)
                                            shakeCell = null
                                        }
                                    }
                                },
                                onSettled = { viewModel.onShotSettled() },
                                modifier = Modifier.size(boardWidthDp, boardHeightDp),
                            )
                            if (uiState.overlay is Overlay.Clear) {
                                ClearFinaleCanvas(
                                    finale = uiState.clearFinale,
                                    cellSizePx = cellSizePx,
                                    gapPx = gapPx,
                                    modifier = Modifier.size(boardWidthDp, boardHeightDp),
                                )
                            }
                        }
                    }

                    uiState.comboPopup?.let { popup ->
                        key(popup.token) {
                            ComboPopupView(
                                combo = popup.combo,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }

                    uiState.highScorePopup?.let { popup ->
                        key(popup.token) {
                            HighScorePopupView(
                                score = popup.score,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                            )
                        }
                    }

                    uiState.levelRecordPopup?.let { popup ->
                        key(popup.token) {
                            LevelRecordPopupView(
                                level = popup.level,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                            )
                        }
                    }

                    if (uiState.overlay != Overlay.None) {
                        GlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xB80A0814), RoundedCornerShape(22.dp)),
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                when (val overlay = uiState.overlay) {
                                    Overlay.Home -> Unit
                                    Overlay.Editor -> Unit
                                    Overlay.LevelList -> Unit
                                    Overlay.Favorites -> Unit
                                    Overlay.Import -> Unit
                                    is Overlay.Clear -> ClearOverlay(
                                        totalScore = overlay.totalScore,
                                        livesLost = overlay.livesLost,
                                        isEditorTest = uiState.isEditorTest,
                                        isImported = uiState.isImported,
                                        crossedHighScore = uiState.crossedHighScoreThisRun,
                                        crossedLevelRecord = uiState.crossedLevelRecordThisRun,
                                        level = uiState.level,
                                        onNext = { viewModel.clearNext() },
                                        onHome = { viewModel.clearHome() },
                                        onAddToFavorites = { memo -> viewModel.addCurrentBoardToFavorites(memo) },
                                    )
                                    is Overlay.GameOver -> GameOverOverlay(
                                        level = uiState.level,
                                        score = uiState.score,
                                        bestScoreBeforeRun = uiState.bestScoreAtRunStart,
                                        beatBest = overlay.beatBest,
                                        crossedLevelRecord = uiState.crossedLevelRecordThisRun,
                                        isEditorTest = uiState.isEditorTest,
                                        isImported = uiState.isImported,
                                        onHome = { viewModel.gameOverHome() },
                                        onAddToFavorites = { memo -> viewModel.addCurrentBoardToFavorites(memo) },
                                        onWatchAdToRevive = {
                                            val activity = context as? Activity
                                            val app = context.applicationContext as LaserArrowsApplication
                                            if (activity != null) {
                                                app.rewardedAdManager.show(
                                                    activity = activity,
                                                    onRewarded = { viewModel.reviveWithAd() },
                                                    onUnavailable = { viewModel.notifyAdUnavailable() },
                                                )
                                            } else {
                                                viewModel.notifyAdUnavailable()
                                            }
                                        },
                                    )
                                    is Overlay.ColorPick -> ColorPickOverlay(
                                        choices = overlay.choices,
                                        onPick = { viewModel.confirmLightColor(it) },
                                        onCancel = { viewModel.cancelLightPick() },
                                    )
                                    is Overlay.DirectionPick -> DirectionPickOverlay(
                                        color = overlay.color,
                                        onPick = { viewModel.confirmLightDirection(it) },
                                        onCancel = { viewModel.cancelLightPick() },
                                    )
                                    Overlay.None -> Unit
                                    is Overlay.Replay -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }

        GlassSurface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Toolbar(
                undoEnabled = uiState.history.isNotEmpty() && uiState.lives > 0,
                restartEnabled = uiState.lives > 0,
                addLightActive = uiState.addingLight,
                addLightEnabled = uiState.lives > 0,
                onUndo = { viewModel.undo() },
                onRestartLevel = { viewModel.restartLevel() },
                onToggleAddLight = { viewModel.toggleAddLight() },
                onShare = {
                    val code = viewModel.exportCurrentBoard()
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, code)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
