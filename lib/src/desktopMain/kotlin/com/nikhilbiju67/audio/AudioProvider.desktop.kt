package com.nikhilbiju67.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
actual fun AudioProvider(
    audioUpdates: AudioUpdates,
    composable: @Composable (AudioPlayer) -> Unit
) {
    val audioPlayer =
        AudioPlayer(onProgressCallback = {
            audioUpdates.onProgressUpdate(it)
        }, context = null, playerState = PlayerState(),onReadyCallback = {
            audioUpdates.onReady()
        }, onErrorCallback = {
            audioUpdates.onError(it)
        })
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.cleanUp()
        }
    }
    composable(audioPlayer)
}