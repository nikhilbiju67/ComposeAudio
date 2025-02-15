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
import platform.CoreMedia.CMTimeCompare
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AudioPlayer actual constructor(
    /**
     * Callback invoked periodically with the player's current state.
     */
    private val onProgressCallback: (PlayerState) -> Unit,

    /**
     * Callback invoked when the player is ready to play.
     */
    val onReadyCallback: () -> Unit,

    /**
     * Callback invoked when an error occurs in the player.
     */
    val onErrorCallback: (Exception) -> Unit,

    /**
     * Platform-specific context, nullable.
     */
    context: Any?
) : NSObject() {

    /**
     * AVPlayer instance for managing audio playback.
     */
    private val avAudioPlayer: AVPlayer = AVPlayer()

    /**
     * Holds the URL of the resource currently being played.
     */
    private var currentPlayingResource: String? = null

    /**
     * Observer to track playback time progress.
     */
    private lateinit var timeObserver: Any

    /**
     * Observer token for the playback-end notification.
     */
    private var playbackEndObserver: NSObjectProtocol? = null

    /**
     * Mutable state flow to track player state updates.
     */
    private val _playerState = MutableStateFlow(PlayerState())

    /**
     * Observer function to track playback progress.
     *
     * Updates the current time and duration in the player state and triggers the [onProgressCallback].
     */
    @OptIn(ExperimentalForeignApi::class)
    private val observer: (CValue<CMTime>) -> Unit = { time: CValue<CMTime> ->
        // Retrieve the current playback time in seconds.
        val currentTimeInSeconds = CMTimeGetSeconds(time)
        // Retrieve the total duration of the current media item.
        val totalDurationInSeconds =
            avAudioPlayer.currentItem?.duration?.let { CMTimeGetSeconds(it) } ?: Double.NaN

        // Only update if valid values are present.
        if (!currentTimeInSeconds.isNaN() &&
            !totalDurationInSeconds.isNaN() &&
            totalDurationInSeconds > 0
        ) {
            _playerState.value = _playerState.value.copy(
                currentTime = currentTimeInSeconds.toFloat(),
                duration = totalDurationInSeconds.toFloat(),
                isPlaying = _playerState.value.isPlaying,
                isBuffering = _playerState.value.isBuffering,
                currentPlayingResource = currentPlayingResource
            )
            onProgressCallback(_playerState.value)
        } else {
            println("Skipping progress update due to invalid time values.")
        }
    }

    init {
        // Configure the audio session for playback.
        setUpAudioSession()
        // Initialize the player state based on the AVPlayer's status.
        _playerState.value = _playerState.value.copy(
            isPlaying = (avAudioPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying)
        )
    }

    /**
     * Starts playback of the audio from the provided [url].
     *
     * @param url The URL string of the audio resource.
     *
     * @throws IllegalArgumentException if the URL is invalid.
     * @throws IllegalStateException if the media duration is invalid.
     */
    @OptIn(ExperimentalForeignApi::class)
    actual fun play(url: String) {
        // Check if we're trying to resume the same resource.
        if (currentPlayingResource == url && avAudioPlayer.currentItem != null &&
            avAudioPlayer.timeControlStatus != AVPlayerTimeControlStatusPlaying
        ) {
            // Resume playback without restarting.
            avAudioPlayer.play()
            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                isBuffering = false
            )
            onProgressCallback(_playerState.value)
            return
        }

        // For a new resource, stop the previous playback.
        currentPlayingResource = url
        _playerState.value = _playerState.value.copy(
            isBuffering = true,
            currentPlayingResource = url
        )
        stop()
        startTimeObserver()

        try {
            val nsUrl = NSURL.URLWithString(url) ?: throw IllegalArgumentException("Invalid URL")
            val playItem = AVPlayerItem(uRL = nsUrl)

            if (CMTimeCompare(
                    playItem.duration,
                    CMTimeMakeWithSeconds(0.0, NSEC_PER_SEC.toInt())
                ) == 0
            ) {
                throw IllegalStateException("Invalid media duration.")
            }

            avAudioPlayer.replaceCurrentItemWithPlayerItem(playItem)
            avAudioPlayer.play()
            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                isBuffering = false
            )
            onReadyCallback()
        } catch (e: Exception) {
            onErrorCallback(e)
            if (::timeObserver.isInitialized) {
                avAudioPlayer.removeTimeObserver(timeObserver)
            }
        }
    }


    /**
     * Pauses the current audio playback.
     */
    actual fun pause() {
        avAudioPlayer.pause()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isBuffering = false,
            currentPlayingResource = currentPlayingResource
        )
        onProgressCallback(_playerState.value)
    }

    /**
     * Configures the audio session for playback.
     */
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

    /**
     * Starts observing the playback progress at regular intervals.
     *
     * Also sets up a notification observer to detect when playback finishes.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun startTimeObserver() {
        val interval = CMTimeMakeWithSeconds(0.1, NSEC_PER_SEC.toInt())
        timeObserver = avAudioPlayer.addPeriodicTimeObserverForInterval(interval, null, observer)

        playbackEndObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = avAudioPlayer.currentItem,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ ->
                _playerState.value = _playerState.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    currentPlayingResource = currentPlayingResource
                )
                onProgressCallback(_playerState.value)
            }
        )
    }

    /**
     * Stops playback, removes observers, and resets the player state.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun stop() {
        // Remove the time observer if it was set.
        if (::timeObserver.isInitialized) {
            avAudioPlayer.removeTimeObserver(timeObserver)
        }
        // Remove the playback end observer.
        playbackEndObserver?.let {
            NSNotificationCenter.defaultCenter.removeObserver(it)
            playbackEndObserver = null
        }

        avAudioPlayer.pause()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isBuffering = false,
            currentTime = 0f,
            currentPlayingResource = null
        )
        onProgressCallback(_playerState.value)

        // Seek to the beginning of the current item.
        avAudioPlayer.currentItem?.seekToTime(CMTimeMakeWithSeconds(0.0, NSEC_PER_SEC.toInt()))
    }

    /**
     * Cleans up the player resources.
     */
    actual fun cleanUp() {
        stop()
    }

    /**
     * Seeks to a specific position within the current media.
     *
     * @param position The desired position in seconds.
     */
    @OptIn(ExperimentalForeignApi::class)
    actual fun seek(position: Float) {
        val time = CMTimeMakeWithSeconds(position.toDouble(), NSEC_PER_SEC.toInt())
        avAudioPlayer.seekToTime(time)
    }
}

