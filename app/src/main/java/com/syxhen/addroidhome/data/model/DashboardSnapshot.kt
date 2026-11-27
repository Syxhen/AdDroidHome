package com.syxhen.addroidhome.data.model

import com.syxhen.addroidhome.data.network.model.ServerStatusDto
import com.syxhen.addroidhome.data.network.model.StatsDto

data class DashboardSnapshot(
    val status: ServerStatusDto,
    val stats: StatsDto,
)
