package com.laserarrows.app.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import com.laserarrows.app.R
import com.laserarrows.app.engine.LaserColor

/**
 * 題材「動物のつがい」: 色ごとに1種類の動物を割り当てる。
 * 矢印つきの子が走り出し、違う種類の矢印の子に当たるとその子の向きへ道案内される(種類は変わらない)。
 * 同じ種類の相手に出会うとカップル成立して赤ちゃんが生まれる。
 * 盤面などで使う3Dの動物イラスト(Microsoft Fluent Emoji, MIT)。
 *
 * どの色にどの動物を割り当てるかは、Lv.20到達後に「どうぶつの設定」で自由に変えられる。
 * 最初の6匹のほかに選べる動物は、Lv.20で1匹、そこから5レベルごとに1匹ずつ増えていく。
 * 用意した動物がすべてそろった次の枠で、自分の写真を動物として登録できるようになる。
 */
data class BuiltinAnimal(val id: String, val name: String, @DrawableRes val res: Int)

object AnimalCatalog {
    /** 選べる動物の一覧(最初の6種は最初からいるなかま)。 */
    val builtins: List<BuiltinAnimal> = listOf(
        BuiltinAnimal("fox", "キツネ", R.drawable.animal_fox),
        BuiltinAnimal("penguin", "ペンギン", R.drawable.animal_penguin),
        BuiltinAnimal("frog", "カエル", R.drawable.animal_frog),
        BuiltinAnimal("lion", "ライオン", R.drawable.animal_lion),
        BuiltinAnimal("rabbit", "ウサギ", R.drawable.animal_rabbit),
        BuiltinAnimal("bear", "クマ", R.drawable.animal_bear),
        BuiltinAnimal("cat", "ネコ", R.drawable.animal_cat),
        BuiltinAnimal("dog", "イヌ", R.drawable.animal_dog),
        BuiltinAnimal("panda", "パンダ", R.drawable.animal_panda),
        BuiltinAnimal("koala", "コアラ", R.drawable.animal_koala),
        BuiltinAnimal("tiger", "トラ", R.drawable.animal_tiger),
        BuiltinAnimal("monkey", "サル", R.drawable.animal_monkey),
        BuiltinAnimal("pig", "ブタ", R.drawable.animal_pig),
        BuiltinAnimal("cow", "ウシ", R.drawable.animal_cow),
        BuiltinAnimal("mouse", "ネズミ", R.drawable.animal_mouse),
        BuiltinAnimal("hamster", "ハムスター", R.drawable.animal_hamster),
        BuiltinAnimal("polar_bear", "シロクマ", R.drawable.animal_polar_bear),
        BuiltinAnimal("raccoon", "アライグマ", R.drawable.animal_raccoon),
        BuiltinAnimal("wolf", "オオカミ", R.drawable.animal_wolf),
        BuiltinAnimal("horse", "ウマ", R.drawable.animal_horse),
        BuiltinAnimal("unicorn", "ユニコーン", R.drawable.animal_unicorn),
        BuiltinAnimal("dragon", "ドラゴン", R.drawable.animal_dragon),
        BuiltinAnimal("chick", "ヒヨコ", R.drawable.animal_chick),
        BuiltinAnimal("chicken", "ニワトリ", R.drawable.animal_chicken),
        BuiltinAnimal("duck", "カモ", R.drawable.animal_duck),
        BuiltinAnimal("owl", "フクロウ", R.drawable.animal_owl),
        BuiltinAnimal("parrot", "オウム", R.drawable.animal_parrot),
        BuiltinAnimal("peacock", "クジャク", R.drawable.animal_peacock),
        BuiltinAnimal("flamingo", "フラミンゴ", R.drawable.animal_flamingo),
        BuiltinAnimal("dodo", "ドードー", R.drawable.animal_dodo),
        BuiltinAnimal("bat", "コウモリ", R.drawable.animal_bat),
        BuiltinAnimal("hedgehog", "ハリネズミ", R.drawable.animal_hedgehog),
        BuiltinAnimal("beaver", "ビーバー", R.drawable.animal_beaver),
        BuiltinAnimal("otter", "ラッコ", R.drawable.animal_otter),
        BuiltinAnimal("badger", "アナグマ", R.drawable.animal_badger),
        BuiltinAnimal("sloth", "ナマケモノ", R.drawable.animal_sloth),
        BuiltinAnimal("kangaroo", "カンガルー", R.drawable.animal_kangaroo),
        BuiltinAnimal("deer", "シカ", R.drawable.animal_deer),
        BuiltinAnimal("goat", "ヤギ", R.drawable.animal_goat),
        BuiltinAnimal("llama", "リャマ", R.drawable.animal_llama),
        BuiltinAnimal("zebra", "シマウマ", R.drawable.animal_zebra),
        BuiltinAnimal("giraffe", "キリン", R.drawable.animal_giraffe),
        BuiltinAnimal("elephant", "ゾウ", R.drawable.animal_elephant),
        BuiltinAnimal("hippopotamus", "カバ", R.drawable.animal_hippopotamus),
        BuiltinAnimal("rhinoceros", "サイ", R.drawable.animal_rhinoceros),
        BuiltinAnimal("gorilla", "ゴリラ", R.drawable.animal_gorilla),
        BuiltinAnimal("crocodile", "ワニ", R.drawable.animal_crocodile),
        BuiltinAnimal("turtle", "カメ", R.drawable.animal_turtle),
        BuiltinAnimal("snail", "カタツムリ", R.drawable.animal_snail),
        BuiltinAnimal("butterfly", "チョウ", R.drawable.animal_butterfly),
        BuiltinAnimal("seal", "アザラシ", R.drawable.animal_seal),
        BuiltinAnimal("dolphin", "イルカ", R.drawable.animal_dolphin),
        BuiltinAnimal("whale", "クジラ", R.drawable.animal_whale),
        BuiltinAnimal("octopus", "タコ", R.drawable.animal_octopus),
    )

