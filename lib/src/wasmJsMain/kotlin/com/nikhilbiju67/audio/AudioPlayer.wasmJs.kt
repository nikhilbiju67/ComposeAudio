package com.nikhilbiju67.audio

import kotlinx.browser.document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.w3c.dom.HTMLAudioElement

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(
    private val onProgressCallback: (PlayerState) -> Unit,
    private val onReadyCallback: () -> Unit,
    private val onErrorCallback: (Exception) -> Unit,
    context: Any?
) {
    private val _playerState = MutableStateFlow(PlayerState())
    private var currentUrl: String? = null
    private val audioElement = document.createElement("audio") as HTMLAudioElement

    actual fun play(url: String) {
        if (url != currentUrl) {
            currentUrl = url
            audioElement.src = if (isLocalFile(url)) "file://$url" else url
        }
        _playerState.update { state ->
            state.copy(
                isPlaying = true,
                currentPlayingResource = url
            )
        }
        setupListeners()

        // Start playback and handle potential promise rejection.
        audioElement.play().catch { exception ->
            onErrorCallback(Exception("Audio playback error: $exception"))
            null
        }

        onProgressCallback(_playerState.value)
    }

    private fun setupListeners() {
        // Trigger the onReady callback when the audio is ready to play.
        audioElement.addEventListener("canplay", {
            onReadyCallback()
        })

        audioElement.addEventListener("timeupdate", {
            _playerState.update { state ->
                state.copy(
                    currentTime = audioElement.currentTime.toFloat(),
                    duration = audioElement.duration.toFloat(),
                    isBuffering = false,
                    currentPlayingResource = currentUrl
                )
            }
            onProgressCallback(_playerState.value)
        })

        audioElement.addEventListener("ended", {
            currentUrl = null
            _playerState.update { state ->
                state.copy(
                    isPlaying = false,
                    currentTime = 0f,
                    duration = 0f,
                    currentPlayingResource = currentUrl
                )
            }
            onProgressCallback(_playerState.value)
        })

        audioElement.addEventListener("error", {
            _playerState.update { state ->
                state.copy(isPlaying = false)
            }
            onErrorCallback(Exception("Audio playback error"))
        })
    }

    actual fun pause() {
        audioElement.pause()
        _playerState.update { state ->
            state.copy(
                isPlaying = false,
                isBuffering = false
            )
        }
        onProgressCallback(_playerState.value)
    }

    actual fun cleanUp() {
        audioElement.pause()
        audioElement.src = ""
        currentUrl = null
    }

    private fun isLocalFile(path: String): Boolean {
        // Determines if the path is local.
        return path.startsWith("/") || path.startsWith("file://")
    }

    actual fun seek(position: Float) {
        audioElement.currentTime = position.toDouble()
    }
}
