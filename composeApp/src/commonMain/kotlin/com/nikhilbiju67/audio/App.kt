package com.nikhilbiju67.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import composemultiplatformaudio.composeapp.generated.resources.Res
import composemultiplatformaudio.composeapp.generated.resources.compose_multiplatform
import composemultiplatformaudio.composeapp.generated.resources.pause_circle_24px
import composemultiplatformaudio.composeapp.generated.resources.play_circle_24px
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

@Composable
@Preview
fun App() {
    MaterialTheme(
    ) {
        var showAudioPlayer by remember { mutableStateOf(false) }

        AudioPlayerView()
    }
}

@Composable
fun AudioPlayerView() {
    var audioPlayerState: MutableState<PlayerState?> = remember { mutableStateOf(null) }

    AudioProvider(
        audioUpdates = object : AudioUpdates {
            override fun onProgressUpdate(playerState: PlayerState) {
                audioPlayerState.value = playerState
                println("Player state: $playerState")
            }

            override fun onReady() {

            }

            override fun onError(exception: Exception) {

            }

        }
    ) { audioPlayer ->
        val animatedProgress by animateFloatAsState(
            targetValue = (audioPlayerState.value?.progress
                ?.takeIf { it.isFinite() && it >= 0f } ?: 0f),
            animationSpec = tween(
                durationMillis = 1000, // Animation duration
                delayMillis = 0 // Delay before the animation starts
            )
        )
        Scaffold(

            bottomBar = {
                val isPlaying = audioPlayerState.value?.isPlaying == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.height(100.dp).padding(top = 4.dp)
                        .shadow(
                            elevation = 10.dp,
                            spotColor = MaterialTheme.colors.onSurface,
                            shape = RoundedCornerShape(8.dp)
                        ).background(MaterialTheme.colors.surface),
                ) {


                    Column(
                        modifier = Modifier.padding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                if (audioPlayerState.value?.isPlaying == true) {
                                    audioPlayer.pause()
                                } else {
                                    if (audioPlayerState.value?.currentPlayingResource != null) {
                                        audioPlayerState.value?.currentPlayingResource?.let {
                                            audioPlayer.play(
                                                it
                                            )
                                        }
                                    } else {
                                        audioPlayer.play(urls.first())
                                    }

                                }
                            }) {
                            Icon(
                                modifier = Modifier.size(54.dp),
                                painter = painterResource(if (!isPlaying) Res.drawable.play_circle_24px else Res.drawable.pause_circle_24px),
                                contentDescription = "Play"
                            )

                        }
                        Row(horizontalArrangement = Arrangement.Start) {
                            Text(
                                style = MaterialTheme.typography.button,
                                text = (audioPlayerState.value?.currentTime?.toLong()?.let {
                                    val minutes = it / 60
                                    val seconds = it % 60
                                    "$minutes:${seconds.toString().padStart(2, '0')}"
                                } ?: "0:00") + " / " +
                                        (audioPlayerState.value?.duration?.toLong()?.let {
                                            val minutes = it / 60
                                            val seconds = it % 60
                                            "$minutes:${seconds.toString().padStart(2, '0')}"
                                        } ?: "0:00"),
                            )
                            WavySlider(
                                modifier = Modifier.weight(4f),
                                value = animatedProgress,
                                onValueChange = {
                                    audioPlayer.seek(it)
                                },
                                enabled = true,

                                waveLength = 34.dp,
                                waveHeight = 8.dp,
                                waveVelocity = 20.dp to WaveDirection.TAIL,
                                waveThickness = 4.dp,
                                trackThickness = 4.dp,
                                incremental = true,

                                )
                        }
                    }
                }
            },
            topBar = {
                TopAppBar {
                    Text("Audio Player")
                }
            }
        ) {
            LazyColumn {
                items(urls.size) { index ->
                    var url = urls[index]
                    var isPlaying = audioPlayerState.value?.currentPlayingResource == url
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KamelImage(
                                resource = asyncPainterResource(data = "https://picsum.photos/200?random=${index + 1}"),
                                contentDescription = "description"
                            )
                            androidx.compose.material3.Text(
                                "Audio ${index + 1}",
                                style = MaterialTheme.typography.button
                            )
                            IconButton(onClick = {
                                audioPlayer.play(url = url)
                            }) {
                                Icon(
                                    painter = painterResource(if (!isPlaying) Res.drawable.play_circle_24px else Res.drawable.pause_circle_24px),
                                    contentDescription = "Play"
                                )

                            }
                        }
                    }

                }
            }

        }


    }
}

