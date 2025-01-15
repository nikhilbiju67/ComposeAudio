package com.nikhilbiju67.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

actual class AudioPlayer actual constructor(
    private val onProgressCallback: (PlayerState) -> Unit,
    onReadyCallback: () -> Unit,
    onErrorCallback: (Exception) -> Unit,
    val playerState: PlayerState,
    context: Any?,

    ) {
    private var mediaPlayer: ExoPlayer =
        ExoPlayer.Builder(context as Context).build()
    private val mediaItems = mutableListOf<MediaItem>()
    private var currentItemIndex = -1
    private val _playerState = MutableStateFlow(playerState)

    private var progressJob: Job? = null
    private  var currentPlayingResource: String? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {}
                Player.STATE_BUFFERING -> {
                    playerState.isBuffering = true
                    updateMediaStatus()
                }

                Player.STATE_READY -> {
                    onReadyCallback()
                    playerState.isBuffering = false
                    playerState.duration = (mediaPlayer.duration / 1000).toFloat()
                    updateMediaStatus()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            playerState.isPlaying = isPlaying
            updateMediaStatus()
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
        }
    }

    init {
        mediaPlayer.addListener(listener)
    }

    actual fun play(url: String) {
        if (url == null) {
            return
        }
        currentPlayingResource = url
        val mediaItem = if (isLocalFile(url)) {
            MediaItem.fromUri("file://$url")
        } else {
            MediaItem.fromUri(url)
        }

        mediaPlayer.setMediaItem(mediaItem)
        mediaPlayer.prepare()
        mediaPlayer.play()

        playerState.isPlaying = true
        playerState.duration = (mediaPlayer.duration / 1000).toFloat()
        updateMediaStatus()
    }

    actual fun pause() {
        mediaPlayer.pause()
        playerState.isPlaying = false
        updateMediaStatus()
    }

    actual fun cleanUp() {
        mediaPlayer.stop()
        mediaPlayer.release()
        currentPlayingResource = null
        mediaPlayer.removeListener(listener)
        stopProgressUpdates()
    }


    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (playerState.isPlaying) {
                val progressInPercentage =
                    mediaPlayer.currentPosition.toFloat() / mediaPlayer.duration.toFloat()
                _playerState.value = _playerState.value.copy(
                    currentTime = (mediaPlayer.currentPosition / 1000).toFloat(),
                    isPlaying = playerState.isPlaying,
                    isBuffering = playerState.isBuffering,
                    duration = playerState.duration
                )
                onProgressCallback(_playerState.value)
                delay(10)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun isLocalFile(path: String): Boolean {
        return File(path).exists()
    }

    actual fun playerState(): PlayerState {
        return playerState
    }

    private fun updateMediaStatus() {
        _playerState.value = _playerState.value.copy(
            currentTime = (mediaPlayer.currentPosition / 1000).toFloat(),
            isPlaying = playerState.isPlaying,
            isBuffering = playerState.isBuffering,
            duration = playerState.duration
        )
        onProgressCallback(playerState)
    }

    actual fun seek(position:Float) {
        mediaPlayer.seekTo(position.toLong() * 1000)
    }
}


