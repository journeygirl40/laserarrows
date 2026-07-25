package com.laserarrows.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laserarrows.app.LaserArrowsApplication
import com.laserarrows.app.audio.SfxEngine
import com.laserarrows.app.data.BestRecord
import com.laserarrows.app.data.ImportHistoryEntry
import com.laserarrows.app.data.ProgressStore
import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.BoardCode
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.engine.LaserSimulator
import com.laserarrows.app.engine.PuzzleGenerator
import com.laserarrows.app.engine.ScoreRules
import com.laserarrows.app.engine.Tile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameViewModel(
    private val progressStore: ProgressStore,
    private val sfxEngine: SfxEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            progressStore.best.collect { best ->
                _uiState.update { it.copy(bestLevel = best.level, bestScore = best.score) }
            }
        }
        viewModelScope.launch {
            progressStore.clearedLevels.collect { cleared ->
                _uiState.update { it.copy(clearedLevels = cleared) }
            }
        }
        viewModelScope.launch {
            progressStore.levelBestLivesLost.collect { best ->
                _uiState.update { it.copy(levelBestLivesLost = best) }
            }
        }
        viewModelScope.launch {
            progressStore.levelBestScore.collect { best ->
                _uiState.update { it.copy(levelBestScore = best) }
            }
        }
        viewModelScope.launch {
            progressStore.importHistory.collect { history ->
                _uiState.update { it.copy(importHistory = history) }
            }
        }
    }

    private fun maxLevelReached(): Int = _uiState.value.clearedLevels.maxOrNull() ?: 0

    // ---- 画面遷移 ----
    fun goStart() = newRun(1)
    fun goContinue() = newRun(maxLevelReached() + 1)
    fun goList() = _uiState.update { it.copy(overlay = Overlay.LevelList) }
    fun listBack() = _uiState.update { it.copy(overlay = Overlay.Home) }

    /** レベル一覧の「パーフェクト/消費ライフ/レベルごとのベストスコア」評価を全レベル分リセットする(クリア済み記録・自己ベスト記録は変更しない)。 */
    fun resetLevelRatings() {
        viewModelScope.launch { progressStore.resetLevelRatings() }
        _uiState.update { it.copy(levelBestLivesLost = emptyMap(), levelBestScore = emptyMap()) }
        showToast("🗑 レベルの評価をリセットしました")
    }
    fun goImport() = _uiState.update { it.copy(overlay = Overlay.Import) }
    fun importBack() = _uiState.update { it.copy(overlay = Overlay.Favorites) }
    fun goFavorites() = _uiState.update { it.copy(overlay = Overlay.Favorites) }
    fun favoritesBack() = _uiState.update { it.copy(overlay = Overlay.Home) }
    fun goEditor() = _uiState.update { it.copy(overlay = Overlay.Editor) }
    fun editorBack() = _uiState.update { it.copy(overlay = Overlay.Home) }
    fun clearHome() = _uiState.update {
        it.copy(
            overlay = when {
                it.isEditorTest -> Overlay.Editor
                it.isImported -> Overlay.Favorites
                else -> Overlay.Home
            },
        )
    }
    fun gameOverHome() = _uiState.update { it.copy(overlay = if (it.isEditorTest) Overlay.Editor else Overlay.Home) }

    /** プレイ中にいつでもホームへ(自作パズルのテストプレイ中はエディタへ)。盤面・スコア・ライフ等はそのまま保持し、後で再開できる。 */
    fun goHome() {
        val current = _uiState.value
        if (current.busy) return
        _uiState.update {
            it.copy(
                overlay = if (it.isEditorTest) Overlay.Editor else Overlay.Home,
                addingLight = false,
                pendingLightCell = null,
            )
        }
    }

    /** ホームから、保持しておいたプレイ中の状態にそのまま戻る。 */
    fun resume() {
        if (!_uiState.value.hasActiveRun) return
        _uiState.update { it.copy(overlay = Overlay.None) }
    }
    fun clearNext() {
        val current = _uiState.value
        val nextLevel = if (current.isImported) maxLevelReached() + 1 else current.level + 1
        _uiState.update { it.copy(level = nextLevel) }
        startLevel()
    }
    fun selectLevel(level: Int) = newRun(level)
    fun debugGoToLevel(level: Int) {
        if (level < 1) return
        newRun(level)
    }

    fun consumeToast() = _uiState.update { it.copy(toast = null) }
    fun consumeComboPopup() = _uiState.update { it.copy(comboPopup = null) }

    private fun newRun(startLevel: Int) {
        _uiState.update {
            GameUiState(
                overlay = Overlay.None,
                level = startLevel,
                bestLevel = it.bestLevel,
                bestScore = it.bestScore,
                clearedLevels = it.clearedLevels,
                levelBestLivesLost = it.levelBestLivesLost,
                levelBestScore = it.levelBestScore,
                importHistory = it.importHistory,
            )
        }
        startLevel()
    }

    private fun startLevel() {
        viewModelScope.launch {
            val level = _uiState.value.level
            val size = PuzzleGenerator.levelSize(level)
            val board = withContext(Dispatchers.Default) {
                PuzzleGenerator.generate(size.rows, size.cols, size.numPairs)
            }
            val remaining = board.tiles.values.count { it is Tile.Receiver }
            _uiState.update {
                it.copy(
                    board = board,
                    remainingReceivers = remaining,
                    busy = false,
                    history = emptyList(),
                    levelSnapshot = LevelSnapshot(board, it.lives, it.score),
                    overlay = Overlay.None,
                    hasActiveRun = true,
                    isImported = false,
                    isEditorTest = false,
                )
            }
        }
    }

    /** この盤面の初期状態(プレイ開始時点のスナップショット)を共有コードへ変換する(純粋関数、UIで共有シートを呼び出す)。 */
    fun exportCurrentBoard(): String {
        val current = _uiState.value
        return BoardCode.encode(current.levelSnapshot?.board ?: current.board)
    }

    /** 今プレイしていた盤面の初期状態を、コードの貼り付けなしでそのままお気に入りに登録する。 */
    fun addCurrentBoardToFavorites(memo: String = "") {
        val current = _uiState.value
        val board = current.levelSnapshot?.board ?: current.board
        val code = BoardCode.encode(board)
        viewModelScope.launch { progressStore.addImportHistory(code, memo) }
        showToast("⭐ お気に入りに登録しました")
    }

    /** お気に入りの全件を、日時・メモ付きのテキストへまとめる(共有シート用)。 */
    fun exportFavorites(): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return _uiState.value.importHistory.joinToString("\n\n") { entry ->
            val header = if (entry.memo.isBlank()) {
                dateFormat.format(Date(entry.importedAt))
            } else {
                "${dateFormat.format(Date(entry.importedAt))} ${entry.memo}"
            }
            "$header\n${entry.code}"
        }
    }

    /** 共有コードを読み込み、その盤面をそのままプレイ開始する(履歴にも記録)。 */
    fun importBoard(code: String, memo: String = "") {
        val board = BoardCode.decode(code)
        if (board == null) {
            showToast("コードを読み取れませんでした", isDanger = true)
            return
        }
        viewModelScope.launch { progressStore.addImportHistory(code, memo) }
        applyImportedBoard(board)
    }

    /** お気に入りから1件削除する。 */
    fun removeFavorite(entry: ImportHistoryEntry) {
        viewModelScope.launch { progressStore.removeImportHistory(entry.importedAt) }
        showToast("🗑 お気に入りから削除しました")
    }

    /** お気に入りから選んだ盤面を再度プレイ開始する(履歴への再記録はしない)。 */
    fun selectFavorite(entry: ImportHistoryEntry) {
        val board = BoardCode.decode(entry.code)
        if (board == null) {
            showToast("コードを読み取れませんでした", isDanger = true)
            return
        }
        applyImportedBoard(board)
    }

    private fun applyImportedBoard(board: Board) {
        val remaining = board.tiles.values.count { it is Tile.Receiver }
        _uiState.update {
            GameUiState(
                overlay = Overlay.None,
                level = it.level,
                bestLevel = it.bestLevel,
                bestScore = it.bestScore,
                clearedLevels = it.clearedLevels,
                levelBestLivesLost = it.levelBestLivesLost,
                levelBestScore = it.levelBestScore,
                importHistory = it.importHistory,
                board = board,
                remainingReceivers = remaining,
                levelSnapshot = LevelSnapshot(board, GameUiState().lives, GameUiState().score),
                hasActiveRun = true,
                isImported = true,
            )
        }
        showToast("🔗 パズルをインポートしました")
    }

    // ---- エディタ ----
    fun editorSetSize(rows: Int, cols: Int) {
        val r = rows.coerceIn(3, 20)
        val c = cols.coerceIn(3, 20)
        _uiState.update {
            val old = it.editorState.board
            val newTiles = old.tiles.filterKeys { pos -> pos.row < r && pos.col < c }
            it.copy(editorState = it.editorState.copy(board = Board(r, c, newTiles)))
        }
    }

    fun editorSelectTool(tool: EditorTool) = _uiState.update { it.copy(editorState = it.editorState.copy(tool = tool)) }
    fun editorSelectColor(color: LaserColor) = _uiState.update { it.copy(editorState = it.editorState.copy(color = color)) }
    fun editorSelectDirection(dir: Direction) = _uiState.update { it.copy(editorState = it.editorState.copy(direction = dir)) }

    fun editorTapCell(pos: CellPos) {
        val es = _uiState.value.editorState
        val newBoard = when (es.tool) {
            EditorTool.ERASER -> es.board.remove(pos)
            EditorTool.SHOOTER -> es.board.place(pos, Tile.Shooter(es.direction, es.color))
            EditorTool.RECEIVER -> es.board.place(pos, Tile.Receiver(es.color))
        }
        _uiState.update { it.copy(editorState = it.editorState.copy(board = newBoard)) }
    }

    fun editorClearAll() {
        _uiState.update {
            val b = it.editorState.board
            it.copy(editorState = it.editorState.copy(board = Board(b.rows, b.cols)))
        }
    }

    fun editorExportCode(): String = BoardCode.encode(_uiState.value.editorState.board)

    /** 自作パズルをその場でテストプレイする(記録には影響しない)。 */
    fun editorTestPlay() {
        val board = _uiState.value.editorState.board
        if (board.tiles.values.none { it is Tile.Shooter }) {
            showToast("矢印を1つ以上置いてください", isDanger = true)
            return
        }
        val remaining = board.tiles.values.count { it is Tile.Receiver }
        if (remaining <= 0) {
            showToast("光る球を1つ以上置いてください", isDanger = true)
            return
        }
        _uiState.update {
            GameUiState(
                overlay = Overlay.None,
                level = it.level,
                bestLevel = it.bestLevel,
                bestScore = it.bestScore,
                clearedLevels = it.clearedLevels,
                levelBestLivesLost = it.levelBestLivesLost,
                levelBestScore = it.levelBestScore,
                importHistory = it.importHistory,
                board = board,
                remainingReceivers = remaining,
                levelSnapshot = LevelSnapshot(board, GameUiState().lives, GameUiState().score),
                hasActiveRun = true,
                isImported = true,
                isEditorTest = true,
                editorState = it.editorState,
            )
        }
    }

    // ---- タップ操作 ----
    fun handleTap(pos: CellPos) {
        val current = _uiState.value
        if (current.busy) return
        if (current.addingLight) {
            if (current.board.tiles[pos] != null) {
                showToast("空いているマスを選んでください")
                return
            }
            _uiState.update {
                it.copy(pendingLightCell = pos, overlay = Overlay.ColorPick(pos, colorChoices(current.board)))
            }
            return
        }
        val tile = current.board.tiles[pos] ?: return
        if (tile is Tile.Receiver) {
            showToast("光る球は受信専用。矢印から発射しよう")
            return
        }
        val result = LaserSimulator.fireLaser(current.board, pos)
        val bends = findBends(result.path)
        val hitIsShooterClash = result.hit?.let { current.board.tiles[it] is Tile.Shooter } ?: false
        sfxEngine.playFire()
        _uiState.update {
            it.copy(
                busy = true,
                activeShot = ShotAnimation(pos, result, bends, hitIsShooterClash, token = System.nanoTime()),
            )
        }
    }

    private fun findBends(path: List<CellPos>): Set<CellPos> {
        if (path.size < 3) return emptySet()
        val bends = mutableSetOf<CellPos>()
        for (i in 1 until path.size - 1) {
            val d1 = path[i].row - path[i - 1].row to path[i].col - path[i - 1].col
            val d2 = path[i + 1].row - path[i].row to path[i + 1].col - path[i].col
            if (d1 != d2) bends += path[i]
        }
        return bends
    }

    /** ビーム経路のリビールアニメーションが完了した時点でCompose側から呼ばれる。 */
    fun onShotRevealFinished() {
        val shot = _uiState.value.activeShot ?: return
        if (shot.result.hit != null) {
            sfxEngine.playHitConfirm()
        }
    }

    /** バースト演出(命中時)または待機(ミス時)が完了した時点でCompose側から呼ばれる。 */
    fun onShotSettled() {
        val shot = _uiState.value.activeShot ?: return
        finishShot(shot)
    }

    private fun finishShot(shot: ShotAnimation) {
        val current = _uiState.value
        val hitKey = shot.result.hit
        if (hitKey != null) {
            val newCombo = current.combo + 1
            val gained = ScoreRules.shotScore(newCombo)
            val originTile = current.board.tiles[shot.originKey] ?: return
            val hitTile = current.board.tiles[hitKey] ?: return
            val wasReceiver = hitTile is Tile.Receiver
            val entry = HistoryEntry(
                removedAPos = shot.originKey,
                removedATile = originTile,
                removedBPos = hitKey,
                removedBTile = hitTile,
                gained = gained,
                wasReceiver = wasReceiver,
            )
            val newBoard = current.board.remove(shot.originKey).remove(hitKey)
            val newRemaining = current.remainingReceivers - (if (wasReceiver) 1 else 0)
            if (wasReceiver) {
                sfxEngine.playConnect(newCombo)
            } else {
                sfxEngine.playShooterClash()
            }
            _uiState.update {
                it.copy(
                    board = newBoard,
                    score = it.score + gained,
                    combo = newCombo,
                    history = it.history + entry,
                    remainingReceivers = newRemaining,
                    busy = false,
                    activeShot = null,
                    comboPopup = if (newCombo >= 2) ComboPopup(newCombo, token = System.nanoTime()) else null,
                )
            }
            if (newRemaining <= 0) {
                viewModelScope.launch {
                    delay(400)
                    levelClear()
                }
            }
        } else {
            val newLives = current.lives - 1
            sfxEngine.playMiss()
            _uiState.update {
                it.copy(combo = 0, lives = newLives, busy = false, activeShot = null, comboPopup = null)
            }
            showToast("✕ 最終的な色が一致しなかった", isDanger = true)
            if (newLives <= 0) {
                viewModelScope.launch {
                    delay(500)
                    gameOver()
                }
            }
        }
    }

    // ---- ツールバー ----
    fun undo() {
        val current = _uiState.value
        if (current.busy || current.history.isEmpty() || current.lives <= 0) return
        val last = current.history.last()
        val newBoard = current.board
            .place(last.removedAPos, last.removedATile)
            .place(last.removedBPos, last.removedBTile)
        val newLives = current.lives - 1
        _uiState.update {
            it.copy(
                board = newBoard,
                score = it.score - last.gained,
                combo = 0,
                lives = newLives,
                remainingReceivers = it.remainingReceivers + (if (last.wasReceiver) 1 else 0),
                history = it.history.dropLast(1),
                comboPopup = null,
            )
        }
        showToast("↺ 直前の一手を戻しました(💔1)", isDanger = true)
        if (newLives <= 0) {
            viewModelScope.launch {
                delay(700)
                gameOver()
            }
        }
    }

    fun restartLevel() {
        val current = _uiState.value
        val snapshot = current.levelSnapshot ?: return
        if (current.busy || current.lives <= 0) return
        val newLives = (current.lives - 1).coerceAtLeast(0)
        _uiState.update {
            it.copy(
                board = snapshot.board,
                lives = newLives,
                score = snapshot.score,
                combo = 0,
                history = emptyList(),
                busy = false,
                remainingReceivers = snapshot.board.tiles.values.count { t -> t is Tile.Receiver },
                comboPopup = null,
            )
        }
        showToast("🔄 この盤面の最初の状態に戻しました(💔1)", isDanger = true)
        if (newLives <= 0) {
            viewModelScope.launch {
                delay(700)
                gameOver()
            }
        }
    }

    fun toggleAddLight() {
        val current = _uiState.value
        if (current.busy || current.lives <= 0) return
        val newAdding = !current.addingLight
        _uiState.update { it.copy(addingLight = newAdding) }
        showToast(
            if (newAdding) "💡 空いているマスをタップして光源を設置・ライフを1失います" else "キャンセルしました",
        )
    }

    private fun colorChoices(board: Board): List<LaserColor> {
        val boardColors = board.tiles.values.filterIsInstance<Tile.Receiver>().map { it.color }.distinct()
        return boardColors.ifEmpty { LaserColor.entries }
    }

    fun confirmLightColor(color: LaserColor) {
        val pos = _uiState.value.pendingLightCell ?: return
        _uiState.update { it.copy(overlay = Overlay.DirectionPick(pos, color)) }
    }

    fun confirmLightDirection(dir: Direction) {
        val current = _uiState.value
        val pick = current.overlay as? Overlay.DirectionPick ?: return
        val finalBoard = current.board.place(pick.cell, Tile.Shooter(dir, pick.color))
        val result = LaserSimulator.fireLaser(finalBoard, pick.cell)
        val foundConnection = result.hit != null
        sfxEngine.playLightPlaced()
        _uiState.update {
            it.copy(
                board = finalBoard,
                lives = it.lives - 1,
                combo = 0,
                addingLight = false,
                pendingLightCell = null,
                overlay = Overlay.None,
                comboPopup = null,
            )
        }
        showToast(
            if (foundConnection) "✨ 新しい光源を設置！繋がる向きが見つかりました" else "光源を設置しました(今すぐ繋がる向きは無いようです)",
            isDanger = !foundConnection,
        )
        if (_uiState.value.lives <= 0) {
            viewModelScope.launch {
                delay(700)
                gameOver()
            }
        }
    }

    fun cancelLightPick() {
        _uiState.update { it.copy(pendingLightCell = null, addingLight = false, overlay = Overlay.None) }
    }

    // ---- クリア/ゲームオーバー ----
    private fun levelClear() {
        sfxEngine.playLevelClear()
        val current = _uiState.value
        val totalScore = current.score
        val livesLost = ((current.levelSnapshot?.lives ?: current.lives) - current.lives).coerceAtLeast(0)

        if (!current.isImported) {
            val newBest = if (current.level > current.bestLevel ||
                (current.level == current.bestLevel && totalScore > current.bestScore)
            ) {
                BestRecord(current.level, totalScore)
            } else {
                BestRecord(current.bestLevel, current.bestScore)
            }
            val alreadyCleared = current.level in current.clearedLevels
            viewModelScope.launch {
                progressStore.saveBest(newBest)
                if (!alreadyCleared) progressStore.markLevelCleared(current.level)
                progressStore.recordLevelClear(current.level, livesLost, totalScore)
            }
            _uiState.update {
                it.copy(
                    bestLevel = newBest.level,
                    bestScore = newBest.score,
                    clearedLevels = it.clearedLevels + current.level,
                )
            }
        }
        _uiState.update {
            it.copy(
                score = totalScore,
                overlay = Overlay.Clear(totalScore, livesLost),
                hasActiveRun = false,
            )
        }
    }

    private fun gameOver() {
        sfxEngine.playGameOver()
        val current = _uiState.value
        val beatBest = current.score >= current.bestScore
        if (!current.isImported && current.score > current.bestScore) {
            val newBest = BestRecord(
                level = if (current.level > current.bestLevel) current.level else current.bestLevel,
                score = current.score,
            )
            viewModelScope.launch { progressStore.saveBest(newBest) }
            _uiState.update { it.copy(bestLevel = newBest.level, bestScore = newBest.score) }
        }
        _uiState.update { it.copy(overlay = Overlay.GameOver(beatBest), hasActiveRun = false) }
    }

    private fun showToast(text: String, isDanger: Boolean = false) {
        _uiState.update { it.copy(toast = ToastMessage(text, isDanger, token = System.nanoTime())) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as LaserArrowsApplication
                GameViewModel(application.progressStore, application.sfxEngine)
            }
        }
    }
}
