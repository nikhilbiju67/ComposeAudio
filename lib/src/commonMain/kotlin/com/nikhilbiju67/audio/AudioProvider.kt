package com.nikhilbiju67.audio

import androidx.compose.runtime.Composable

@Composable
expect fun AudioProvider( audioUpdates: AudioUpdates,composable: @Composable (AudioPlayer) -> Unit)

interface AudioUpdates {
    fun onProgressUpdate(playerState: PlayerState)
    fun onReady()
    fun onError(exception: Exception)
}