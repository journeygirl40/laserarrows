package com.laserarrows.app.engine

/**
 * 盤面を短いテキストコードへ変換し、他の端末のアプリへ貼り付けてそのまま
 * 同じパズルを再生できるようにする(SNS・メッセージアプリでの共有を想定)。
 */
object BoardCode {
    private const val PREFIX = "LAZ1"
    private const val MAX_SIZE = 40
    private const val EGG_CHAR = '?'

    private fun colorChar(c: LaserColor): Char = when (c) {
        LaserColor.RED -> 'R'
        LaserColor.BLUE -> 'B'
        LaserColor.GREEN -> 'G'
        LaserColor.YELLOW -> 'Y'
        LaserColor.PURPLE -> 'P'
        LaserColor.ORANGE -> 'O'
    }

    private fun charToColor(c: Char): LaserColor? = when (c) {
        'R' -> LaserColor.RED
        'B' -> LaserColor.BLUE
        'G' -> LaserColor.GREEN
        'Y' -> LaserColor.YELLOW
        'P' -> LaserColor.PURPLE
        'O' -> LaserColor.ORANGE
        else -> null
    }

    private fun charToDir(c: Char): Direction? = Direction.entries.firstOrNull { it.name[0] == c }

    fun encode(board: Board): String {
        val sb = StringBuilder()
        sb.append(PREFIX).append('-').append(board.rows).append('x').append(board.cols).append('-')
        for (r in 0 until board.rows) {
            for (c in 0 until board.cols) {
                when (val tile = board.tiles[CellPos(r, c)]) {
                    is Tile.Shooter -> {
                        sb.append(tile.dir.name[0])
                        // なぞのたまごは種類を持たないので「?」で表す(旧バージョンのコードには出てこない文字)。
                        sb.append(if (tile.egg) EGG_CHAR else colorChar(tile.color))
                    }
                    is Tile.Receiver -> {
                        sb.append('o')
                        sb.append(colorChar(tile.color))
                    }
                    null -> sb.append("..")
                }
            }
        }
        return sb.toString()
    }

    fun decode(code: String): Board? {
        val trimmed = code.trim()
        val parts = trimmed.split("-")
        if (parts.size != 3 || parts[0] != PREFIX) return null
        val dims = parts[1].split("x")
        if (dims.size != 2) return null
        val rows = dims[0].toIntOrNull() ?: return null
        val cols = dims[1].toIntOrNull() ?: return null
        if (rows <= 0 || cols <= 0 || rows > MAX_SIZE || cols > MAX_SIZE) return null

        val cells = parts[2]
        if (cells.length != rows * cols * 2) return null

        var board = Board(rows, cols)
        var i = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val a = cells[i]
                val b = cells[i + 1]
                i += 2
                when {
                    a == '.' -> Unit
                    a == 'o' -> {
                        val color = charToColor(b) ?: return null
                        board = board.place(CellPos(r, c), Tile.Receiver(color))
                    }
                    else -> {
                        val dir = charToDir(a) ?: return null
                        val tile = if (b == EGG_CHAR) Tile.egg(dir) else Tile.Shooter(dir, charToColor(b) ?: return null)
                        board = board.place(CellPos(r, c), tile)
                    }
                }
            }
        }
        return board
    }
}
