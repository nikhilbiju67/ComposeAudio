package com.nikhilbiju67.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable

@Composable
actual fun AudioProvider(
    audioUpdates: AudioUpdates,
    composable: @Composable (AudioPlayer) -> Unit
) {
    val audioPlayer =
        AudioPlayer(onProgressCallback = {
            audioUpdates.onProgressUpdate(it)
        }, context = null, playerState = PlayerState())
    composable(audioPlayer)
}