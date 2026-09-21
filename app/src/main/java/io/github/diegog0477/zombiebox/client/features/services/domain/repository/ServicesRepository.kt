package io.github.diegog0477.zombiebox.client.features.services.domain.repository

import io.github.diegog0477.zombiebox.client.features.services.domain.model.AuthorizationPrompt
import io.github.diegog0477.zombiebox.client.features.services.domain.model.ServicesSnapshot

interface ServicesRepository {
    fun load(): ServicesSnapshot

    fun authorize(operatorCode: String): AuthorizationPrompt

    fun command(action: String, operatorCode: String)
}
