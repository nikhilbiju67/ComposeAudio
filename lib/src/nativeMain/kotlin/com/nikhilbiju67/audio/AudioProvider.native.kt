package com.nikhilbiju67.audio

import androidx.compose.runtime.Composable

@Composable
actual fun AudioProvider(composable: @Composable (AudioPlayer) -> Unit) {
}