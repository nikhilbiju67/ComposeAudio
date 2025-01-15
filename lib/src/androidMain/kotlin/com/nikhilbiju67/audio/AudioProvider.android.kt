package com.nikhilbiju67.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
            onReadyCallback = {
                audioUpdates.onReady()
            },
            onErrorCallback = {
                audioUpdates.onError(it)
            },
            playerState = PlayerState()
        )
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.cleanUp()
        }
    }
    composable(audioPlayer)
}