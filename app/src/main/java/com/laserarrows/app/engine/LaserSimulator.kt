package com.laserarrows.app.engine

/**
 * ビームは違う色の矢印(Shooter)に当たるたびにその矢印の色・向きへ変化して進み続け、
 * 現在のビーム色と一致するタイル(役割問わず)に届いた時点で接続成立(hit)となる。
 * 色が一致しないReceiverに当たった場合はそこでビームが止まる(ブロック)。
 * この「色一致判定がShooter判定より先」という優先順位が原典(HTML版)の仕様。
 */
object LaserSimulator {
    fun fireLaser(board: Board, start: CellPos): LaserResult {
        val startTile = board.tiles[start] as? Tile.Shooter
            ?: return LaserResult(null, listOf(start), emptyList())

        var pos = start
        var dir = startTile.dir
        var curColor = startTile.color
        val path = mutableListOf(start)
        val colorTrail = mutableListOf<LaserColor>()
        val maxSteps = board.rows * board.cols * 4
        val visited = HashSet<Triple<CellPos, Direction, LaserColor>>()

        repeat(maxSteps) {
            // 同じ(位置, 向き, 色)の組み合わせが再び現れたら、以降は必ず同じ結果を繰り返す
            // 無限ループなので、maxStepsまで待たずここで打ち切る。
            if (!visited.add(Triple(pos, dir, curColor))) {
                return LaserResult(null, path, colorTrail)
            }
            colorTrail += curColor
            pos += dir
            if (!pos.inBounds(board.rows, board.cols)) {
                return LaserResult(null, path, colorTrail)
            }
            path += pos
            if (pos != start) {
                val tile = board.tiles[pos]
                if (tile != null) {
                    when {
                        tile.color == curColor -> return LaserResult(pos, path, colorTrail)
                        tile is Tile.Shooter -> {
                            curColor = tile.color
                            dir = tile.dir
                        }
                        else -> return LaserResult(null, path, colorTrail)
                    }
                }
            }
        }
        return LaserResult(null, path, colorTrail)
    }
}
