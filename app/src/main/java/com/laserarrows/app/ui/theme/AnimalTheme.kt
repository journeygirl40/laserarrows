package com.laserarrows.app.ui.theme

import androidx.annotation.DrawableRes
import com.laserarrows.app.R
import com.laserarrows.app.engine.LaserColor

/**
 * 題材「動物のつがい」: 色ごとに1種類の動物を割り当てる。
 * 矢印つきの子が走り出し、違う種類の矢印の子に当たるとその子の向きへ道案内される(種類は変わらない)。
 * 同じ種類の相手に出会うとカップル成立して赤ちゃんが生まれる。
 * 盤面などで使う3Dの動物イラスト(Microsoft Fluent Emoji, MIT)。
 */
@DrawableRes
fun LaserColor.animalRes(): Int = when (this) {
    LaserColor.RED -> R.drawable.animal_fox
    LaserColor.BLUE -> R.drawable.animal_penguin
    LaserColor.GREEN -> R.drawable.animal_frog
    LaserColor.YELLOW -> R.drawable.animal_lion
    LaserColor.PURPLE -> R.drawable.animal_rabbit
    LaserColor.ORANGE -> R.drawable.animal_bear
}

fun LaserColor.animalName(): String = when (this) {
    LaserColor.RED -> "キツネ"
    LaserColor.BLUE -> "ペンギン"
    LaserColor.GREEN -> "カエル"
    LaserColor.YELLOW -> "ライオン"
    LaserColor.PURPLE -> "ウサギ"
    LaserColor.ORANGE -> "クマ"
}
