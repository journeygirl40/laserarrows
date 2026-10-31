package com.laserarrows.app.engine

sealed class Tile {
    abstract val color: LaserColor

    /**
     * 走り出す子。egg=trueなら「なぞのたまご」: 最初は種類を持たず誰とも出会えないが、
     * 動物の矢印にぶつかるたびにその動物の赤ちゃんに変わる。たまごのcolorは使わない(置き場所としての値)。
     */
    data class Shooter(val dir: Direction, override val color: LaserColor, val egg: Boolean = false) : Tile()
    data class Receiver(override val color: LaserColor) : Tile()

    companion object {
        /** なぞのたまご。種類を持たないので、colorには意味のない仮の値を入れておく。 */
        fun egg(dir: Direction): Shooter = Shooter(dir, LaserColor.RED, egg = true)
    }
}
