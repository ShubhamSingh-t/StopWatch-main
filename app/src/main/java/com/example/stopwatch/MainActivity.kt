package com.example.stopwatch

import android.os.Bundle
import androidx.compose.ui.graphics.StrokeCap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stopwatch.ui.theme.StopWatchTheme
import kotlinx.coroutines.*
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StopWatchTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StopwatchScreen()
                }
            }
        }
    }
}

class Stopwatch(var onTick: (Long) -> Unit) {
    private var timeMs: Long = 0L
    private var isRunning = false
    private var job: Job? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        job = CoroutineScope(Dispatchers.Default).launch {
            val startTime = System.currentTimeMillis() - timeMs
            while (isRunning) {
                timeMs = System.currentTimeMillis() - startTime
                onTick(timeMs)
                delay(16) // ~60 FPS to update every 16 milliseconds
            }
        }
    }

    fun pause() {
        isRunning = false
        job?.cancel()
    }

    fun reset() {
        pause()
        timeMs = 0L
        onTick(timeMs)
    }

    fun currentTime(): Long = timeMs
}


@Composable
fun StopwatchScreen() {
    val stopwatch = remember { Stopwatch {} }
    var timeMs by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    val laps = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        stopwatch.onTick = { timeMs = it }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Circular Timer at the Top of the screen
        val totalProgress = min(timeMs / 60000f, 1f) // Clamp progress to 1f max (1 minute)
        val animatedProgress by animateFloatAsState(targetValue = totalProgress)

        Box(modifier = Modifier
            .align(Alignment.TopCenter) // Position at top-center
            .padding(top = 50.dp) // Adjust padding from top
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress }, // Updated to use a lambda for progress
                modifier = Modifier.size(200.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = formatTime(timeMs),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Lap List in Between the Timer and Buttons
        LapListSection(
            laps = laps,
            modifier = Modifier
                .align(Alignment.TopCenter) // Center it horizontally
                .padding(top = 220.dp) // Adjust the vertical position below the timer
        )

        // Buttons (Start/Stop, Reset/Lap) at the Bottom Center
        TimerFabSection(
            isRunning = isRunning,
            onStartStop = {
                if (isRunning) stopwatch.pause() else stopwatch.start()
                isRunning = !isRunning
            },
            onLapOrReset = {
                if (isRunning) {
                    laps.add(formatTime(stopwatch.currentTime()))
                } else if (timeMs > 0L) {
                    stopwatch.reset()
                    laps.clear()
                    isRunning = false
                }
            },
            showReset = !isRunning && timeMs > 0L,
            modifier = Modifier
                .align(Alignment.BottomCenter) // Keep buttons at the bottom center
                .padding(bottom = 48.dp)
                .navigationBarsPadding()
        )
    }
}


@Composable
fun TimerFabSection(
    isRunning: Boolean,
    onStartStop: () -> Unit,
    onLapOrReset: () -> Unit,
    showReset: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onStartStop,
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            if (isRunning || showReset) {
                FloatingActionButton(
                    onClick = onLapOrReset,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Flag else Icons.Default.Refresh,
                        contentDescription = if (isRunning) "Lap" else "Reset",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LapListSection(laps: List<String>, modifier: Modifier = Modifier) {
    if (laps.isNotEmpty()) {
        Column(
            modifier = modifier
                .padding(horizontal = 24.dp) // Adds padding to the left and right
        ) {
            Text(
                "Laps",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp) // Padding below the title
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp), // Spacing between laps
                modifier = Modifier.fillMaxHeight()
            ) {
                itemsIndexed(laps.reversed()) { index, lap ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp) // Padding around each lap item
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp), // Padding inside each lap item
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Lap ${laps.size - index}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface // Text color
                            )
                            Text(
                                lap,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary // Color for time
                            )
                        }
                    }
                }
            }
        }
    }
}


fun formatTime(ms: Long): String {
    val minutes = (ms / 60000)
    val seconds = (ms / 1000) % 60
    val millis = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, millis)
}
