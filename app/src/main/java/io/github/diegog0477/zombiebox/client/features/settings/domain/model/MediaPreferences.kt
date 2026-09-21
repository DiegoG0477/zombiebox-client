package io.github.diegog0477.zombiebox.client.features.settings.domain.model

data class MediaPreferences(
    val audioLanguages: List<String> = listOf("en", "es"),
    val subtitleLanguages: List<String> = listOf("en", "es"),
    val subtitleMode: String = "auto",
) {
    fun valid(): Boolean =
        subtitleMode in listOf("off", "forced", "auto", "always") &&
            listOf(audioLanguages, subtitleLanguages).all { languages ->
                languages.size <= 8 &&
                    languages.all { it.matches(Regex("[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})?")) }
            }
}
