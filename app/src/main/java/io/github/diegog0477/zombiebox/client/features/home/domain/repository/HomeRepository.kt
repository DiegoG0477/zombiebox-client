package io.github.diegog0477.zombiebox.client.features.home.domain.repository

import io.github.diegog0477.zombiebox.client.features.home.domain.model.HomeScope
import io.github.diegog0477.zombiebox.client.features.home.domain.model.HomeSnapshot

interface HomeRepository {
    fun load(scope: HomeScope): HomeSnapshot
}
