package com.laserarrows.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.laserarrows.app.engine.LaserColor

// ---- 「ぷっくり3Dの森」: 日差しの差す明るい森。空色→若草→草原のグラデーションの上に、クリーム色のカードと木枠を置く。 ----

/** 背景の空(上)。 */
val Bg1 = Color(0xFF9FDCFF)
/** 背景の中間(地平線あたりの明るい若草)。 */
val Bg2 = Color(0xFFE6F7D2)
/** 背景の下(草原)。 */
val Bg3 = Color(0xFF9FD777)

/** カード面(クリーム)。旧ガラス面の役割を引き継ぐ。 */
val GlassFill = Color(0xFFFFF8EC)
/** カードの縁取り(淡い小麦色)。 */
val GlassBorder = Color(0xFFEAD5AE)
/** カード上辺のハイライト。 */
val GlassHi = Color(0xFFFFFFFF)
/** カードやボタンの「厚み」(下側に見える側面)。 */
val CardEdge = Color(0xFFDDBF8C)

val TextPrimary = Color(0xFF4A2D19)
val TextMuted = Color(0xFF8C6C52)

val DangerColor = Color(0xFFF0435A)
val GoldColor = Color(0xFFFFB302)
/** クリーム地の上に置く金色の文字(GoldColorのままでは淡くて読みにくいため一段濃くする)。 */
val GoldText = Color(0xFFC47300)
/** メインの行動ボタン(若葉の緑)。 */
val Neon1 = Color(0xFF5BC63B)
/** Neon1ボタンの厚み(側面)。 */
val Neon1Edge = Color(0xFF3A9323)
/** 情報・選択中を示すアクセント(空の青)。 */
val Neon2 = Color(0xFF2B96E0)
/** サブの行動ボタン(夕日のオレンジ)。 */
val PurpleAccent = Color(0xFFFF8A3D)
val PurpleAccentEdge = Color(0xFFD9641C)
/** 「あと少し」を煽るニアミス演出用。 */
val EmberColor = Color(0xFFFF7A00)

/** なぞのたまごが、まだ何にも化けていない間に走った跡の色(殻の生成り色)。 */
val EggTrail = Color(0xFFE0B871)

/** 盤面の木枠。 */
val Wood = Color(0xFFC88A4E)
val WoodDark = Color(0xFF8E5A2C)
/** 盤面の芝生。 */
val BoardGrass = Color(0xFFDDF1C6)
val BoardGrassDark = Color(0xFFC9E6AC)

/** 動物ごとのテーマ色(マスの色・走る軌跡の色)。明るい背景でも沈まない、彩度高めのパステル。 */
fun LaserColor.toComposeColor(): Color = when (this) {
    LaserColor.RED -> Color(0xFFFF7A3D)
    LaserColor.BLUE -> Color(0xFF42B0F5)
    LaserColor.GREEN -> Color(0xFF63C94A)
    LaserColor.YELLOW -> Color(0xFFFFC21F)
    LaserColor.PURPLE -> Color(0xFFB98AF5)
    LaserColor.ORANGE -> Color(0xFFB9784A)
}
