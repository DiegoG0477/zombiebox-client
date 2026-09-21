package io.github.diegog0477.zombiebox.client.features.home.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.home.domain.model.HomeScope
import io.github.diegog0477.zombiebox.client.features.home.domain.model.HomeSnapshot

data class HomeState(
    val scope: HomeScope = HomeScope(),
    val snapshot: HomeSnapshot = HomeSnapshot(),
    val loading: Boolean = false,
    val failure: Exception? = null,
)
