package com.nikhilbiju67.audio

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
        }, onErrorCallback = {
            audioUpdates.onError(it)
        }, onReadyCallback = {
            audioUpdates.onReady()
        }, context = null)
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.cleanUp()
        }
    }
    composable(audioPlayer)
}