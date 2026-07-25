package com.laserarrows.app.engine

data class LaserResult(
    val hit: CellPos?,
    val path: List<CellPos>,
    val colorTrail: List<LaserColor>,
)
