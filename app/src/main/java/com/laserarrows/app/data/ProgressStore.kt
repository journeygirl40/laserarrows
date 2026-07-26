package com.laserarrows.app.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class BestRecord(val level: Int = 1, val score: Int = 0)

data class ImportHistoryEntry(val code: String, val memo: String, val importedAt: Long, val level: Int? = null)

private val Context.progressDataStore by preferencesDataStore(name = "laserarrows_progress")

class ProgressStore(private val context: Context) {
    private val bestLevelKey = intPreferencesKey("best_level")
    private val bestScoreKey = intPreferencesKey("best_score")
    private val bestLevelReachedKey = intPreferencesKey("best_level_reached")
    private val clearedLevelsKey = stringPreferencesKey("cleared_levels")
    private val importHistoryKey = stringPreferencesKey("import_history")
    private val levelBestLivesLostKey = stringPreferencesKey("level_best_lives_lost")
    private val levelBestScoreKey = stringPreferencesKey("level_best_score")
    private val activeRunKey = stringPreferencesKey("active_run")

    val best: Flow<BestRecord> = context.progressDataStore.data.map { prefs ->
        BestRecord(
            level = prefs[bestLevelKey] ?: 1,
            score = prefs[bestScoreKey] ?: 0,
        )
    }

    val clearedLevels: Flow<Set<Int>> = context.progressDataStore.data.map { prefs ->
        prefs[clearedLevelsKey]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun saveBest(record: BestRecord) {
        context.progressDataStore.edit { prefs ->
            prefs[bestLevelKey] = record.level
            prefs[bestScoreKey] = record.score
        }
    }

    /** 「ハイスコア」とは独立した、これまでに到達した最も深いレベル(クリアできたかは問わない)。一度上がった記録は下がらない。 */
    val bestLevelReached: Flow<Int> = context.progressDataStore.data.map { prefs ->
        prefs[bestLevelReachedKey] ?: 1
    }

    suspend fun recordLevelReached(level: Int) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[bestLevelReachedKey] ?: 1
            if (level > current) prefs[bestLevelReachedKey] = level
        }
    }

    suspend fun markLevelCleared(level: Int) {
        context.progressDataStore.edit { prefs ->
            val current = prefs[clearedLevelsKey]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { it.toIntOrNull() }
                ?.toMutableSet()
                ?: mutableSetOf()
            current += level
            prefs[clearedLevelsKey] = current.joinToString(",")
        }
    }

    /** レベルごとの「これまでで最も少なかった消費ライフ数」(0ならパーフェクト)。一度良くなった評価は下がらない。 */
    val levelBestLivesLost: Flow<Map<Int, Int>> = context.progressDataStore.data.map { prefs ->
        decodeLevelIntMap(prefs[levelBestLivesLostKey])
    }

    /** レベルごとの「これまでで最も高かったクリア時スコア」。一度良くなった評価は下がらない。 */
    val levelBestScore: Flow<Map<Int, Int>> = context.progressDataStore.data.map { prefs ->
        decodeLevelIntMap(prefs[levelBestScoreKey])
    }

    suspend fun recordLevelClear(level: Int, livesLost: Int, score: Int) {
        context.progressDataStore.edit { prefs ->
            val currentLivesLost = decodeLevelIntMap(prefs[levelBestLivesLostKey]).toMutableMap()
            val existingLivesLost = currentLivesLost[level]
            if (existingLivesLost == null || livesLost < existingLivesLost) {
                currentLivesLost[level] = livesLost
                prefs[levelBestLivesLostKey] = encodeLevelIntMap(currentLivesLost)
            }

            val currentBestScore = decodeLevelIntMap(prefs[levelBestScoreKey]).toMutableMap()
            val existingBestScore = currentBestScore[level]
            if (existingBestScore == null || score > existingBestScore) {
                currentBestScore[level] = score
                prefs[levelBestScoreKey] = encodeLevelIntMap(currentBestScore)
            }
        }
    }

    suspend fun resetLevelRatings() {
        context.progressDataStore.edit { prefs ->
            prefs.remove(levelBestLivesLostKey)
            prefs.remove(levelBestScoreKey)
        }
    }

    /** プレイ中のラン(盤面・ライフ・スコア等)を保存する。アプリのプロセスが落ちても「再開する」で復元できるようにするため。 */
    suspend fun saveActiveRun(serialized: String?) {
        context.progressDataStore.edit { prefs ->
            if (serialized == null) {
                prefs.remove(activeRunKey)
            } else {
                prefs[activeRunKey] = serialized
            }
        }
    }

    suspend fun loadActiveRun(): String? = context.progressDataStore.data.first()[activeRunKey]

    private fun encodeLevelIntMap(map: Map<Int, Int>): String =
        map.entries.joinToString(",") { (level, value) -> "$level:$value" }

    private fun decodeLevelIntMap(raw: String?): Map<Int, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val level = parts[0].toIntOrNull() ?: return@mapNotNull null
            val value = parts[1].toIntOrNull() ?: return@mapNotNull null
            level to value
        }.toMap()
    }

    val importHistory: Flow<List<ImportHistoryEntry>> = context.progressDataStore.data.map { prefs ->
        decodeImportHistory(prefs[importHistoryKey])
    }

    suspend fun addImportHistory(code: String, memo: String, level: Int? = null) {
        context.progressDataStore.edit { prefs ->
            val current = decodeImportHistory(prefs[importHistoryKey])
            val updated = (listOf(ImportHistoryEntry(code, memo, System.currentTimeMillis(), level)) + current)
                .take(MAX_IMPORT_HISTORY)
            prefs[importHistoryKey] = encodeImportHistory(updated)
        }
    }

    suspend fun removeImportHistory(importedAt: Long) {
        context.progressDataStore.edit { prefs ->
            val current = decodeImportHistory(prefs[importHistoryKey])
            val updated = current.filterNot { it.importedAt == importedAt }
            prefs[importHistoryKey] = encodeImportHistory(updated)
        }
    }

    suspend fun updateImportHistoryMemo(importedAt: Long, memo: String) {
        context.progressDataStore.edit { prefs ->
            val current = decodeImportHistory(prefs[importHistoryKey])
            val updated = current.map { entry ->
                if (entry.importedAt == importedAt) entry.copy(memo = memo) else entry
            }
            prefs[importHistoryKey] = encodeImportHistory(updated)
        }
    }

    private fun encodeImportHistory(entries: List<ImportHistoryEntry>): String = entries.joinToString("\n") { entry ->
        val memoB64 = Base64.encodeToString(entry.memo.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        "${entry.importedAt}|${entry.code}|$memoB64|${entry.level ?: ""}"
    }

    private fun decodeImportHistory(raw: String?): List<ImportHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            // 旧形式(levelフィールド無し)との互換のため、末尾のlevelは無くても読み込めるようにする。
            val parts = line.split("|", limit = 4)
            if (parts.size < 3) return@mapNotNull null
            val importedAt = parts[0].toLongOrNull() ?: return@mapNotNull null
            val memo = runCatching {
                String(Base64.decode(parts[2], Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrDefault("")
            val level = parts.getOrNull(3)?.toIntOrNull()
            ImportHistoryEntry(code = parts[1], memo = memo, importedAt = importedAt, level = level)
        }
    }

    companion object {
        private const val MAX_IMPORT_HISTORY = 50
    }
}
