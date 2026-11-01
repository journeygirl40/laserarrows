package com.laserarrows.app.ui.game

/**
 * タイトルの森の「にぎわい」段階。集めた★(レベルごとの最高★の合計)が増えるほど住人が増え、森が生き生きしていく。
 * ★は1レベル最大3つ。境目は、全部★3で進んだ時にLv.10で3段目、Lv.30で5段目、Lv.80で最後の段になるよう置いている
 * (始めたばかりでもすぐ1〜2段は育ち、上の段ほど★3を取りこぼさない腕前が要る)。
 */
object ForestStage {
    /** level=段階(0〜)、minScore=この段階になる値(にぎわいは集めた★の数、広さは森のなかまの数)、news=この段階で森に起きること。 */
    data class Stage(val level: Int, val name: String, val minScore: Int, val news: String)

    val stages = listOf(
        Stage(0, "しずかな森", 0, ""),
        Stage(1, "めざめの森", 1, "なかまたちが目をさました"),
        Stage(2, "なかよしの森", 10, "カエルがやってきた"),
        Stage(3, "にぎやかな森", 25, "クマとちょうちょがやってきた"),
        Stage(4, "みんなの森", 50, "ライオンも来て、みんながそろった"),
        Stage(5, "赤ちゃんの森", 90, "赤ちゃんが生まれた"),
        Stage(6, "おまつりの森", 150, "森じゅうでおまつりが始まった"),
        Stage(7, "黄金の森", 240, "森が黄金の光につつまれた"),
    )

    /** 集めた★の数から、にぎわいの段階を決める。 */
    fun of(totalStars: Int): Stage = stages.last { totalStars >= it.minScore }

    fun next(stage: Stage): Stage? = stages.getOrNull(stage.level + 1)
}

/**
 * タイトルの森の「広さ」段階。ハイスコアに関係なく、これまでに生まれた赤ちゃんの累計(森のなかま)で育ち、
 * 段階ごとに花畑・小川・橋・池…と景色が増えて丘も広くなる。先へ進む(レベルアップチャレンジ)・遊び直す、
 * のどちらでも育つので、コンボを気にしない遊び方にも目に見えるご褒美が付く。
 * 境目は、パー通りのランの累計(Lv.10で約300匹、Lv.20で約2000匹)を目安に置いている。
 */
object ForestExpansion {
    /** minScoreは、この段階になる森のなかまの数。 */
    val stages = listOf(
        ForestStage.Stage(0, "はじまりの丘", 0, ""),
        ForestStage.Stage(1, "花畑の丘", 30, "花畑ができた"),
        ForestStage.Stage(2, "小川の森", 100, "小川が流れはじめた"),
        ForestStage.Stage(3, "橋のある森", 250, "木の橋がかかった"),
        ForestStage.Stage(4, "池のある森", 500, "池にスイレンが咲いた"),
        ForestStage.Stage(5, "おうちの森", 1000, "動物たちのおうちが建った"),
        ForestStage.Stage(6, "大きな木の森", 2000, "大きな木にツリーハウスができた"),
        ForestStage.Stage(7, "ホタルの森", 4000, "ホタルが光りはじめた"),
        ForestStage.Stage(8, "虹の森", 8000, "森に虹がかかった"),
    )

    fun of(population: Int): ForestStage.Stage = stages.last { population >= it.minScore }

    fun next(stage: ForestStage.Stage): ForestStage.Stage? = stages.getOrNull(stage.level + 1)
}
