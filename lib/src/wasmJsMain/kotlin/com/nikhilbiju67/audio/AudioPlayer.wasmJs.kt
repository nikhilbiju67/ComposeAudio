package com.nikhilbiju67.audio

import kotlinx.browser.document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.w3c.dom.HTMLAudioElement

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(
    private val onProgressCallback: (PlayerState) -> Unit,
    onReadyCallback: () -> Unit,
    onErrorCallback: (Exception) -> Unit,
    private val playerState: PlayerState,
    context: Any?,
) {
    private var currentPlayingResource: String? = null
    private val audioElement = document.createElement("audio") as HTMLAudioElement
    private var currentUrl: String? = null
    private val _playerState = MutableStateFlow(playerState)
    actual fun play(url: String) {
        if (url != currentUrl) {
            currentUrl = url
            audioElement.src = if (isLocalFile(url)) "file://$url" else url
        }
        _playerState.update {
            it.copy(
                isPlaying = true,
                currentPlayingResource = url,
            )
        }
        setupListeners()

        audioElement.play()

        onProgressCallback(_playerState.value)
    }

    private fun setupListeners() {
        audioElement.addEventListener("timeupdate", {

            _playerState.value = _playerState.value.copy(
                currentTime = audioElement.currentTime.toFloat(),
                duration = audioElement.duration.toFloat(),
                isPlaying = _playerState.value.isPlaying,
                isBuffering = false,
                currentPlayingResource = currentUrl
            )

            onProgressCallback(_playerState.value)

        })

        audioElement.addEventListener("ended", {
            currentUrl = null
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                currentTime = 0f,
                duration = 0f,
                currentPlayingResource = currentUrl
            )
            onProgressCallback(_playerState.value)
        })

        audioElement.addEventListener("error", {
            playerState.isPlaying = false
            // Handle playback error if needed
        })
    }

    actual fun pause() {
        audioElement.pause()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isBuffering = false
        )

        onProgressCallback(_playerState.value)
    }


    actual fun cleanUp() {
        audioElement.pause()
        audioElement.src = ""
        currentUrl = null
    }

    private fun isLocalFile(path: String): Boolean {
        // This implementation assumes you have a way to determine if a path is local
        return path.startsWith("/") || path.startsWith("file://")
    }

    actual fun playerState(): PlayerState {
        return playerState
    }

    actual fun seek(position: Float) {
        audioElement.currentTime = position.toDouble()
    }

}