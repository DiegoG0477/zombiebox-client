package io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.TracksRepository

/** Session ownership and generations prevent late track results crossing playback changes. */
class TracksViewModel(
    private val repository: TracksRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    @Volatile private var generation = 0
    @Volatile private var closed = false
    private var session = ""
    private var timeline = SubtitleTimeline(emptyList())
    private var subtitleGeneration = 0
    var subtitleId: Int? = null
        private set

    fun attach(id: String) {
        generation++
        subtitleGeneration++
        session = id
        subtitleId = null
        timeline = SubtitleTimeline(emptyList())
    }

    fun inventory(done: (MediaTracks) -> Unit, failed: (Exception) -> Unit) {
        if (closed || session.isEmpty()) return
        val request = generation
        val id = session
        execute {
            try {
                val value = repository.inventory(id)
                deliver { if (!closed && request == generation) done(value) }
            } catch (error: Exception) {
                deliver { if (!closed && request == generation) failed(error) }
            }
        }
    }

    fun subtitles(track: Int?, changed: () -> Unit, failed: (Exception) -> Unit) {
        if (closed || session.isEmpty()) return
        val subtitleRequest = ++subtitleGeneration
        val request = generation
        val id = session
        subtitleId = null
        timeline = SubtitleTimeline(emptyList())
        changed()
        if (track == null) return
        execute {
            try {
                val value = SubtitleTimeline(repository.subtitles(id, track))
                deliver {
                    if (!closed && request == generation && subtitleRequest == subtitleGeneration) {
                        subtitleId = track
                        timeline = value
                        changed()
                    }
                }
            } catch (error: Exception) {
                deliver {
                    if (!closed && request == generation && subtitleRequest == subtitleGeneration)
                        failed(error)
                }
            }
        }
    }

    fun audio(
        track: Int,
        positionMs: Int,
        done: (PlaybackPlan) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        if (closed || session.isEmpty()) return
        val request = ++generation
        val id = session
        execute {
            try {
                val plan = repository.audio(id, track, positionMs)
                deliver {
                    if (closed || request != generation) execute { release(plan.sessionId) }
                    else {
                        session = plan.sessionId
                        // Cancel the old stream before starting the replacement media job.
                        execute {
                            release(id)
                            deliver {
                                if (!closed && request == generation) done(plan)
                                else execute { release(plan.sessionId) }
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                deliver { if (!closed && request == generation) failed(error) }
            }
        }
    }

    fun textAt(positionMs: Int) = timeline.textAt(positionMs)

    private fun release(id: String) {
        try {
            repository.release(id)
        } catch (_: Exception) {}
    }

    fun close() {
        attach("")
        closed = true
    }
}
