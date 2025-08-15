package com.example.paddleball.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val gameState by viewModel.gameState.collectAsState()

    // Maps to track which player controls which pointer and the pointer's position
    val activePointers = remember { mutableStateMapOf<PointerId, Boolean>() } // true for P1 (top), false for P2 (bottom)
    val pointerPositions = remember { mutableStateMapOf<PointerId, Float>() }

    val thumbZoneHeightDp = 150.dp
    val density = LocalDensity.current
    val thumbZoneHeightPx =
        remember(thumbZoneHeightDp, density) { with(density) { thumbZoneHeightDp.toPx() } }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val canvasHeight = size.height

                                event.changes.forEach { change ->
                                    val pointerId = change.id

                                    // Check if the pointer is currently pressed down
                                    if (change.pressed) {
                                        // If this is a new pointer that we aren't tracking yet, it's a "press"
                                        if (!activePointers.containsKey(pointerId)) {
                                            val touchY = change.position.y
                                            val isTopPlayerTouch = touchY < thumbZoneHeightPx
                                            val isBottomPlayerTouch = touchY > (canvasHeight - thumbZoneHeightPx)

                                            // Check if the press is inside a valid player zone
                                            if (isTopPlayerTouch || isBottomPlayerTouch) {
                                                val playerToAssign = isTopPlayerTouch // true for top, false for bottom
                                                // Check if the player's slot is already taken by another finger
                                                val slotTaken = activePointers.any { (_, isP1) -> isP1 == playerToAssign }

                                                if (!slotTaken) {
                                                    activePointers[pointerId] = playerToAssign
                                                    pointerPositions[pointerId] = change.position.x
                                                }
                                            }
                                        }
                                        // If we are already tracking this pointer, it's a "move"
                                        else {
                                            activePointers[pointerId]?.let { isTopPlayer ->
                                                val lastX = pointerPositions[pointerId] ?: change.previousPosition.x
                                                val dx = change.position.x - lastX
                                                viewModel.movePaddleHorizontal(isTopPlayer, dx)
                                                pointerPositions[pointerId] = change.position.x
                                            }
                                        }
                                    }
                                    // If the pointer is not pressed, it's a "release"
                                    else {
                                        activePointers.remove(pointerId)
                                        pointerPositions.remove(pointerId)
                                    }

                                    // Consume the change to prevent it from being handled elsewhere
                                    change.consume()
                                }
                            }
                        }
                    }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Background
            drawRect(color = Color.Black, topLeft = Offset.Zero, size = this.size)

            // Visual indicators for touch zones
            val indicatorColor = Color.White.copy(alpha = 0.15f)
            val indicatorStrokeWidth = 2.dp.toPx()

            // Top touch zone
            drawRect(
                color = indicatorColor,
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, thumbZoneHeightPx),
            )
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, thumbZoneHeightPx),
                style = Stroke(width = indicatorStrokeWidth),
            )

            // Bottom touch zone
            drawRect(
                color = indicatorColor,
                topLeft = Offset(0f, canvasHeight - thumbZoneHeightPx),
                size = Size(canvasWidth, thumbZoneHeightPx),
            )
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(0f, canvasHeight - thumbZoneHeightPx),
                size = Size(canvasWidth, thumbZoneHeightPx),
                style = Stroke(width = indicatorStrokeWidth),
            )

            // Ball
            drawCircle(
                color = Color.White,
                radius = gameState.ball.radius,
                center = Offset(gameState.ball.x, gameState.ball.y),
            )

            // Player 1 Paddle (Top)
            drawRect(
                color = Color.White,
                topLeft = Offset(gameState.player1.x, gameState.player1.y),
                size = Size(gameState.player1.width, gameState.player1.height),
            )

            // Player 2 Paddle (Bottom)
            drawRect(
                color = Color.White,
                topLeft = Offset(gameState.player2.x, gameState.player2.y),
                size = Size(gameState.player2.width, gameState.player2.height),
            )
        }

        // Score Text
        Text(
            text = "${gameState.player1Score} - ${gameState.player2Score}",
            style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 48.sp),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 20.dp),
        )
    }

    // Game loop
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.update()
            delay(16) // Aims for ~60 FPS
        }
    }
}
