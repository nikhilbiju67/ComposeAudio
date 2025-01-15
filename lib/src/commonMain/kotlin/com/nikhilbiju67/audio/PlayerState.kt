package com.nikhilbiju67.audio

import androidx.compose.runtime.Stable

@Stable
data class PlayerState(
    var isPlaying: Boolean = false,
    var isBuffering: Boolean = false,
    var currentTime: Float = 0f,
    var duration: Float = 0f,
    var currentPlayingResource:String?=null

) {
    val progress = currentTime.toFloat() / duration.toFloat()
}