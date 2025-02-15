package com.nikhilbiju67.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
/// This class is responsible for audio playback using VLCJ on desktop platforms.
actual class AudioPlayer actual constructor(
    /// Callback invoked with the current [PlayerState] whenever progress updates.
    private val onProgressCallback: (PlayerState) -> Unit,
    /// Callback invoked when the media is ready to play.
    val onReadyCallback: () -> Unit,
    /// Callback invoked if there's an error during playback.
    val onErrorCallback: (Exception) -> Unit,
    /// Optional context (not used on desktop).
    context: Any?,
) {
    /// Holds the current state of the player in a [MutableStateFlow].
    private val _playerState = MutableStateFlow(PlayerState())

    /// VLCJ media player reference.
    private var mediaPlayer: MediaPlayer? = null

    init {
        initializeMediaPlayer()
    }

    /// Sets up the VLCJ media player, including discovery and event listeners.
    private fun initializeMediaPlayer() {
        /// Discover VLC native libraries on the system.
        NativeDiscovery().discover()

        /// Create the embedded media player component.
        val playerComponent = EmbeddedMediaPlayerComponent()
        mediaPlayer = playerComponent.mediaPlayer()

        /// No video output needed for audio-only playback.
        (mediaPlayer as EmbeddedMediaPlayer?)?.videoSurface()?.set(null)

        /// Add event listeners to handle playback events.
        mediaPlayer?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {

            override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                val durationSeconds =
                    (mediaPlayer?.media()?.info()?.duration() ?: 0) / 1000f

                // Update the player's state flow with current time and duration.
                _playerState.update {
                    it.copy(
                        currentTime = newTime / 1000f,
                        duration = durationSeconds
                    )
                }

                // Notify about progress updates.
                onProgressCallback(_playerState.value)
            }

            override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
                // Notify that the media is ready.
                onReadyCallback()
                super.mediaPlayerReady(mediaPlayer)
            }

            override fun finished(mediaPlayer: MediaPlayer?) {
                // When playback finishes, reset state.
                _playerState.update {
                    it.copy(
                        isPlaying = false,
                        currentTime = 0f,
                        currentPlayingResource = null
                    )
                }

                // Notify listeners that playback finished.
                onProgressCallback(_playerState.value)
            }

            override fun error(mediaPlayer: MediaPlayer?) {
                // On error, update state and invoke error callback.
                _playerState.update {
                    it.copy(
                        isPlaying = false,
                        currentTime = 0f,
                        currentPlayingResource = null
                    )
                }
                onErrorCallback(Exception("Error playing media"))
                onProgressCallback(_playerState.value)
            }
        })
    }

    /// Starts playback of the given URL.
    actual fun play(url: String) {
        println("Attempting to play: $url")

        // Try to play the media from the URL.
        val didPlay = mediaPlayer?.media()?.play(url) == true
        if (didPlay) {
            // Update the state to reflect that we're now playing this resource.
            _playerState.update {
                it.copy(
                    isPlaying = true,
                    currentPlayingResource = url
                )
            }
            onProgressCallback(_playerState.value)

            println("Playback started successfully.")

            // Set volume to 100%.
            mediaPlayer?.audio()?.setVolume(100)
        } else {
            // Handle case where media failed to play.
            onErrorCallback(Exception("Failed to start playback for: $url"))
        }
    }

    /// Pauses the current playback.
    actual fun pause() {
        mediaPlayer?.controls()?.pause()
        _playerState.update { it.copy(isPlaying = false) }
        onProgressCallback(_playerState.value)
    }

    /// Cleans up the media player resources.
    actual fun cleanUp() {
        mediaPlayer?.controls()?.stop()
        mediaPlayer?.release()

        // Reset player state.
        _playerState.update {
            it.copy(
                isPlaying = false,
                currentTime = 0f,
                duration = 0f,
                currentPlayingResource = null
            )
        }
        onProgressCallback(_playerState.value)
    }

    /// Seeks to the given position (0f..100f range for percentage).
    actual fun seek(position: Float) {
        // VLCJ expects a float between 0.0 and 1.0 for setPosition().
        mediaPlayer?.controls()?.setPosition(position / 100)
    }
}
