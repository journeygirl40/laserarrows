package com.laserarrows.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.laserarrows.app.engine.LaserColor
import com.laserarrows.app.ui.theme.AnimalCatalog
import com.laserarrows.app.ui.theme.AnimalLook
import com.laserarrows.app.ui.theme.AnimalLooks
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** 写真から作った、自分だけの動物。 */
data class CustomAnimal(val id: String, val name: String, val bitmap: ImageBitmap) {
    val look: AnimalLook.Custom get() = AnimalLook.Custom(id, name, bitmap)
}

/** どうぶつの設定: 写真から作った動物の一覧と、色ごとの割り当て(AnimalLook.keyの形)。 */
data class AnimalSetup(
    val customs: List<CustomAnimal> = emptyList(),
    val assignment: Map<LaserColor, String> = emptyMap(),
) {
    private val customById = customs.associateBy { it.id }

    fun lookOf(key: String): AnimalLook? = when {
        key.startsWith("b:") -> AnimalCatalog.builtin(key.removePrefix("b:"))?.let { AnimalLook.Builtin(it) }
        key.startsWith("c:") -> customById[key.removePrefix("c:")]?.look
        else -> null
    }

    /** 色ごとの見た目。割り当てが無い・消えた動物を指している色は、最初からいる動物に戻す。 */
    val looks: AnimalLooks = AnimalLooks(
        LaserColor.entries.associateWith { color -> assignment[color]?.let { lookOf(it) } ?: AnimalCatalog.defaultLook(color) },
    )

    /** 色ごとに実際に使われているキー。 */
    val resolvedKeys: Map<LaserColor, String> get() = LaserColor.entries.associateWith { looks.of(it).key }
}

private val Context.animalDataStore by preferencesDataStore(name = "laserarrows_animals")

/**
 * どうぶつの設定の保存先。割り当てと動物の名前はDataStoreに、写真から作った絵はアプリ専用の保存領域にPNGで置く。
 * 6色の動物は必ずすべて違う種類になるようにする(同じ見た目の色があると、盤面でどれとどれが出会えるのか分からなくなるため)。
 */
class AnimalStore(private val context: Context, private val scope: CoroutineScope) {
    private val assignmentKey = stringPreferencesKey("assignment")
    private val customsKey = stringPreferencesKey("customs")
    private val imageDir = File(context.filesDir, "animals")

