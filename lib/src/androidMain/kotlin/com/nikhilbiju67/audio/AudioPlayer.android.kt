package com.nikhilbiju67.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/// A multiplatform AudioPlayer for Android using ExoPlayer.
/// This class handles audio playback, including play, pause, seek, and clean-up.
/// It also provides real-time progress updates through a callback function.
///
/// @param onProgressCallback A lambda invoked every time the player's progress is updated.
/// @param onReadyCallback A lambda invoked when the player is ready to start playback.
/// @param onErrorCallback A lambda invoked when an error occurs during playback.
/// @param context The Android context used to build the ExoPlayer instance.
actual class AudioPlayer actual constructor(
    private val onProgressCallback: (PlayerState) -> Unit,
    private val onReadyCallback: () -> Unit,
    private val onErrorCallback: (Exception) -> Unit,
    context: Any?
) {

    // ExoPlayer instance for audio playback.
    private var mediaPlayer: ExoPlayer = ExoPlayer.Builder(context as Context).build()

    // Internal MutableStateFlow to manage player state updates.
    private val _playerState = MutableStateFlow(PlayerState())

    // Expose the player state flow as a read-only state flow for external subscribers.
    private val playerState = _playerState.asStateFlow()

    // A reference to the file or URL currently being played.
    private var currentPlayingResource: String? = null

    // A Job that handles continuous progress updates while audio is playing.
    private var progressJob: Job? = null

    // A scope for launching coroutines related to the player.
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ExoPlayer event listener to handle state changes and errors.
    private val listener = object : Player.Listener {

        /// Called when the playback state changes (e.g., buffering, ready, ended).
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    // Player is idle. Usually happens just before preparing or after release.
                }

                Player.STATE_BUFFERING -> {
                    // Update our state to reflect buffering.
                    _playerState.update { it.copy(isBuffering = true) }
                    updateMediaStatus()
                }

                Player.STATE_READY -> {
                    // Player is ready to play. Notify that we're set.
                    onReadyCallback()
                    _playerState.update {
                        it.copy(
                            isBuffering = false,
                            // Duration is in milliseconds, convert to seconds.
                            duration = (mediaPlayer.duration / 1000).toFloat()
                        )
                    }
                    updateMediaStatus()
                }

                Player.STATE_ENDED -> {
                    // Playback has ended for the current item. You could trigger a callback here if needed.
                    // For now, we’ll simply stop progress updates.
                    stopProgressUpdates()
                }
            }
        }

        /// Called when the player's play/pause state changes.
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
            updateMediaStatus()

            // Manage the job that updates progress in real time.
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
        }

        /// Called when the player encounters an error.
        override fun onPlayerError(error: PlaybackException) {
            // Send the error via callback.
            onErrorCallback(error)
        }
    }

    init {
        // Attach the listener to the ExoPlayer instance.
        mediaPlayer.addListener(listener)
    }

    /// Starts playback of an audio resource from a URL or local file path.
    /// @param url The URL or file path of the audio to play.
    actual fun play(url: String) {
        currentPlayingResource = url
        val mediaItem = if (isLocalFile(url)) {
            // Local file path must be prefixed with "file://"
            MediaItem.fromUri("file://$url")
        } else {
            MediaItem.fromUri(url)
        }

        // Clear previously set media items if necessary, then set the new one.
        mediaPlayer.setMediaItem(mediaItem)
        mediaPlayer.prepare()
        mediaPlayer.play()

        // Update our internal state to reflect the new resource.
        // Duration may still be 0 until the player is fully ready (handled in the listener).
        _playerState.update {
            it.copy(
                currentPlayingResource = url,
                duration = (mediaPlayer.duration / 1000).toFloat()
            )
        }
        updateMediaStatus()
    }

    /// Pauses playback if something is currently playing.
    actual fun pause() {
        mediaPlayer.pause()
        _playerState.update { it.copy(isPlaying = false) }
        updateMediaStatus()
    }

    /// Cleans up resources by stopping and releasing the ExoPlayer instance.
    actual fun cleanUp() {
        mediaPlayer.stop()
        mediaPlayer.release()
        mediaPlayer.removeListener(listener)
        currentPlayingResource = null
        stopProgressUpdates()
    }

    /// Begins continuous progress updates on a fixed interval while audio is playing.
    private fun startProgressUpdates() {
        stopProgressUpdates()  // Ensure we don't create multiple jobs.
        progressJob = coroutineScope.launch {
            while (_playerState.value.isPlaying) {
                val currentPos = mediaPlayer.currentPosition.toFloat()
                val totalDuration =
                    mediaPlayer.duration.toFloat().coerceAtLeast(1f) // Avoid division by zero
                currentPos / totalDuration

                _playerState.update {
                    it.copy(
                        currentTime = currentPos / 1000, // Convert milliseconds to seconds
                    )
                }

                onProgressCallback(_playerState.value)
                delay(100)  // Update progress ~10 times per second.
            }
        }
    }

    /// Stops the continuous progress updates job, if it exists.
    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    /// Checks if the provided path corresponds to an existing local file.
    private fun isLocalFile(path: String): Boolean {
        return File(path).exists()
    }

    /// Updates the current playback time and triggers the onProgressCallback.
    private fun updateMediaStatus() {
        val currentPos = mediaPlayer.currentPosition / 1000
        _playerState.update { it.copy(currentTime = currentPos.toFloat()) }
        onProgressCallback(_playerState.value)
    }

    /// Seeks to a specific position (in seconds) within the current media.
    /// @param position The desired position, in seconds.
    actual fun seek(position: Float) {
        mediaPlayer.seekTo((position * 1000).toLong())
    }

}
