package com.nikhilbiju67.audio

import androidx.compose.runtime.Stable

@Stable
data class PlayerState(
    var isPlaying: Boolean = false,
    var isBuffering: Boolean = false,
    var currentTime: Int = 0,
    var duration: Int = 0,

) {
    val progress = currentTime.toFloat() / duration.toFloat()
}