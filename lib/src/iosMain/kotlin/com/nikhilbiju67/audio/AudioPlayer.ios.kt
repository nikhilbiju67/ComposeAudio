package com.nikhilbiju67.audio

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSObject

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(
    /// Callback invoked periodically with the player's current state.
    private val onProgressCallback: (PlayerState) -> Unit,

    /// Callback invoked when the player is ready to play.
    val onReadyCallback: () -> Unit,

    /// Callback invoked when an error occurs in the player.
    val onErrorCallback: (Exception) -> Unit,

    /// The initial state of the player.
    private val playerState: PlayerState,

    /// Platform-specific context, nullable.
    context: Any?
) : NSObject() {

    /// AVPlayer instance for managing audio playback.
    private val avAudioPlayer: AVPlayer = AVPlayer()
    private var currentPlayingResource: String? = null

    /// Observer to track playback time progress.
    private lateinit var timeObserver: Any

    /// Mutable state flow to track player state updates.
    private val _playerState = MutableStateFlow(playerState)

    /// Observer function to track playback progress.
    @OptIn(ExperimentalForeignApi::class)
    private val observer: (CValue<CMTime>) -> Unit = { time: CValue<CMTime> ->
        // Get current and total playback time
        val currentTimeInSeconds = CMTimeGetSeconds(time)
        val totalDurationInSeconds =
            avAudioPlayer.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: Double.NaN

        // Validate currentTime and totalDuration
        if (!currentTimeInSeconds.isNaN() && !totalDurationInSeconds.isNaN() && totalDurationInSeconds > 0) {
            val progress = (currentTimeInSeconds / totalDurationInSeconds).toFloat()

            _playerState.value = _playerState.value.copy(
                currentTime = currentTimeInSeconds.toFloat(),
                duration = totalDurationInSeconds.toFloat(),
                isPlaying = playerState.isPlaying,
                isBuffering = playerState.isBuffering,
                currentPlayingResource = currentPlayingResource

            )
            onProgressCallback(_playerState.value)
        } else {
            println("Skipping progress update due to invalid time values.")
        }
    }

    init {
        setUpAudioSession()
        playerState.isPlaying = avAudioPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying
    }

    /// Starts playback of the audio from the given [url].
    @OptIn(ExperimentalForeignApi::class)
    actual fun play(url: String) {
        playerState.isBuffering = true

        stop()
        startTimeObserver()

        try {
            val nsUrl = NSURL.URLWithString(url) ?: throw IllegalArgumentException("Invalid URL")
            val playItem = AVPlayerItem(uRL = nsUrl)

            if (playItem.duration == CMTimeMakeWithSeconds(0.0, NSEC_PER_SEC.toInt())) {
                throw IllegalStateException("Invalid media duration.")
            }

            avAudioPlayer.replaceCurrentItemWithPlayerItem(playItem)
            avAudioPlayer.play()
            playerState.isPlaying = true
        } catch (e: Exception) {
            onErrorCallback(e)
            if (::timeObserver.isInitialized) {
                avAudioPlayer.removeTimeObserver(timeObserver)
            }
            return
        }
    }

    /// Pauses the current audio playback.
    actual fun pause() {
        avAudioPlayer.pause()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isBuffering = false,
            currentPlayingResource = currentPlayingResource
        )
        onProgressCallback(_playerState.value)
    }

    /// Configures the audio session for playback.
    @OptIn(ExperimentalForeignApi::class)
    private fun setUpAudioSession() {
        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryPlayback, null)
            audioSession.setActive(true, null)
        } catch (e: Exception) {
            println("Error setting up audio session: ${e.message}")
            onErrorCallback(e)
        }
    }

    /// Starts observing the playback progress at regular intervals.
    @OptIn(ExperimentalForeignApi::class)
    private fun startTimeObserver() {
        val interval = CMTimeMakeWithSeconds(0.1, NSEC_PER_SEC.toInt())
        timeObserver = avAudioPlayer.addPeriodicTimeObserverForInterval(interval, null, observer)

        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = avAudioPlayer.currentItem,
            queue = NSOperationQueue.mainQueue,
            usingBlock = {
                _playerState.value = _playerState.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    currentPlayingResource = currentPlayingResource
                )
                onProgressCallback(_playerState.value)
            }
        )
    }

    /// Stops playback and resets the player state.
    @OptIn(ExperimentalForeignApi::class)
    private fun stop() {
        if (::timeObserver.isInitialized) {
            avAudioPlayer.removeTimeObserver(timeObserver)
        }

        avAudioPlayer.pause()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isBuffering = false,
            currentTime = 0f,
            currentPlayingResource = null
        )
        onProgressCallback(_playerState.value)

        avAudioPlayer.currentItem?.seekToTime(CMTimeMakeWithSeconds(0.0, NSEC_PER_SEC.toInt()))
    }

    /// Cleans up player resources.
    actual fun cleanUp() {
        stop()
    }

    /// Returns the current player state.
    actual fun playerState(): PlayerState {
        return _playerState.value
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun seek(position: Float) {
        val time = CMTimeMakeWithSeconds(position.toDouble(), NSEC_PER_SEC.toInt())
        avAudioPlayer.seekToTime(time)
    }
}

