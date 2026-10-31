package com.laserarrows.app.engine

data class LaserResult(
    val hit: CellPos?,
    val path: List<CellPos>,
    /** 区間ごとの走る子の種類。なぞのたまごがまだ何にも化けていない区間はnull。 */
    val colorTrail: List<LaserColor?>,
    /** 命中までに、違う種類の矢印に当たって向きを変えられた(道案内された)回数。 */
    val redirects: Int = 0,
)
