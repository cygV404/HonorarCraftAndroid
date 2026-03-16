package com.juliandobrodolac.honorarcraftandroid

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juliandobrodolac.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme
import com.juliandobrodolac.honorarcraftandroid.ui.theme.MontserratFontFamily

enum class SplashState {
    Initial, Finished
}

@Composable
fun AnimatedSplashScreen4() {
    var currentState by remember { mutableStateOf(SplashState.Initial) }

    val transition = updateTransition(targetState = currentState, label = "FullSplashTransition")

    // 1. Y-Versatz für das fallende C (0-1000ms fallen)
    val fallingYOffset by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 3000
                -100f at 0 with LinearOutSlowInEasing
                0f at 1000
                0f at 3000
            }
        },
        label = "FallingY"
    ) { state -> if (state == SplashState.Initial) -100f else 0f }

    // 2. X-Versatz für beide Cs (Rollen startet nach 1500ms)
    val rollingXOffset by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 3000
                0f at 1500 with LinearOutSlowInEasing
                50f at 3000
            }
        },
        label = "RollingX"
    ) { state -> if (state == SplashState.Initial) 0f else 50f }

    // 3. Rotation für das fallende C (0 -> 270 beim Fall, dann Pause, dann auf 720 beim Rollen)
    val fallingRotation by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 3000
                0f at 0 with LinearOutSlowInEasing
                270f at 1000
                270f at 1500 with LinearOutSlowInEasing
                720f at 3000
            }
        },
        label = "FallingRotation"
    ) { state -> if (state == SplashState.Initial) 0f else 720f }

    // 4. Rotation für das Basis-C (Wartet bei 270, rollt dann ab 1500ms mit)
    val baseRotation by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 3000
                270f at 1500 with LinearOutSlowInEasing
                720f at 3000
            }
        },
        label = "BaseRotation"
    ) { state -> if (state == SplashState.Initial) 270f else 720f }

    // 5. Alpha für "raft" (Erscheint am Ende der Roll-Animation)
    val raftAlpha by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 3000
                0f at 2000
                1f at 2300
            }
        },
        label = "RaftAlpha"
    ) { state -> if (state == SplashState.Initial) 0f else 1f }

    LaunchedEffect(Unit) {
        currentState = SplashState.Finished
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Honorar",
                fontSize = 50.sp,
                fontFamily = MontserratFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Box(contentAlignment = Alignment.Center) {
                // Das ursprüngliche "C" (Basis) - jetzt transparent
                Text(
                    text = "C",
                    fontSize = 50.sp,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .offset(x = ((-16) + rollingXOffset).dp, y = (-20).dp)
                        .rotate(baseRotation)
                        .alpha(0f)
                )

                // Das fallende "C"
                Text(
                    text = "C",
                    fontSize = 50.sp,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .offset(x = ((-16) + rollingXOffset).dp, y = ((-20) + fallingYOffset).dp)
                        .rotate(fallingRotation)
                )

                // Der Text "raft" erscheint hinter dem C, um "Craft" zu bilden
                Text(
                    text = "raft",
                    fontSize = 50.sp,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .offset(x = (50 + rollingXOffset).dp, y = (-20).dp)
                        .alpha(raftAlpha)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnimatedSplashScreen4Preview() {
    HonorarCraftAndroidTheme {
        AnimatedSplashScreen4()
    }
}
