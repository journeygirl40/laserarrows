package com.laserarrows.app.engine

sealed class Tile {
    abstract val color: LaserColor

    data class Shooter(val dir: Direction, override val color: LaserColor) : Tile()
    data class Receiver(override val color: LaserColor) : Tile()
}
