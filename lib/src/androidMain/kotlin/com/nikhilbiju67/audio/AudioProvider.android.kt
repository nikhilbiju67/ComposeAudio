package com.nikhilbiju67.audio

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun AudioProvider(
    audioUpdates: AudioUpdates,
    composable: @Composable (AudioPlayer) -> Unit
) {
    val context = LocalContext.current
    val audioPlayer =
        AudioPlayer(
            onProgressCallback = {
                audioUpdates.onProgressUpdate(it)
            }, context = context,
            onReadyCallback = {},
            onErrorCallback = {},
            playerState = PlayerState()
        )
    composable(audioPlayer)
}