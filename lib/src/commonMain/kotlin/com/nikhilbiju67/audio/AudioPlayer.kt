package com.nikhilbiju67.audio

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class AudioPlayer(
    onProgressCallback: (PlayerState) -> Unit,
    onReadyCallback: () -> Unit,
    onErrorCallback: (Exception) -> Unit,
    playerState: PlayerState,
    context: Any?,
) {
    fun pause()
    fun play(url: String)
    fun playerState(): PlayerState
    fun cleanUp()

// Flow to observe media status
}
