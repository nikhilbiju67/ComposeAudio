package com.nikhilbiju67.audio

import kotlinx.coroutines.flow.MutableStateFlow
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(
    private val onProgressCallback: (PlayerState) -> Unit,
    val onReadyCallback: () -> Unit,
    val onErrorCallback: (Exception) -> Unit,
    val playerState: PlayerState,
    context: Any?,
) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingResource: String? = null

    init {
        initializeMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        NativeDiscovery().discover()

        val playerComponent = EmbeddedMediaPlayerComponent()
        mediaPlayer = playerComponent.mediaPlayer()
        (mediaPlayer as EmbeddedMediaPlayer?)?.videoSurface()?.set(null) // No video output needed
        mediaPlayer?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
                playerState.currentTime = (newTime / 1000).toFloat()
                playerState.duration =
                    ((mediaPlayer?.media()?.info()?.duration() ?: 0) / 1000).toFloat()
                onProgressCallback(
                    PlayerState(
                        isPlaying = playerState.isPlaying,
                        currentTime = playerState.currentTime,
                        duration = playerState.duration,
                        currentPlayingResource = currentPlayingResource
                    )
                )
//                _mediaStatus.update {
//                    it.copy(
//                        audioProgress = progressPercentage / 100,
//                        isPlaying = playerState.isPlaying,
//                        currentlyPlaying = currentlyPlaying,
//                    )
//                }


            }

            override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
                onReadyCallback()
                super.mediaPlayerReady(mediaPlayer)

            }

            override fun finished(mediaPlayer: MediaPlayer?) {
                playerState.isPlaying = false
                playerState.currentTime = 0f
                onProgressCallback(
                    PlayerState(
                        isPlaying = false,
                        currentTime = 0f,
                        duration = playerState.duration,
                        currentPlayingResource = null
                    )
                )
            }

            override fun error(mediaPlayer: MediaPlayer?) {
                playerState.isPlaying = false
                onErrorCallback(Exception("Error playing media"))
                onProgressCallback(
                    PlayerState(
                        isPlaying = false,
                        currentTime = 0f,
                        duration = playerState.duration,
                        currentPlayingResource = null
                    )
                )
            }
        })
    }

    actual fun play(url: String) {
        println("Attempting to play: $url")
        if (mediaPlayer?.media()?.play(url) == true)
            currentPlayingResource = url
        onProgressCallback(
            playerState.copy(
                isPlaying = true,
                currentPlayingResource = url
            )
        )

        println("Playback started successfully.")
        playerState.isPlaying = true
        mediaPlayer?.audio()?.setVolume(100) // Set volume to 100%
    }


    actual fun pause() {
        mediaPlayer?.controls()?.pause()
        playerState.isPlaying = false
    }


    actual fun playerState(): PlayerState {
        return playerState
    }

    actual fun cleanUp() {
        mediaPlayer?.controls()?.stop()
        mediaPlayer?.release()

        playerState.isPlaying = false
        playerState.currentTime = 0f
        playerState.duration = 0f
    }

    actual fun seek(position: Float) {
        mediaPlayer?.controls()?.setPosition(position / 100)
    }
}