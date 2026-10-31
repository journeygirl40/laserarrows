package com.laserarrows.app.engine

/**
 * 走り出した子(ビーム)は、違う種類の矢印(Shooter)に当たるとその矢印の向きへ曲がる(道案内)が、
 * 自分の種類(色)は変わらない(バトンタッチはしない)。
 * 自分と同じ種類のタイル(役割問わず)に届いた時点で出会い成立(hit)となる。
 * 種類が違うReceiverに当たった場合はそこで止まる(ブロック)。
 * 「色一致判定がShooter判定より先」という優先順位は原典(HTML版)の仕様のまま。
 *
 * なぞのたまご: 最初は種類を持たない(誰とも出会えない)。動物の矢印にぶつかるたびに、
 * その動物の赤ちゃんに変わってその向きへ曲がる。最後に変わった動物と同じ種類の子に届けば出会い成立。
 * 走る子がたまご(矢印)に当たった時は、たまごには種類が無いので出会わず、向きだけ変わる。
 */
object LaserSimulator {
    fun fireLaser(board: Board, start: CellPos): LaserResult {
        val startTile = board.tiles[start] as? Tile.Shooter
            ?: return LaserResult(null, listOf(start), emptyList())

        var pos = start
        var dir = startTile.dir
        val runnerIsEgg = startTile.egg
        var curColor: LaserColor? = if (runnerIsEgg) null else startTile.color
        val path = mutableListOf(start)
        val colorTrail = mutableListOf<LaserColor?>()
        val maxSteps = board.rows * board.cols * 4
        val visited = HashSet<Triple<CellPos, Direction, LaserColor?>>()
        var redirects = 0

        repeat(maxSteps) {
            // 同じ(位置, 向き, 色)の組み合わせが再び現れたら、以降は必ず同じ結果を繰り返す
            // 無限ループなので、maxStepsまで待たずここで打ち切る。
            if (!visited.add(Triple(pos, dir, curColor))) {
                return LaserResult(null, path, colorTrail, redirects)
            }
            colorTrail += curColor
            pos += dir
            if (!pos.inBounds(board.rows, board.cols)) {
                return LaserResult(null, path, colorTrail, redirects)
            }
            path += pos
            if (pos != start) {
                val tile = board.tiles[pos]
                if (tile != null) {
                    when {
                        tile is Tile.Shooter && tile.egg -> {
                            if (tile.dir != dir) redirects++
                            dir = tile.dir
                        }
                        curColor != null && tile.color == curColor -> return LaserResult(pos, path, colorTrail, redirects)
                        tile is Tile.Shooter -> {
                            if (tile.dir != dir) redirects++
                            dir = tile.dir
                            if (runnerIsEgg) curColor = tile.color
                        }
                        else -> return LaserResult(null, path, colorTrail, redirects)
                    }
                }
            }
        }
        return LaserResult(null, path, colorTrail, redirects)
    }
}