    private val _setup = MutableStateFlow(AnimalSetup())
    val setup: StateFlow<AnimalSetup> = _setup.asStateFlow()

    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            runCatching { load() }
            loaded.complete(Unit)
        }
    }

    private suspend fun load() {
        val prefs = context.animalDataStore.data.first()
        val customs = decodeCustoms(prefs[customsKey]).mapNotNull { (id, name) ->
            val bitmap = runCatching { BitmapFactory.decodeFile(imageFile(id).path) }.getOrNull() ?: return@mapNotNull null
            CustomAnimal(id, name, bitmap.asImageBitmap())
        }
        _setup.value = AnimalSetup(customs, decodeAssignment(prefs[assignmentKey]))
    }

    /** colorの動物をkeyに変える。他の色がすでにその動物なら、その色とは入れ替える。 */
    fun assign(color: LaserColor, key: String) = mutate { setup ->
        if (setup.lookOf(key) == null) return@mutate setup
        val keys = setup.resolvedKeys.toMutableMap()
        val previous = keys.getValue(color)
        keys.entries.firstOrNull { it.key != color && it.value == key }?.setValue(previous)
        keys[color] = key
        setup.copy(assignment = keys)
    }

    /** すべての色を最初からいる動物に戻す(写真から作った動物は消さない)。 */
    fun resetAssignment() = mutate { it.copy(assignment = emptyMap()) }

    /** 写真から動物を作って登録し、colorに割り当てる。onDoneは成功したかどうか(メイン側で呼ぶ)。 */
    fun addCustom(uri: Uri, name: String, color: LaserColor, onDone: (Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            loaded.await()
            val ok = runCatching {
                val sticker = makeSticker(decode(uri) ?: return@runCatching false)
                val id = System.currentTimeMillis().toString(36)
                imageDir.mkdirs()
                imageFile(id).outputStream().use { sticker.compress(Bitmap.CompressFormat.PNG, 100, it) }
                mutex.withLock {
                    val current = _setup.value
                    val added = current.copy(customs = current.customs + CustomAnimal(id, cleanName(name), sticker.asImageBitmap()))
                    val keys = added.resolvedKeys.toMutableMap().apply { this[color] = AnimalLook.customKey(id) }
                    save(added.copy(assignment = keys))
                }
                true
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onDone(ok) }
        }
    }

    fun renameCustom(id: String, name: String) = mutate { setup ->
        setup.copy(customs = setup.customs.map { if (it.id == id) it.copy(name = cleanName(name)) else it })
    }

    /** 写真から作った動物を消す。その動物だった色は、まだ誰も使っていない用意済みの動物に戻す(最初の動物を優先)。 */
    fun deleteCustom(id: String) = mutate { setup ->
        val removedKey = AnimalLook.customKey(id)
        val keys = setup.resolvedKeys.toMutableMap()
        for (color in LaserColor.entries) {
            if (keys[color] != removedKey) continue
            val used = keys.values.toSet()
            val fallback = AnimalLook.builtinKey(AnimalCatalog.defaultId(color)).takeIf { it !in used }
                ?: AnimalCatalog.builtins.map { AnimalLook.builtinKey(it.id) }.first { it !in used }
            keys[color] = fallback
        }
        runCatching { imageFile(id).delete() }
        setup.copy(customs = setup.customs.filterNot { it.id == id }, assignment = keys)
    }

    private fun mutate(change: (AnimalSetup) -> AnimalSetup) {
        scope.launch(Dispatchers.IO) {
            loaded.await()
            mutex.withLock { save(change(_setup.value)) }
        }
    }

    private suspend fun save(setup: AnimalSetup) {
        _setup.value = setup
        context.animalDataStore.edit { prefs ->
            prefs[assignmentKey] = setup.assignment.entries.joinToString(";") { (color, key) -> "${color.name}=$key" }
            prefs[customsKey] = setup.customs.joinToString("\n") { c ->
                "${c.id}|${Base64.encodeToString(c.name.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}"
            }
        }
    }

    private fun imageFile(id: String) = File(imageDir, "$id.png")

    private fun cleanName(name: String): String = name.trim().replace("\n", " ").take(MAX_NAME_LENGTH).ifBlank { "なかま" }

    /** 写真を読み込む(大きな写真はステッカーに必要な大きさまで縮めて読む)。 */
    private fun decode(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            // ImageDecoderは写真の向き(縦横の回転情報)も反映してくれる。
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(max(1, min(info.size.width, info.size.height) / (STICKER_SIZE * 2)))
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val shortSide = min(bounds.outWidth, bounds.outHeight)
        if (shortSide <= 0) return null
        var sample = 1
        while (shortSide / (sample * 2) >= STICKER_SIZE * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** 写真の真ん中を丸く切り抜き、白いふちどりをつけたステッカーにする(盤面で他の動物と並んでもなじむように)。 */
    private fun makeSticker(src: Bitmap): Bitmap {
        val side = min(src.width, src.height).toFloat()
        val left = (src.width - side) / 2f
        val top = (src.height - side) / 2f
        val out = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val radius = STICKER_SIZE / 2f
        val rim = STICKER_SIZE * 0.045f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(radius, radius, radius - 1f, paint)
        val scale = 2f * (radius - rim) / side
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply {
                setScale(scale, scale)
                postTranslate(rim - left * scale, rim - top * scale)
            })
        }
        paint.shader = shader
        canvas.drawCircle(radius, radius, radius - rim, paint)
        return out
    }

    private fun decodeAssignment(raw: String?): Map<LaserColor, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val (colorName, key) = entry.split("=", limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            val color = runCatching { LaserColor.valueOf(colorName) }.getOrNull() ?: return@mapNotNull null
            color to key
        }.toMap()
    }

    private fun decodeCustoms(raw: String?): List<Pair<String, String>> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val name = runCatching { String(Base64.decode(parts[1], Base64.NO_WRAP), Charsets.UTF_8) }.getOrDefault("なかま")
            parts[0] to name
        }
    }

    companion object {
        const val STICKER_SIZE = 256
        const val MAX_NAME_LENGTH = 10
        const val MAX_CUSTOMS = 24
    }
}
