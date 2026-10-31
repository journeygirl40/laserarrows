package com.laserarrows.app.ui.game

/**
 * タイトルの森の「にぎわい」段階。ハイスコアが伸びるほど住人が増え、森が生き生きしていく。
 * 境目は、パー通りに遊んだ時のランの累計(Lv.10で約290匹、Lv.20で約2000匹、Lv.30で約7000匹)を目安に、
 * 始めたばかりでもすぐ1〜2段は育ち、上の段ほど腕前が要るように置いている。
 */
object ForestStage {
    /** level=段階(0〜)、minScore=この段階になるハイスコア、news=この段階で森に起きること(段階が上がった時の知らせ)。 */
    data class Stage(val level: Int, val name: String, val minScore: Int, val news: String)

    val stages = listOf(
        Stage(0, "しずかな森", 0, ""),
        Stage(1, "めざめの森", 1, "なかまたちが目をさました"),
        Stage(2, "なかよしの森", 50, "カエルがやってきて、お花が咲いた"),
        Stage(3, "にぎやかな森", 150, "クマとちょうちょがやってきた"),
        Stage(4, "みんなの森", 400, "ライオンも来て、みんながそろった"),
        Stage(5, "赤ちゃんの森", 1000, "赤ちゃんが生まれて、おうちが建った"),
        Stage(6, "おまつりの森", 2500, "森じゅうでおまつりが始まった"),
        Stage(7, "黄金の森", 5000, "森が黄金の光につつまれた"),
    )

    fun of(bestScore: Int): Stage = stages.last { bestScore >= it.minScore }

    fun next(stage: Stage): Stage? = stages.getOrNull(stage.level + 1)
}
