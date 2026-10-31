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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.laserarrows.app.ui.theme.BoardGrass
import com.laserarrows.app.ui.theme.BoardGrassDark
import com.laserarrows.app.ui.theme.GlassFill
import com.laserarrows.app.ui.theme.GoldColor
import com.laserarrows.app.ui.theme.TextPrimary
import com.laserarrows.app.ui.theme.Wood
import com.laserarrows.app.ui.theme.WoodDark
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
                forestPopulation = uiState.forestPopulation,
                forestStageSeen = uiState.forestStageSeen,
                onForestStageCelebrated = { viewModel.onForestStageCelebrated(it) },
                forestExpansionSeen = uiState.forestExpansionSeen,
                onForestExpansionCelebrated = { viewModel.onForestExpansionCelebrated(it) },
                totalStars = uiState.totalStars,
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
                levelStars = uiState.levelStars,
                hasActiveRun = uiState.hasActiveRun,
                activeLevel = uiState.level,
                initialExcludePerfect = uiState.levelListExcludePerfect,
                sortDescending = uiState.levelListSortDescending,
                onToggleSort = { viewModel.setLevelListSortDescending(!uiState.levelListSortDescending) },
                onSelectLevel = { viewModel.selectLevel(it) },
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

        if (uiState.showEggIntro && uiState.overlay == Overlay.None) {
            EggIntroDialog(onClose = { viewModel.dismissEggIntro() })
        } else if (uiState.showScoreHelp && uiState.overlay == Overlay.None) {
            ScoreHelpDialog(onClose = { viewModel.dismissScoreHelp() })
        }

        // 盤面づくりが1秒以上かかった時だけ読み込みバーを出す(すぐ終わる時にチラつかせない)。
        val loadingProgress = uiState.loadingProgress
        var showLoading by remember { mutableStateOf(false) }
        LaunchedEffect(loadingProgress != null) {
            showLoading = false
            if (loadingProgress != null) {
                delay(1000)
                showLoading = true
            }
        }
        if (showLoading && loadingProgress != null) {
            LoadingOverlay(progress = loadingProgress)
        }

        uiState.toast?.let { toast ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .background(GlassFill, RoundedCornerShape(20.dp))
                    .border(2.dp, if (toast.isDanger) DangerColor else GoldColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    toast.text,
                    color = if (toast.isDanger) DangerColor else TextPrimary,
                    fontSize = 13.sp,
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
                levelScore = uiState.levelScore,
                par = uiState.par,
                onScoreHelp = { viewModel.openScoreHelp() },
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
                // マスの大きさと隙間を画面の1ピクセル単位にそろえる。半端なピクセルのままだと、行ごとに四捨五入の向きが変わって
                // マスの位置が1〜2ピクセルずつ上下にずれ、上に重ねて描くキャラクターや線ともずれてしまう。
                val density = LocalDensity.current
                fun androidx.compose.ui.unit.Dp.snapToPx(): androidx.compose.ui.unit.Dp = with(density) { kotlin.math.floor(toPx()).coerceAtLeast(0f).toDp() }
                val gapDp = 5.dp.snapToPx()
                val marginDp = 10.dp

                val cellSizeDp: androidx.compose.ui.unit.Dp
                val boardWidthDp: androidx.compose.ui.unit.Dp
                val boardHeightDp: androidx.compose.ui.unit.Dp
                if (hasBoard) {
                    val availW = maxWidth - marginDp * 2
                    val availH = maxHeight - marginDp * 2
                    val cellFromW = (availW - gapDp * (board.cols - 1)) / board.cols
                    val cellFromH = (availH - gapDp * (board.rows - 1)) / board.rows
                    cellSizeDp = (if (cellFromW < cellFromH) cellFromW else cellFromH).snapToPx()
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

                // 盤面は木枠に囲まれた芝生の上に置く(手触りのある「おもちゃ箱」感で、作り込みの印象を出す)。
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(panelWidthDp, panelHeightDp)
                        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = WoodDark, spotColor = WoodDark)
                        .background(Brush.verticalGradient(listOf(lerp(Wood, Color.White, 0.18f), Wood)), RoundedCornerShape(24.dp))
                        .border(2.5.dp, WoodDark, RoundedCornerShape(24.dp))
                        .padding(5.dp)
                        .background(Brush.verticalGradient(listOf(BoardGrass, BoardGrassDark)), RoundedCornerShape(19.dp)),
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
                                meetingCell = uiState.activeShot?.takeIf { !it.hitIsShooterClash }?.result?.hit,
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
                            uiState.scoreBonusPopup?.let { popup ->
                                val at = popup.at
                                val color = popup.color
                                if (at != null && color != null) {
                                    key(popup.token) {
                                        BirthPopupView(at, color, popup.gained, cellSizeDp, gapDp)
                                    }
                                }
                            }
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
                            shape = RoundedCornerShape(19.dp),
                            modifier = Modifier.fillMaxSize(),
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
                                        levelScore = overlay.levelScore,
                                        par = overlay.par,
                                        stars = overlay.stars,
                                        isEditorTest = uiState.isEditorTest,
                                        isImported = uiState.isImported,
                                        crossedHighScore = uiState.crossedHighScoreThisRun,
                                        crossedLevelRecord = uiState.crossedLevelRecordThisRun,
                                        level = uiState.level,
                                        onNext = { viewModel.clearNext() },
                                        onHome = { viewModel.clearHome() },
                                        onAddToFavorites = { memo -> viewModel.addCurrentBoardToFavorites(memo) },
                                        populationGain = overlay.populationGain,
                                        doubled = overlay.doubled,
                                        population = uiState.forestPopulation,
                                        onWatchAdToDouble = {
                                            val activity = context as? Activity
                                            val app = context.applicationContext as LaserArrowsApplication
                                            if (activity != null) {
                                                app.rewardedAdManager.show(
                                                    activity = activity,
                                                    onRewarded = { viewModel.doubleClearRewardWithAd() },
                                                    onUnavailable = { viewModel.notifyAdUnavailable() },
                                                )
                                            } else {
                                                viewModel.notifyAdUnavailable()
                                            }
                                        },
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

        // クリア画面の間は、使い道のないツールバーの場所に「出会えたふたり組」のパレードを出す。
        // ツールバー自体は透明にして残し、盤面の大きさ(=クリア花火の位置)が変わらないようにする。
        val parade = (uiState.overlay as? Overlay.Clear)?.takeIf { it.couples.isNotEmpty() }
        Box(modifier = Modifier.fillMaxWidth()) {
            Toolbar(
                undoEnabled = parade == null && uiState.history.isNotEmpty() && uiState.lives > 0,
                restartEnabled = parade == null && uiState.lives > 0,
                addLightActive = uiState.addingLight,
                addLightEnabled = parade == null && uiState.lives > 0,
                onUndo = { viewModel.undo() },
                onRestartLevel = { viewModel.restartLevel() },
                onToggleAddLight = { viewModel.toggleAddLight() },
                onShare = {
                    if (parade == null) {
                        val code = viewModel.exportCurrentBoard()
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, code)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    }
                },
                modifier = Modifier.padding(horizontal = 2.dp).alpha(if (parade == null) 1f else 0f),
            )
            if (parade != null) {
                MeetParade(
                    couples = parade.couples,
                    stars = parade.stars,
                    levelScore = parade.levelScore,
                    par = parade.par,
                    onCouplePop = { viewModel.onParadePop(it) },
                    onAllMet = { viewModel.onParadeAllMet(parade.stars) },
                    modifier = Modifier.matchParentSize().padding(horizontal = 2.dp),
                )
            }
        }
    }
}
