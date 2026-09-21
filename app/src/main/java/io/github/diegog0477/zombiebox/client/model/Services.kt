package io.github.diegog0477.zombiebox.client.model

data class Integration(val id: String, val state: String)

data class MusicStatus(val state: String, val title: String, val artist: String)

data class AuthorizationPrompt(val waiting: Boolean, val url: String, val code: String)

data class ServicesSnapshot(val integrations: List<Integration>, val music: MusicStatus?)

interface ServicesRepository {
    fun load(): ServicesSnapshot

    fun authorize(operatorCode: String): AuthorizationPrompt

    fun command(action: String, operatorCode: String)
}
