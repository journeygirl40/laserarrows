package com.laserarrows.app.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.laserarrows.app.LaserArrowsApplication
import com.laserarrows.app.audio.SfxEngine
import com.laserarrows.app.data.BestRecord
import com.laserarrows.app.data.FavoriteSortKey
import com.laserarrows.app.data.ImportHistoryEntry
import com.laserarrows.app.data.ProgressStore
import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.BoardCode
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.engine.LaserSimulator
import com.laserarrows.app.engine.ParResult
import com.laserarrows.app.engine.ParSolver
import com.laserarrows.app.engine.PuzzleGenerator
import com.laserarrows.app.engine.ReplayEngine
import com.laserarrows.app.engine.ScoreRules
import com.laserarrows.app.engine.Tile
import com.laserarrows.app.ui.theme.AnimalCatalog
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
            val restored = progressStore.loadActiveRun()?.let { deserializeActiveRun(it) }
            if (restored != null) {
                _uiState.update { current ->
                    restored.copy(
                        bestLevel = current.bestLevel,
                        bestScore = current.bestScore,
                        bestLevelReached = current.bestLevelReached,
                        clearedLevels = current.clearedLevels,
                        levelBestLivesLost = current.levelBestLivesLost,
                        levelBestScore = current.levelBestScore,
                        levelStars = current.levelStars,
                        forestPopulation = current.forestPopulation,
                        forestStageSeen = current.forestStageSeen,
                        forestExpansionSeen = current.forestExpansionSeen,
                        eggIntroSeen = current.eggIntroSeen,
                        scoreHelpSeen = current.scoreHelpSeen,
                        importHistory = current.importHistory,
                    )
                }
                refreshPar()
            } else {
                // クリア画面のまま閉じていた場合は、クリア画面で「ホームへ」を押した時と同じく、
                // スコア・ライフを引き継いだ次のレベルを用意して、ホームの「続きから」で再開できるようにする。
                progressStore.loadClearedRun()?.let { decodeClearedRun(it) }?.let { cleared ->
                    // 起動直後はまだ記録の読み込みが終わっていないことがあるので、次のレベルの用意に使う記録は先に読んでおく
                    // (到達レベル記録の更新や、たまご・なかまの増え方の説明を、誤って出さないように)。
                    val reached = progressStore.bestLevelReached.first()
                    val eggSeen = progressStore.eggIntroSeen.first()
                    val helpSeen = progressStore.scoreHelpSeen.first()
                    _uiState.update {
                        cleared.applyTo(it).copy(bestLevelReached = reached, eggIntroSeen = eggSeen, scoreHelpSeen = helpSeen)
                    }
                    startLevel(targetOverlay = Overlay.Home)
                }
            }
            // プレイ中の盤面・ライフ・スコア等を都度保存し、アプリのプロセスが落ちても「再開する」で復元できるようにする。
            // 復元処理より前に監視を始めると、復元前の初期状態を保存して既存のデータを消してしまうため、必ず復元後に開始する。
            // テストプレイ中は本来のラン(savedRunに退避済み)を上書きしたくないので、その間は保存自体をスキップする。
            launch {
                uiState.filter { !it.isEditorTest }.map { serializeActiveRun(it) }.distinctUntilChanged().collect { serialized ->
                    progressStore.saveActiveRun(serialized)
                }
            }
        }
        viewModelScope.launch {
            progressStore.best.collect { best ->
                _uiState.update { it.copy(bestLevel = best.level, bestScore = best.score) }
            }
        }
        viewModelScope.launch {
            progressStore.bestLevelReached.collect { level ->
                _uiState.update { it.copy(bestLevelReached = level) }
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
            progressStore.levelStars.collect { stars ->
                _uiState.update { it.copy(levelStars = stars) }
            }
        }
        viewModelScope.launch {
            progressStore.eggIntroSeen.collect { seen ->
                _uiState.update { it.copy(eggIntroSeen = seen) }
            }
        }
        viewModelScope.launch {
            progressStore.scoreHelpSeen.collect { seen ->
                _uiState.update { it.copy(scoreHelpSeen = seen) }
            }
        }
        viewModelScope.launch {
            progressStore.levelStars.combine(progressStore.forestStageSeen) { stars, seen -> stars.values.sum() to seen }.collect { (totalStars, seen) ->
                val current = ForestStage.of(totalStars).level
                // まだ記録が無い人(この機能が入る前・にぎわいを★で数えるようになる前から遊んでいた人)は、
                // 今の段階を「見届け済み」から始める(起動直後にいきなり祝ったり、段階が下がったように見せたりしない)。
                if (seen < 0) progressStore.saveForestStageSeen(current)
                _uiState.update { it.copy(forestStageSeen = if (seen < 0) current else seen) }
            }
        }
        viewModelScope.launch {
            progressStore.forestExpansionSeen.collect { seen ->
                _uiState.update { it.copy(forestExpansionSeen = seen) }
            }
        }
        viewModelScope.launch {
            progressStore.forestPopulation.collect { population ->
                _uiState.update { it.copy(forestPopulation = population) }
            }
        }
        viewModelScope.launch {
            progressStore.importHistory.collect { history ->
                _uiState.update { it.copy(importHistory = history) }
            }
        }
        viewModelScope.launch {
            progressStore.levelListSortDescending.collect { descending ->
                _uiState.update { it.copy(levelListSortDescending = descending) }
            }
        }
        viewModelScope.launch {
            progressStore.favoriteSortKey.collect { key ->
                _uiState.update { it.copy(favoriteSortKey = key) }
            }
        }
        viewModelScope.launch {
            progressStore.favoriteSortDescending.collect { descending ->
                _uiState.update { it.copy(favoriteSortDescending = descending) }
            }
        }
    }

    fun setFavoriteSortKey(key: FavoriteSortKey) {
        _uiState.update { it.copy(favoriteSortKey = key) }
        viewModelScope.launch { progressStore.saveFavoriteSort(key, _uiState.value.favoriteSortDescending) }
    }

    fun setFavoriteSortDescending(descending: Boolean) {
        _uiState.update { it.copy(favoriteSortDescending = descending) }
        viewModelScope.launch { progressStore.saveFavoriteSort(_uiState.value.favoriteSortKey, descending) }
    }

    fun setLevelListSortDescending(descending: Boolean) {
        _uiState.update { it.copy(levelListSortDescending = descending) }
        viewModelScope.launch { progressStore.saveLevelListSortDescending(descending) }
    }

    /** プレイ画面の「?」から「なかまの増え方」の説明を開く。 */
    fun openScoreHelp() = _uiState.update { it.copy(showScoreHelp = true) }

    /** 「なかまの増え方」の説明を閉じる(以後は自動では出さない)。 */
    fun dismissScoreHelp() {
        _uiState.update { it.copy(showScoreHelp = false, scoreHelpSeen = true) }
        viewModelScope.launch { progressStore.markScoreHelpSeen() }
    }

    /** 「なぞのたまご」の説明を閉じる(以後は出さない)。 */
    fun dismissEggIntro() {
        _uiState.update { it.copy(showEggIntro = false, eggIntroSeen = true) }
        viewModelScope.launch { progressStore.markEggIntroSeen() }
    }

    // ---- 次のレベルの先読み ----
    // 遊んでいる間に次のレベルの盤面を裏で作っておき、「次のレベルへ」を押した時の待ち時間をなくす。
    // 盤面は毎回新しく作るので、先読みした盤面は一度使ったら捨てる(同じ盤面には二度と出会わない)。

    /** 作成中/作成済みの盤面1つ分。progress=作成の進み具合(待つことになった時の読み込みバー用)。 */
    private class BoardPrefetch(val board: Deferred<Board>, val progress: MutableStateFlow<Float>)

    private val boardPrefetches = mutableMapOf<Int, BoardPrefetch>()

    /** クリア時点で先に計算しておく、次のレベルのパー。パーは開始時のコンボで変わるので、コンボも合わせて覚えておく。 */
    private class ParPrefetch(val level: Int, val combo: Int, val par: Deferred<ParResult>)

    private var parPrefetch: ParPrefetch? = null

    /** そのレベルの盤面の作成を(まだなら)裏で始める。先読みは直近の数レベル分だけ残す。 */
    private fun prefetchBoard(level: Int): BoardPrefetch {
        boardPrefetches[level]?.let { return it }
        val progress = MutableStateFlow(0f)
        val board = viewModelScope.async(Dispatchers.Default) {
            PuzzleGenerator.generateForLevel(level) { progress.value = it }.board
        }
        val prefetch = BoardPrefetch(board, progress)
        boardPrefetches[level] = prefetch
        boardPrefetches.keys.filter { it < level - 1 || it > level + 1 }.forEach { boardPrefetches.remove(it)?.board?.cancel() }
        return prefetch
    }

    /** クリアした時に、次のレベルのパーを(次の盤面ができ次第)裏で計算し始める。 */
    private fun prefetchNextPar(nextLevel: Int, combo: Int) {
        val next = prefetchBoard(nextLevel)
        parPrefetch?.par?.cancel()
        parPrefetch = ParPrefetch(
            nextLevel,
            combo,
            viewModelScope.async(Dispatchers.Default) { ParSolver.solve(next.board.await(), combo) },
        )
    }

    /** 今の盤面のパーを、レベル開始時点の盤面とコンボから計算し直す(インポート・再開・テストプレイ等、生成を経ない盤面用)。 */
    private fun refreshPar(precomputed: Deferred<ParResult>? = null) {
        val snapshot = _uiState.value.levelSnapshot ?: return
        // 先に計算済みなら、「計算中」を挟まずにそのまま出す(終わっているDeferredのawaitは待たずに返る)。
        if (precomputed?.isCompleted != true) _uiState.update { it.copy(par = null) }
        viewModelScope.launch {
            val par = precomputed?.let { runCatching { it.await() }.getOrNull() }
                ?: withContext(Dispatchers.Default) { ParSolver.solve(snapshot.board, snapshot.combo) }
            // 計算中に別の盤面へ移っていたら、古い盤面のパーで上書きしない。
            _uiState.update { if (it.levelSnapshot == snapshot) it.copy(par = par) else it }
        }
    }

    private fun maxLevelReached(): Int = _uiState.value.clearedLevels.maxOrNull() ?: 0

    // ---- 画面遷移 ----
    fun goStart() = newRun(1)
    fun goContinue() = newRun(maxLevelReached() + 1)
    fun goList(excludePerfect: Boolean = false) = _uiState.update {
        it.copy(overlay = Overlay.LevelList, levelListExcludePerfect = excludePerfect)
    }
    fun listBack() = _uiState.update { it.copy(overlay = Overlay.Home) }

    fun goImport() = _uiState.update { it.copy(overlay = Overlay.Import) }
    fun importBack() = _uiState.update { it.copy(overlay = Overlay.Favorites) }
    fun goFavorites() = _uiState.update { it.copy(overlay = Overlay.Favorites) }
    fun favoritesBack() = _uiState.update { it.copy(overlay = Overlay.Home) }
    fun goEditor() = _uiState.update { it.copy(overlay = Overlay.Editor) }
    fun editorBack() = _uiState.update { it.copy(overlay = Overlay.Home) }

    /** どうぶつの設定を開く。Lv.20に到達するまでは鍵がかかっていて開けない。 */
    fun goAnimals() {
        if (_uiState.value.bestLevelReached >= AnimalCatalog.SETTINGS_UNLOCK_LEVEL) {
            _uiState.update { it.copy(overlay = Overlay.Animals) }
        } else {
            showToast("🔒 Lv.${AnimalCatalog.SETTINGS_UNLOCK_LEVEL}に到達すると解放されます")
        }
    }
    fun animalsBack() = _uiState.update { it.copy(overlay = Overlay.Home) }
    fun gameOverHome() = _uiState.update {
        when {
            it.isEditorTest -> restoreFromEditorTest(it).copy(overlay = Overlay.Editor)
            it.isImported -> it.copy(overlay = Overlay.Favorites)
            else -> it.copy(overlay = Overlay.Home)
        }
    }

    /** レベルクリア画面から「ホームに戻る」。次のレベルを先読み生成してアクティブランにし、スコア・ライフを引き継いだまま「再開する」で戻れるようにする。 */
    fun clearHome() {
        val current = _uiState.value
        when {
            current.isEditorTest -> _uiState.update { restoreFromEditorTest(it).copy(overlay = Overlay.Editor) }
            current.isImported -> _uiState.update { it.copy(overlay = Overlay.Favorites) }
            else -> {
                _uiState.update { it.copy(level = current.level + 1) }
                startLevel(targetOverlay = Overlay.Home)
            }
        }
    }

    /** プレイ中にいつでもホームへ(自作パズルのテストプレイ中はエディタへ、その際は退避しておいた本来のランを復元する)。盤面・スコア・ライフ等はそのまま保持し、後で再開できる。 */
    fun goHome() {
        val current = _uiState.value
        if (current.busy) return
        _uiState.update {
            val wasEditorTest = it.isEditorTest
            val restored = if (wasEditorTest) restoreFromEditorTest(it) else it
            restored.copy(
                overlay = if (wasEditorTest) Overlay.Editor else Overlay.Home,
                addingLight = false,
                pendingLightCell = null,
            )
        }
    }

    /** テストプレイを抜ける際、退避しておいた本来のラン([SavedRun])があればそれに戻す。無ければ(テスト前にランが無かった場合)アクティブランなしの状態にする。 */
    private fun restoreFromEditorTest(state: GameUiState): GameUiState {
        val saved = state.savedRun
        return if (saved != null) {
            state.copy(
                level = saved.level,
                score = saved.score,
                combo = saved.combo,
                lives = saved.lives,
                board = saved.board,
                remainingReceivers = saved.remainingReceivers,
                history = saved.history,
                levelSnapshot = saved.levelSnapshot,
                isImported = saved.isImported,
                activeFavoriteImportedAt = saved.activeFavoriteImportedAt,
                bestScoreAtRunStart = saved.bestScoreAtRunStart,
                crossedHighScoreThisRun = saved.crossedHighScoreThisRun,
                crossedLevelRecordThisRun = saved.crossedLevelRecordThisRun,
                par = saved.par,
                hasActiveRun = true,
                isEditorTest = false,
                savedRun = null,
            )
        } else {
            state.copy(hasActiveRun = false, isEditorTest = false, savedRun = null)
        }
    }

    /** ホームから、保持しておいたプレイ中の状態にそのまま戻る。 */
    fun resume() {
        if (!_uiState.value.hasActiveRun) return
        // ゲームオーバー中のランに戻った時は、ゲームオーバー画面から(広告を見て復活しないと遊べない)。
        _uiState.update {
            val overlay = if (it.lives <= 0) Overlay.GameOver(beatBest = it.score >= it.bestScoreAtRunStart) else Overlay.None
            it.copy(overlay = overlay)
        }
        // テストプレイ等で計算が中断されたままのランに戻った場合は、パーを計算し直す。
        if (_uiState.value.par == null) refreshPar()
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
    fun consumeScoreBonusPopup() = _uiState.update { it.copy(scoreBonusPopup = null) }
    fun consumeHighScorePopup() = _uiState.update { it.copy(highScorePopup = null) }
    fun consumeLevelRecordPopup() = _uiState.update { it.copy(levelRecordPopup = null) }

    private fun newRun(startLevel: Int) {
        _uiState.update {
            GameUiState(
                overlay = Overlay.None,
                level = startLevel,
                bestLevel = it.bestLevel,
                bestScore = it.bestScore,
                bestScoreAtRunStart = it.bestScore,
                bestLevelReached = it.bestLevelReached,
                clearedLevels = it.clearedLevels,
                levelBestLivesLost = it.levelBestLivesLost,
                levelBestScore = it.levelBestScore,
                levelStars = it.levelStars,
                forestPopulation = it.forestPopulation,
                forestStageSeen = it.forestStageSeen,
                forestExpansionSeen = it.forestExpansionSeen,
                eggIntroSeen = it.eggIntroSeen,
                scoreHelpSeen = it.scoreHelpSeen,
                importHistory = it.importHistory,
            )
        }
        startLevel()
    }

    private fun startLevel(targetOverlay: Overlay = Overlay.None) {
        viewModelScope.launch {
            val level = _uiState.value.level
            // 先読み済みならすぐに盤面を出す。作成中なら、その続きを待つ(待つ間だけ読み込みバーを進める)。
            val prefetch = prefetchBoard(level)
            boardPrefetches.remove(level)
            val board = if (prefetch.board.isCompleted) {
                prefetch.board.await()
            } else {
                val watcher = launch { prefetch.progress.collect { p -> _uiState.update { it.copy(loadingProgress = p) } } }
                try {
                    prefetch.board.await()
                } finally {
                    watcher.cancel()
                }
            }
            val remaining = board.tiles.values.count { it is Tile.Receiver }
            // 過去の最深到達レベルを超えた瞬間は「まだ誰も(自分すら)見たことのない領域」として一度だけ大きく祝う。
            val enteringNewLevelRecord = level > _uiState.value.bestLevelReached
            _uiState.update {
                it.copy(
                    board = board,
                    remainingReceivers = remaining,
                    busy = false,
                    history = emptyList(),
                    levelSnapshot = LevelSnapshot(board, it.lives, it.score, it.combo),
                    loadingProgress = null,
                    // なぞのたまごが初めて盤面に出た時だけ、遊び方の説明を出す。
                    showEggIntro = !it.eggIntroSeen && board.tiles.values.any { tile -> tile is Tile.Shooter && tile.egg },
                    // なかまの増え方は、最初のプレイで一度だけ説明する(たまごの説明と重なる時は、たまごを先に)。
                    showScoreHelp = it.showScoreHelp || it.scoreHelpSeen == false,
                    overlay = targetOverlay,
                    clearFinale = null,
                    hasActiveRun = true,
                    isImported = false,
                    isEditorTest = false,
                    crossedLevelRecordThisRun = it.crossedLevelRecordThisRun || enteringNewLevelRecord,
                    levelRecordPopup = if (enteringNewLevelRecord) {
                        LevelRecordPopup(level, token = System.nanoTime())
                    } else {
                        it.levelRecordPopup
                    },
                )
            }
            // 次のレベル(または新しいラン)が始まったので、クリア画面のまま閉じた時用の記録はもう要らない。
            progressStore.saveClearedRun(null)
            // 大きな盤面ではパーの探索に時間がかかるため、盤面は先に見せてパーは裏で計算する(HUDは計算中表示)。
            // クリア時に先に計算しておいたパーが、この盤面と開始コンボに合っていればそれを使う。
            val startCombo = _uiState.value.combo
            val precomputed = parPrefetch?.takeIf { it.level == level && it.combo == startCombo }?.par
            parPrefetch = null
            refreshPar(precomputed)
            // 遊んでいる間に、次のレベルの盤面を裏で作っておく。
            prefetchBoard(level + 1)
            if (enteringNewLevelRecord) {
                progressStore.recordLevelReached(level)
            }
        }
    }

    /** この盤面の初期状態(プレイ開始時点のスナップショット)を共有コードへ変換する(純粋関数、UIで共有シートを呼び出す)。 */
    /** 今プレイしていた盤面の初期状態を、コードの貼り付けなしでそのままお気に入りに登録する。
     * クリア画面から登録した場合は、その時のクリア実績(消費ライフ・一手ごとの再生ログ)もあわせて記録し、
     * お気に入り一覧からすぐ振り返れるようにする。 */
    fun addCurrentBoardToFavorites(memo: String = "") {
        val current = _uiState.value
        val board = current.levelSnapshot?.board ?: current.board
        val code = BoardCode.encode(board)
        // 実レベルの盤面ならcurrent.levelがそのまま由来レベル。お気に入り・自作のテストの盤面(isImported)はレベル不明。
        val level = current.level.takeIf { !current.isImported }
        val clearedOverlay = current.overlay as? Overlay.Clear
        // プレイ中にも登録できるので、同じ盤面がもう入っていれば新しく増やさず、その項目にメモ・クリア記録を書き足す。
        val existing = current.importHistory.firstOrNull { it.code == code }
        if (existing != null) {
            if (clearedOverlay == null && memo.isBlank()) {
                showToast("⭐ もうお気に入りに入っています")
                return
            }
            viewModelScope.launch {
                if (memo.isNotBlank()) progressStore.updateImportHistoryMemo(existing.importedAt, memo)
                if (clearedOverlay != null) {
                    progressStore.recordFavoriteClear(existing.importedAt, clearedOverlay.livesLost, encodeReplayLog(current.history))
                }
            }
            showToast("⭐ お気に入りを更新しました")
            return
        }
        val importedAt = System.currentTimeMillis()
        viewModelScope.launch {
            progressStore.addImportHistory(code, memo, level, importedAt)
            if (clearedOverlay != null) {
                progressStore.recordFavoriteClear(importedAt, clearedOverlay.livesLost, encodeReplayLog(current.history))
            }
        }
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
        val importedAt = System.currentTimeMillis()
        viewModelScope.launch { progressStore.addImportHistory(code, memo, importedAt = importedAt) }
        applyImportedBoard(board, level = null, importedAt = importedAt)
    }

    /** お気に入りから1件削除する。 */
    fun removeFavorite(entry: ImportHistoryEntry) {
        viewModelScope.launch { progressStore.removeImportHistory(entry.importedAt) }
        showToast("🗑 お気に入りから削除しました")
    }

    /** お気に入りのメモ欄を書き換える。 */
    fun updateFavoriteMemo(entry: ImportHistoryEntry, memo: String) {
        viewModelScope.launch { progressStore.updateImportHistoryMemo(entry.importedAt, memo) }
    }

    /** お気に入りから選んだ盤面を再度プレイ開始する(履歴への再記録はしない)。 */
    fun selectFavorite(entry: ImportHistoryEntry) {
        val board = BoardCode.decode(entry.code)
        if (board == null) {
            showToast("コードを読み取れませんでした", isDanger = true)
            return
        }
        applyImportedBoard(board, level = entry.level, importedAt = entry.importedAt)
    }

    /** お気に入りのクリア記録を、一手ごとに振り返るログ画面を開く。 */
    fun openReplay(entry: ImportHistoryEntry) {
        val log = entry.replayLog ?: return
        val board = BoardCode.decode(entry.code) ?: return
        val steps = ReplayEngine.decodeSteps(log)
        if (steps.isEmpty()) return
        _uiState.update { it.copy(overlay = Overlay.Replay(entry, board, steps, 0)) }
    }

    /** 振り返りログを1手分だけ進める(delta=1)/戻す(delta=-1)。 */
    fun replayStep(delta: Int) {
        val replay = _uiState.value.overlay as? Overlay.Replay ?: return
        val newIndex = (replay.stepIndex + delta).coerceIn(0, replay.steps.size)
        _uiState.update { it.copy(overlay = replay.copy(stepIndex = newIndex)) }
    }

    fun closeReplay() = _uiState.update { it.copy(overlay = Overlay.Favorites) }

    private fun applyImportedBoard(board: Board, level: Int?, importedAt: Long?) {
        val remaining = board.tiles.values.count { it is Tile.Receiver }
        // 由来レベルが分からない盤面(生コードの読み込み等)は、ペア数からおおよその相当レベルを逆算して見せる。
        val displayLevel = level ?: PuzzleGenerator.estimateLevelForBoard(board)
        _uiState.update {
            GameUiState(
                overlay = Overlay.None,
                level = displayLevel,
                bestLevel = it.bestLevel,
                bestScore = it.bestScore,
                bestScoreAtRunStart = it.bestScore,
                bestLevelReached = it.bestLevelReached,
                clearedLevels = it.clearedLevels,
                levelBestLivesLost = it.levelBestLivesLost,
                levelBestScore = it.levelBestScore,
                levelStars = it.levelStars,
                forestPopulation = it.forestPopulation,
                forestStageSeen = it.forestStageSeen,
                forestExpansionSeen = it.forestExpansionSeen,
                eggIntroSeen = it.eggIntroSeen,
                scoreHelpSeen = it.scoreHelpSeen,
                importHistory = it.importHistory,
                board = board,
                remainingReceivers = remaining,
                levelSnapshot = LevelSnapshot(board, GameUiState().lives, GameUiState().score, GameUiState().combo),
                hasActiveRun = true,
                isImported = true,
                activeFavoriteImportedAt = importedAt,
            )
        }
        refreshPar()
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
            EditorTool.EGG -> es.board.place(pos, Tile.egg(es.direction))
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
        val displayLevel = PuzzleGenerator.estimateLevelForBoard(board)
        _uiState.update {
            // テスト前に本来のランが進行中だった場合は、テストで上書きされないよう退避しておく。
            val savedRun = if (it.hasActiveRun && !it.isEditorTest) {
                SavedRun(
                    level = it.level,
                    score = it.score,
                    combo = it.combo,
                    lives = it.lives,
                    board = it.board,
                    remainingReceivers = it.remainingReceivers,
                    history = it.history,
                    levelSnapshot = it.levelSnapshot,
                    isImported = it.isImported,
                    activeFavoriteImportedAt = it.activeFavoriteImportedAt,
                    bestScoreAtRunStart = it.bestScoreAtRunStart,
                    crossedHighScoreThisRun = it.crossedHighScoreThisRun,
                    crossedLevelRecordThisRun = it.crossedLevelRecordThisRun,
                    par = it.par,
                )
            } else {
                it.savedRun
            }
            GameUiState(
                overlay = Overlay.None,
                level = displayLevel,
                bestLevel = it.bestLevel,
                bestScore = it.bestScore,
                bestScoreAtRunStart = it.bestScore,
                bestLevelReached = it.bestLevelReached,
                clearedLevels = it.clearedLevels,
                levelBestLivesLost = it.levelBestLivesLost,
                levelBestScore = it.levelBestScore,
                levelStars = it.levelStars,
                forestPopulation = it.forestPopulation,
                forestStageSeen = it.forestStageSeen,
                forestExpansionSeen = it.forestExpansionSeen,
                eggIntroSeen = it.eggIntroSeen,
                scoreHelpSeen = it.scoreHelpSeen,
                importHistory = it.importHistory,
                board = board,
                remainingReceivers = remaining,
                levelSnapshot = LevelSnapshot(board, GameUiState().lives, GameUiState().score, GameUiState().combo),
                hasActiveRun = true,
                isImported = true,
                isEditorTest = true,
                savedRun = savedRun,
                editorState = it.editorState,
            )
        }
        refreshPar()
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
            showToast("💗の子は相手を待っています。矢印のついた子をタップしよう")
            return
        }
        val result = LaserSimulator.fireLaser(current.board, pos)
        val bends = ReplayEngine.findBends(result.path)
        val hitIsShooterClash = result.hit?.let { current.board.tiles[it] is Tile.Shooter } ?: false
        sfxEngine.playFire()
        _uiState.update {
            it.copy(
                busy = true,
                activeShot = ShotAnimation(pos, result, bends, hitIsShooterClash, token = System.nanoTime()),
            )
        }
    }

    /** Undo履歴を、お気に入りの一手ごとの振り返り再生用にエンコードする(発射/光源設置の両方を含む)。 */
    private fun encodeReplayLog(history: List<HistoryEntry>): String {
        val steps = history.map { entry ->
            when (entry) {
                is HistoryEntry.Shot -> ReplayEngine.Step.Fire(entry.removedAPos)
                is HistoryEntry.LightAdded -> ReplayEngine.Step.AddLight(entry.cell, entry.tile.color, entry.tile.dir)
            }
        }
        return ReplayEngine.encodeSteps(steps)
    }

    /** ビーム経路のリビールアニメーションが完了した時点でCompose側から呼ばれる。
     * 相手の子に届いた時は、出会い演出で二人がくっつく瞬間に合わせて到達音を鳴らす。 */
    fun onShotRevealFinished() {
        val current = _uiState.value
        val shot = current.activeShot ?: return
        val hitKey = shot.result.hit ?: return
        if (current.board.tiles[hitKey] is Tile.Receiver) {
            sfxEngine.playConnect(current.combo + 1, delayMs = MEET_BUMP_DELAY_MS)
        } else {
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
            val originTile = current.board.tiles[shot.originKey] ?: return
            val hitTile = current.board.tiles[hitKey] ?: return
            val wasReceiver = hitTile is Tile.Receiver
            // 矢印(Shooter)同士が同色でぶつかった場合は、見た目上はタイルが消えても「狙った接続」ではないため失敗扱いにする(コンボは繋げない・得点も与えない)。
            val newCombo = if (wasReceiver) current.combo + 1 else 0
            val relays = ScoreRules.relayCount(shot.result)
            val gained = if (wasReceiver) ScoreRules.shotScore(newCombo, relays) else 0
            val entry = HistoryEntry.Shot(
                removedAPos = shot.originKey,
                removedATile = originTile,
                removedBPos = hitKey,
                removedBTile = hitTile,
                gained = gained,
                wasReceiver = wasReceiver,
                previousCombo = current.combo,
            )
            val newBoard = current.board.remove(shot.originKey).remove(hitKey)
            val newRemaining = current.remainingReceivers - (if (wasReceiver) 1 else 0)
            val newScore = current.score + gained
            // 自己ベストが0(初回プレイ等)の場合は「更新」演出に値しないので鳴らさない。既にこのランで一度鳴らした後は再度は鳴らさない。
            val justBeatHighScore = gained > 0 && !current.crossedHighScoreThisRun && current.bestScoreAtRunStart > 0 &&
                current.score <= current.bestScoreAtRunStart && newScore > current.bestScoreAtRunStart
            if (wasReceiver) {
                if (justBeatHighScore) {
                    sfxEngine.playHighScore()
                }
            } else {
                sfxEngine.playShooterClash()
            }
            _uiState.update {
                it.copy(
                    board = newBoard,
                    score = newScore,
                    combo = newCombo,
                    history = it.history + entry,
                    remainingReceivers = newRemaining,
                    busy = false,
                    activeShot = null,
                    comboPopup = if (wasReceiver && newCombo >= 2) ComboPopup(newCombo, token = System.nanoTime()) else null,
                    scoreBonusPopup = if (wasReceiver && gained > 0) ScoreBonusPopup(gained, relays, at = hitKey, color = hitTile.color, token = System.nanoTime()) else null,
                    crossedHighScoreThisRun = it.crossedHighScoreThisRun || justBeatHighScore,
                    highScorePopup = if (justBeatHighScore) {
                        HighScorePopup(newScore, token = System.nanoTime())
                    } else {
                        it.highScorePopup
                    },
                )
            }
            if (!wasReceiver) {
                showToast("💥 同じ種類の走る子どうしがぶつかった(コンボが途切れました)", isDanger = true)
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
            showToast("✕ 相手に出会えなかった", isDanger = true)
            if (newLives <= 0) {
                viewModelScope.launch {
                    delay(500)
                    gameOver()
                }
            }
        }
    }

    // ---- タイトルの森 ----
    /** ホームで森の段階が上がったのを見せた時に呼ぶ。同じ段階で二度祝わないよう記録し、祝いの音を鳴らす。 */
    fun onForestStageCelebrated(level: Int) {
        sfxEngine.playForestLevelUp()
        _uiState.update { it.copy(forestStageSeen = level) }
        viewModelScope.launch { progressStore.saveForestStageSeen(level) }
    }

    /** ホームで森の広さの段階が上がったのを見せた時に呼ぶ。 */
    fun onForestExpansionCelebrated(level: Int) {
        sfxEngine.playForestLevelUp()
        _uiState.update { it.copy(forestExpansionSeen = level) }
        viewModelScope.launch { progressStore.saveForestExpansionSeen(level) }
    }

    // ---- クリア画面のパレード ----
    fun onParadePop(index: Int) = sfxEngine.playParadePop(index)

    /** 全員が並び終えた瞬間。★3の時だけ締めのベルで一番盛り上げる。 */
    fun onParadeAllMet(stars: Int) {
        if (stars >= 3) sfxEngine.playParadeFinale()
    }

    // ---- ツールバー ----
    fun undo() {
        val current = _uiState.value
        if (current.busy || current.history.isEmpty() || current.lives <= 0) return
        val newLives = current.lives - 1
        when (val last = current.history.last()) {
            is HistoryEntry.Shot -> {
                val newBoard = current.board
                    .place(last.removedAPos, last.removedATile)
                    .place(last.removedBPos, last.removedBTile)
                _uiState.update {
                    it.copy(
                        board = newBoard,
                        score = it.score - last.gained,
                        combo = last.previousCombo,
                        lives = newLives,
                        remainingReceivers = it.remainingReceivers + (if (last.wasReceiver) 1 else 0),
                        history = it.history.dropLast(1),
                        comboPopup = null,
                    )
                }
                showToast("↺ 直前の一手を戻しました(💔1)", isDanger = true)
            }
            is HistoryEntry.LightAdded -> {
                val newBoard = current.board.remove(last.cell)
                _uiState.update {
                    it.copy(
                        board = newBoard,
                        combo = last.previousCombo,
                        lives = newLives,
                        history = it.history.dropLast(1),
                        comboPopup = null,
                    )
                }
                showToast("↺ 呼んだなかまを帰しました(💔1)", isDanger = true)
            }
        }
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
                combo = snapshot.combo,
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

    /**
     * ヒント: ハートを1つ使って、今の盤面・コンボから最高点になる「次の1手」を教える(タップする子を光らせ、走る道筋を見せる)。
     * 戻す・やり直すと同じく最後のハートでも使える。0になったらゲームオーバーになるが、広告で復活すれば
     * 盤面はそのままなので、ヒントも表示されたまま続けられる。
     * もう全員は出会えない盤面だった時は、ハートを使わずに「やり直す」をすすめる。
     */
    fun useHint() {
        val current = _uiState.value
        if (current.busy || current.hintLoading || current.shownHint != null || current.overlay != Overlay.None || current.lives <= 0) return
        val board = current.board
        val combo = current.combo
        _uiState.update { it.copy(hintLoading = true, addingLight = false, pendingLightCell = null) }
        viewModelScope.launch {
            val move = withContext(Dispatchers.Default) { ParSolver.bestMove(board, combo) }
            // 考えている間に盤面が変わっていたら、そのヒントは使わない(ハートも使わない)。
            if (_uiState.value.board != board) {
                _uiState.update { it.copy(hintLoading = false) }
                return@launch
            }
            when {
                move == null -> {
                    _uiState.update { it.copy(hintLoading = false) }
                    showToast("今は出会える子がいません。「呼ぶ」か「やり直す」を試してね(ハートは使っていません)")
                }
                !move.clearable -> {
                    _uiState.update { it.copy(hintLoading = false) }
                    showToast("この盤面はもう全員は出会えません。「やり直す」がおすすめ(ハートは使っていません)")
                }
                else -> {
                    val result = LaserSimulator.fireLaser(board, move.origin)
                    val newLives = _uiState.value.lives - 1
                    _uiState.update {
                        it.copy(
                            hintLoading = false,
                            lives = newLives,
                            hint = HintMove(board, move.origin, result, move.gained, token = System.nanoTime()),
                        )
                    }
                    sfxEngine.playLightPlaced()
                    showToast(
                        if (move.gained > 0) "💡 光っている子をタップ！ なかま+${move.gained}匹(💔1)" else "💡 光っている子をタップ！ 道をあけよう(💔1)",
                    )
                    if (newLives <= 0) {
                        delay(700)
                        gameOver()
                    }
                }
            }
        }
    }

    fun toggleAddLight() {
        val current = _uiState.value
        if (current.busy || current.lives <= 0) return
        val newAdding = !current.addingLight
        _uiState.update { it.copy(addingLight = newAdding) }
        showToast(
            if (newAdding) "➕ 空いているマスをタップしてなかまを呼ぶ・ライフを1失います" else "キャンセルしました",
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
        val newTile = Tile.Shooter(dir, pick.color)
        val finalBoard = current.board.place(pick.cell, newTile)
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
                history = it.history + HistoryEntry.LightAdded(pick.cell, newTile, it.combo),
            )
        }
        showToast(
            if (foundConnection) "✨ なかまが来た！相手に出会える向きです" else "なかまが来ました(今すぐ出会える向きではないようです)",
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
        val snapshot = current.levelSnapshot
        // パーの計算がまだ終わっていない(ごく短時間でクリアした)場合は、探索量を絞ってその場で求める。
        val par = current.par?.par
            ?: snapshot?.let { ParSolver.solve(it.board, it.combo, nodeBudget = 5_000).par }
            ?: 0
        val levelScore = current.levelScore
        val stars = ScoreRules.starsFor(levelScore, par)
        // 「次のレベルへ」で使う盤面とパーを、クリア画面を見ている間に用意しておく(お気に入り・自作のテストには「次」が無い)。
        if (!current.isImported) prefetchNextPar(current.level + 1, current.combo)

        if (!current.isImported) {
            val newBest = if (totalScore > current.bestScore) {
                BestRecord(current.level, totalScore)
            } else {
                BestRecord(current.bestLevel, current.bestScore)
            }
            val alreadyCleared = current.level in current.clearedLevels
            val newLevelStars = current.levelStars + (current.level to maxOf(stars, current.levelStars[current.level] ?: 0))
            viewModelScope.launch {
                progressStore.saveBest(newBest)
                if (!alreadyCleared) progressStore.markLevelCleared(current.level)
                progressStore.recordLevelClear(current.level, livesLost, totalScore)
                progressStore.recordLevelStars(current.level, stars)
                // 戻す(Undo)での稼ぎ直しを数えないよう、森のなかまはクリア確定時にまとめて加える。
                progressStore.addForestPopulation(levelScore)
            }
            _uiState.update {
                it.copy(
                    bestLevel = newBest.level,
                    bestScore = newBest.score,
                    clearedLevels = it.clearedLevels + current.level,
                    levelStars = newLevelStars,
                )
            }
        } else {
            val favoriteImportedAt = current.activeFavoriteImportedAt
            if (favoriteImportedAt != null) {
                val replayLog = encodeReplayLog(current.history)
                viewModelScope.launch { progressStore.recordFavoriteClear(favoriteImportedAt, livesLost, replayLog) }
            }
        }
        val meetings = current.history
            .filterIsInstance<HistoryEntry.Shot>()
            .filter { it.wasReceiver }
        val finaleBursts = meetings.map { FinaleBurst(it.removedBPos, it.removedBTile.color) }
        val couples = meetings.map {
            ClearCouple(it.removedBTile.color, babyRunner = (it.removedATile as? Tile.Shooter)?.egg == true, gained = it.gained)
        }
        _uiState.update {
            it.copy(
                score = totalScore,
                overlay = Overlay.Clear(
                    totalScore,
                    livesLost,
                    levelScore,
                    par,
                    stars,
                    couples,
                    populationGain = if (current.isImported) 0 else levelScore,
                ),
                hasActiveRun = false,
                clearFinale = if (finaleBursts.isNotEmpty()) ClearFinale(finaleBursts, token = System.nanoTime()) else null,
            )
        }
        // クリア画面の間はプレイ中のランとして保存されないので、ここで閉じられても次のレベルから続けられるよう別に覚えておく。
        if (!current.isImported && !current.isEditorTest) {
            val state = _uiState.value
            val cleared = ClearedRun(
                level = state.level,
                score = state.score,
                combo = state.combo,
                lives = state.lives,
                bestScoreAtRunStart = state.bestScoreAtRunStart,
                crossedHighScore = state.crossedHighScoreThisRun,
                crossedLevelRecord = state.crossedLevelRecordThisRun,
            )
            viewModelScope.launch { progressStore.saveClearedRun(cleared.encode()) }
        }
    }

    /** クリアして、まだ次のレベルへ進んでいないラン(クリア画面のまま閉じた時の再開用)。 */
    private data class ClearedRun(
        val level: Int,
        val score: Int,
        val combo: Int,
        val lives: Int,
        val bestScoreAtRunStart: Int,
        val crossedHighScore: Boolean,
        val crossedLevelRecord: Boolean,
    ) {
        fun encode(): String = listOf(level, score, combo, lives, bestScoreAtRunStart, crossedHighScore, crossedLevelRecord).joinToString("|")

        /** 次のレベルを始める直前の状態にする(盤面はstartLevelで用意する)。 */
        fun applyTo(state: GameUiState): GameUiState = state.copy(
            level = level + 1,
            score = score,
            combo = combo,
            lives = lives,
            bestScoreAtRunStart = bestScoreAtRunStart,
            crossedHighScoreThisRun = crossedHighScore,
            crossedLevelRecordThisRun = crossedLevelRecord,
            isImported = false,
            isEditorTest = false,
            activeFavoriteImportedAt = null,
            history = emptyList(),
        )
    }

    private fun decodeClearedRun(raw: String): ClearedRun? {
        val f = raw.split("|")
        if (f.size != 7) return null
        return ClearedRun(
            level = f[0].toIntOrNull() ?: return null,
            score = f[1].toIntOrNull() ?: return null,
            combo = f[2].toIntOrNull() ?: return null,
            lives = f[3].toIntOrNull()?.takeIf { it > 0 } ?: return null,
            bestScoreAtRunStart = f[4].toIntOrNull() ?: return null,
            crossedHighScore = f[5].toBooleanStrictOrNull() ?: return null,
            crossedLevelRecord = f[6].toBooleanStrictOrNull() ?: return null,
        )
    }

    private fun gameOver() {
        sfxEngine.playGameOver()
        val current = _uiState.value
        val beatBest = current.score >= current.bestScoreAtRunStart
        if (!current.isImported && current.score > current.bestScore) {
            val newBest = BestRecord(level = current.level, score = current.score)
            viewModelScope.launch { progressStore.saveBest(newBest) }
            _uiState.update { it.copy(bestLevel = newBest.level, bestScore = newBest.score) }
        }
        // ゲームオーバーになってもランは終わらせない(広告で復活できるよう「続きから」に残す)。
        // 自作パズルのテストだけは、抜けると本来のランに戻るので残さない。
        _uiState.update { it.copy(overlay = Overlay.GameOver(beatBest), hasActiveRun = it.hasActiveRun && !it.isEditorTest) }
    }

    /** ゲームオーバー画面から、広告視聴の報酬としてライフを全回復し、ゲームオーバーになった時点の盤面からそのまま続ける。 */
    fun reviveWithAd() {
        val current = _uiState.value
        if (current.overlay !is Overlay.GameOver) return
        _uiState.update {
            it.copy(
                lives = GameUiState().lives,
                busy = false,
                activeShot = null,
                comboPopup = null,
                overlay = Overlay.None,
                hasActiveRun = true,
            )
        }
        showToast("📺 広告視聴でライフ全回復!続きから挑戦できます")
    }

    /** リワード広告がまだ視聴できない状態だった場合に呼ぶ。 */
    fun notifyAdUnavailable() {
        showToast("広告の準備ができていません。少し待ってからもう一度お試しください", isDanger = true)
    }

    /** 進行中のラン(自作パズルのテストプレイを除く)を復元可能な形へシリアライズする。runが無ければnull。
     * historyも含めることで、アプリがプロセス再起動を挟んで「再開する」で復帰した後にクリア→お気に入り登録しても、
     * 再生ログ(replayLog)が復帰前の手を欠いた不完全なものにならないようにする。 */
    private fun serializeActiveRun(state: GameUiState): String? {
        // ライフ0(ゲームオーバー中)のランも保存する。ホームに戻ったりアプリを閉じたりしても、
        // 「続きから」でゲームオーバー画面に戻り、広告で復活すれば続けられるようにするため。
        if (!state.hasActiveRun || state.isEditorTest) return null
        val snapshot = state.levelSnapshot ?: return null
        return listOf(
            state.level,
            state.score,
            state.combo,
            state.lives,
            state.isImported,
            snapshot.lives,
            snapshot.score,
            snapshot.combo,
            BoardCode.encode(state.board),
            BoardCode.encode(snapshot.board),
            state.crossedHighScoreThisRun,
            state.crossedLevelRecordThisRun,
            state.bestScoreAtRunStart,
            state.activeFavoriteImportedAt ?: "",
            encodeHistory(state.history),
        ).joinToString("|")
    }

    private fun deserializeActiveRun(raw: String): GameUiState? {
        val parts = raw.split("|")
        if (parts.size < 14) return null
        val level = parts[0].toIntOrNull() ?: return null
        val score = parts[1].toIntOrNull() ?: return null
        val combo = parts[2].toIntOrNull() ?: return null
        val lives = parts[3].toIntOrNull() ?: return null
        val isImported = parts[4] == "true"
        val snapshotLives = parts[5].toIntOrNull() ?: return null
        val snapshotScore = parts[6].toIntOrNull() ?: return null
        val snapshotCombo = parts[7].toIntOrNull() ?: return null
        val board = BoardCode.decode(parts[8]) ?: return null
        val snapshotBoard = BoardCode.decode(parts[9]) ?: return null
        val crossedHighScoreThisRun = parts[10] == "true"
        val crossedLevelRecordThisRun = parts[11] == "true"
        val bestScoreAtRunStart = parts[12].toIntOrNull() ?: return null
        val activeFavoriteImportedAt = parts[13].toLongOrNull()
        // 旧形式(historyフィールド無し)との互換のため、14番目のフィールドが無ければ空のhistoryとして扱う。
        val history = parts.getOrNull(14)?.let { decodeHistory(it) } ?: emptyList()
        return GameUiState(
            overlay = Overlay.Home,
            level = level,
            score = score,
            combo = combo,
            lives = lives,
            board = board,
            remainingReceivers = board.tiles.values.count { it is Tile.Receiver },
            history = history,
            levelSnapshot = LevelSnapshot(snapshotBoard, snapshotLives, snapshotScore, snapshotCombo),
            hasActiveRun = true,
            isImported = isImported,
            crossedHighScoreThisRun = crossedHighScoreThisRun,
            crossedLevelRecordThisRun = crossedLevelRecordThisRun,
            bestScoreAtRunStart = bestScoreAtRunStart,
            activeFavoriteImportedAt = activeFavoriteImportedAt,
        )
    }

    private fun encodeTile(tile: Tile): String = when (tile) {
        is Tile.Shooter -> "SH:${tile.dir.name}:${tile.color.name}" + if (tile.egg) ":EGG" else ""
        is Tile.Receiver -> "RC:${tile.color.name}"
    }

    private fun decodeTile(raw: String): Tile? {
        val parts = raw.split(":")
        return when (parts.getOrNull(0)) {
            "SH" -> {
                if (parts.size != 3 && parts.size != 4) return null
                val dir = runCatching { Direction.valueOf(parts[1]) }.getOrNull() ?: return null
                val color = runCatching { LaserColor.valueOf(parts[2]) }.getOrNull() ?: return null
                Tile.Shooter(dir, color, egg = parts.getOrNull(3) == "EGG")
            }
            "RC" -> {
                if (parts.size != 2) return null
                val color = runCatching { LaserColor.valueOf(parts[1]) }.getOrNull() ?: return null
                Tile.Receiver(color)
            }
            else -> null
        }
    }

    /** Undo/再生ログ用の一手履歴を、アクティブラン保存用にシリアライズする。 */
    private fun encodeHistory(history: List<HistoryEntry>): String = history.joinToString(";") { entry ->
        when (entry) {
            is HistoryEntry.Shot -> listOf(
                "S",
                entry.removedAPos.row, entry.removedAPos.col, encodeTile(entry.removedATile),
                entry.removedBPos.row, entry.removedBPos.col, encodeTile(entry.removedBTile),
                entry.gained, if (entry.wasReceiver) 1 else 0, entry.previousCombo,
            ).joinToString(",")
            is HistoryEntry.LightAdded -> listOf(
                "L",
                entry.cell.row, entry.cell.col, encodeTile(entry.tile), entry.previousCombo,
            ).joinToString(",")
        }
    }

    private fun decodeHistory(raw: String): List<HistoryEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { segment -> decodeHistoryEntry(segment) }
    }

    private fun decodeHistoryEntry(segment: String): HistoryEntry? {
        val f = segment.split(",")
        return when (f.getOrNull(0)) {
            "S" -> {
                if (f.size != 10) return null
                val aRow = f[1].toIntOrNull() ?: return null
                val aCol = f[2].toIntOrNull() ?: return null
                val aTile = decodeTile(f[3]) ?: return null
                val bRow = f[4].toIntOrNull() ?: return null
                val bCol = f[5].toIntOrNull() ?: return null
                val bTile = decodeTile(f[6]) ?: return null
                val gained = f[7].toIntOrNull() ?: return null
                val wasReceiver = f[8] == "1"
                val previousCombo = f[9].toIntOrNull() ?: return null
                HistoryEntry.Shot(CellPos(aRow, aCol), aTile, CellPos(bRow, bCol), bTile, gained, wasReceiver, previousCombo)
            }
            "L" -> {
                if (f.size != 5) return null
                val row = f[1].toIntOrNull() ?: return null
                val col = f[2].toIntOrNull() ?: return null
                val tile = decodeTile(f[3]) as? Tile.Shooter ?: return null
                val previousCombo = f[4].toIntOrNull() ?: return null
                HistoryEntry.LightAdded(CellPos(row, col), tile, previousCombo)
            }
            else -> null
        }
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