    private val byId = builtins.associateBy { it.id }
    private val indexById = builtins.withIndex().associate { (i, a) -> a.id to i }

    /** 最初からいる動物の数(これらはいつでも選べる)。 */
    const val STARTER_COUNT = 6

    /** どうぶつの設定を開けるようになるレベル(到達したら解放)。ここで7匹目も選べるようになる。 */
    const val SETTINGS_UNLOCK_LEVEL = 20

    /** 7匹目以降は、このレベル数ごとに1匹ずつ選べるようになる。 */
    const val UNLOCK_STEP = 5

    /** その動物を選べるようになるレベル。最初の6匹は1(=最初から)。 */
    fun unlockLevel(animal: BuiltinAnimal): Int {
        val index = indexById.getValue(animal.id)
        return if (index < STARTER_COUNT) 1 else SETTINGS_UNLOCK_LEVEL + (index - STARTER_COUNT) * UNLOCK_STEP
    }

    /** 到達レベルまでに選べるようになった動物の数。 */
    fun unlockedCount(levelReached: Int): Int = builtins.count { unlockLevel(it) <= levelReached }

    /** 写真から動物を作れるようになるレベル。用意した動物がすべてそろった、その次の枠。 */
    val PHOTO_UNLOCK_LEVEL: Int = SETTINGS_UNLOCK_LEVEL + (builtins.size - STARTER_COUNT) * UNLOCK_STEP

    fun builtin(id: String): BuiltinAnimal? = byId[id]

    /** 最初から決まっている、色ごとの動物。 */
    fun defaultId(color: LaserColor): String = when (color) {
        LaserColor.RED -> "fox"
        LaserColor.BLUE -> "penguin"
        LaserColor.GREEN -> "frog"
        LaserColor.YELLOW -> "lion"
        LaserColor.PURPLE -> "rabbit"
        LaserColor.ORANGE -> "bear"
    }

    fun defaultLook(color: LaserColor): AnimalLook.Builtin = AnimalLook.Builtin(byId.getValue(defaultId(color)))
}

/** 色ごとの動物の見た目。用意した動物か、写真から作った自分だけの動物。 */
sealed interface AnimalLook {
    /** 保存用のキー(「b:fox」「c:<id>」)。同じキーなら同じ動物。 */
    val key: String
    val name: String

    data class Builtin(val animal: BuiltinAnimal) : AnimalLook {
        override val key: String get() = builtinKey(animal.id)
        override val name: String get() = animal.name
    }

    data class Custom(val id: String, override val name: String, val bitmap: ImageBitmap) : AnimalLook {
        override val key: String get() = customKey(id)
    }

    companion object {
        fun builtinKey(id: String) = "b:$id"
        fun customKey(id: String) = "c:$id"
    }
}

/** いま遊んでいる盤面で、色ごとにどの動物を描くか。 */
class AnimalLooks(private val looks: Map<LaserColor, AnimalLook>) {
    fun of(color: LaserColor): AnimalLook = looks[color] ?: AnimalCatalog.defaultLook(color)

    companion object {
        val Default = AnimalLooks(emptyMap())
    }
}

val LocalAnimalLooks = compositionLocalOf { AnimalLooks.Default }

@Composable
fun AnimalLook.painter(): Painter = when (this) {
    is AnimalLook.Builtin -> painterResource(animal.res)
    is AnimalLook.Custom -> remember(bitmap) { BitmapPainter(bitmap) }
}

@Composable
fun AnimalLook.imageBitmap(): ImageBitmap = when (this) {
    is AnimalLook.Builtin -> ImageBitmap.imageResource(animal.res)
    is AnimalLook.Custom -> bitmap
}

/** その色の動物の絵(盤面・ポップアップ・パレードなど、動物を描く所はすべてここを通す)。 */
@Composable
fun animalPainter(color: LaserColor): Painter = LocalAnimalLooks.current.of(color).painter()

@Composable
fun animalBitmap(color: LaserColor): ImageBitmap = LocalAnimalLooks.current.of(color).imageBitmap()

@Composable
fun animalName(color: LaserColor): String = LocalAnimalLooks.current.of(color).name
