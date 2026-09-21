package io.github.diegog0477.zombiebox.client.features.services.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.services.domain.model.AuthorizationPrompt
import io.github.diegog0477.zombiebox.client.features.services.domain.model.ServicesSnapshot

data class ServicesState(
    val snapshot: ServicesSnapshot = ServicesSnapshot(emptyList(), null),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val prompt: AuthorizationPrompt? = null,
)
