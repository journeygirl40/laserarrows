package com.laserarrows.app.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BestRecord(val level: Int = 1, val score: Int = 0)

data class ImportHistoryEntry(val code: String, val memo: String, val importedAt: Long)

private val Context.progressDataStore by preferencesDataStore(name = "laserarrows_progress")

class ProgressStore(private val context: Context) {
    private val bestLevelKey = intPreferencesKey("best_level")
    private val bestScoreKey = intPreferencesKey("best_score")
    private val clearedLevelsKey = stringPreferencesKey("cleared_levels")
    private val importHistoryKey = stringPreferencesKey("import_history")
    private val levelBestLivesLostKey = stringPreferencesKey("level_best_lives_lost")
    private val levelBestScoreKey = stringPreferencesKey("level_best_score")

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

    suspend fun addImportHistory(code: String, memo: String) {
        context.progressDataStore.edit { prefs ->
            val current = decodeImportHistory(prefs[importHistoryKey])
            val updated = (listOf(ImportHistoryEntry(code, memo, System.currentTimeMillis())) + current)
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

    private fun encodeImportHistory(entries: List<ImportHistoryEntry>): String = entries.joinToString("\n") { entry ->
        val memoB64 = Base64.encodeToString(entry.memo.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        "${entry.importedAt}|${entry.code}|$memoB64"
    }

    private fun decodeImportHistory(raw: String?): List<ImportHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val importedAt = parts[0].toLongOrNull() ?: return@mapNotNull null
            val memo = runCatching {
                String(Base64.decode(parts[2], Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrDefault("")
            ImportHistoryEntry(code = parts[1], memo = memo, importedAt = importedAt)
        }
    }

    companion object {
        private const val MAX_IMPORT_HISTORY = 50
    }
}
