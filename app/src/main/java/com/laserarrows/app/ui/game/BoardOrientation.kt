package com.laserarrows.app.ui.game

import androidx.compose.runtime.Composable
import com.laserarrows.app.engine.Board
import com.laserarrows.app.engine.CellPos
import com.laserarrows.app.engine.Direction
import com.laserarrows.app.engine.LaserResult
import com.laserarrows.app.engine.Tile
import com.laserarrows.app.ui.components.isLandscape

/*
 * 盤面の「見せ方」だけを、左上→右下の対角線で折り返す(行と列を入れ替える)ための変換。
 * 縦長の盤面を横向きの画面で見る時に横長にして、マスを大きく見せるのに使う。
 * 矢印の向きも一緒に折り返す(↑⇔←、↓⇔→)ので、どの子がどこへ走ってどこで出会うかは元の盤面とまったく同じ。
 * ゲームの中身(ルール・記録・共有コード・振り返りのログ)は常に元の向きのままで、描く直前とタップを受けた直後だけ変換する。
 * 2回折り返すと元に戻るので、見た目→元の盤面への変換にも同じ関数を使う。
 */

/** この盤面を、今の画面の向きに合わせて折り返して見せるか(横向きなのに縦長/縦向きなのに横長の盤面)。 */
@Composable
fun shouldFlipBoard(board: Board): Boolean {
    val landscape = isLandscape()
    return if (landscape) board.rows > board.cols else board.cols > board.rows
}

fun CellPos.flipped(): CellPos = CellPos(col, row)

fun Direction.flipped(): Direction = when (this) {
    Direction.U -> Direction.L
    Direction.L -> Direction.U
    Direction.D -> Direction.R
    Direction.R -> Direction.D
}

fun Tile.flipped(): Tile = when (this) {
    is Tile.Shooter -> copy(dir = dir.flipped())
    is Tile.Receiver -> this
}

fun Board.flipped(): Board = Board(
    rows = cols,
    cols = rows,
    tiles = tiles.entries.associate { (pos, tile) -> pos.flipped() to tile.flipped() },
)

fun LaserResult.flipped(): LaserResult = copy(hit = hit?.flipped(), path = path.map { it.flipped() })

fun ShotAnimation.flipped(): ShotAnimation = copy(
    originKey = originKey.flipped(),
    result = result.flipped(),
    bends = bends.mapTo(mutableSetOf()) { it.flipped() },
)

fun ClearFinale.flipped(): ClearFinale = copy(bursts = bursts.map { it.copy(pos = it.pos.flipped()) })

/** flipがtrueの時だけ折り返す(見た目⇔元の盤面のどちら向きにも使える)。 */
fun Board.flippedIf(flip: Boolean) = if (flip) flipped() else this
fun CellPos.flippedIf(flip: Boolean) = if (flip) flipped() else this
fun Direction.flippedIf(flip: Boolean) = if (flip) flipped() else this
fun ShotAnimation.flippedIf(flip: Boolean) = if (flip) flipped() else this
fun ClearFinale.flippedIf(flip: Boolean) = if (flip) flipped() else this
fun LaserResult.flippedIf(flip: Boolean) = if (flip) flipped() else this
