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
import androidx.compose.ui.graphics.drawscope.Stroke // For drawing outlines
import androidx.compose.ui.input.pointer.PointerEventType
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

    val activePointers = remember { mutableStateMapOf<PointerId, Boolean>() }
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
                                val canvasHeight = size.height // Use DrawScope's size.height

                                event.changes.forEach { change ->
                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            val touchY = change.position.y
                                            var isTopPlayerTouchAssigned = false
                                            var isBottomPlayerTouchAssigned = false

                                            // Use canvasHeight from DrawScope for consistency
                                            if (touchY >= 0 && touchY < thumbZoneHeightPx) {
                                                isTopPlayerTouchAssigned = true
                                            } else if (
                                                touchY > (canvasHeight - thumbZoneHeightPx) &&
                                                touchY <= canvasHeight
                                            ) {
                                                isBottomPlayerTouchAssigned = true
                                            }

                                            if (
                                                isTopPlayerTouchAssigned || isBottomPlayerTouchAssigned
                                            ) {
                                                val playerToAssign = isTopPlayerTouchAssigned
                                                val slotTaken =
                                                    activePointers.any { (_, isP1) ->
                                                        isP1 == playerToAssign
                                                    }
                                                if (
                                                    !slotTaken && !activePointers.containsKey(change.id)
                                                ) {
                                                    activePointers[change.id] = playerToAssign
                                                    pointerPositions[change.id] = change.position.x
                                                }
                                            }
                                            change.consume()
                                        }

                                        PointerEventType.Move -> {
                                            if (
                                                activePointers.containsKey(change.id) && change.pressed
                                            ) {
                                                val isTopPlayer = activePointers[change.id]!!
                                                val lastX =
                                                    pointerPositions[change.id]
                                                        ?: change.previousPosition.x
                                                val dx = change.position.x - lastX
                                                viewModel.movePaddleHorizontal(isTopPlayer, dx)
                                                pointerPositions[change.id] = change.position.x
                                                change.consume()
                                            }
                                        }

                                        PointerEventType.Release,
                                        PointerEventType.Exit -> {
                                            activePointers.remove(change.id)
                                            pointerPositions.remove(change.id)
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
        ) {
            // --- Drawing Code ---
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Background
            drawRect(color = Color.Black, topLeft = Offset.Zero, size = this.size)

            // Thumb Zone Indicators
            val indicatorColor = Color.White.copy(alpha = 0.15f) // Semi-transparent white
            val indicatorStrokeWidth = 2.dp.toPx()

            // Top Thumb Zone Indicator
            drawRect(
                color = indicatorColor,
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, thumbZoneHeightPx),
            )
            // Optional: Outline for more definition
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, thumbZoneHeightPx),
                style = Stroke(width = indicatorStrokeWidth),
            )

            // Bottom Thumb Zone Indicator
            drawRect(
                color = indicatorColor,
                topLeft = Offset(0f, canvasHeight - thumbZoneHeightPx),
                size = Size(canvasWidth, thumbZoneHeightPx),
            )
            // Optional: Outline for more definition
            drawRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(0f, canvasHeight - thumbZoneHeightPx),
                size = Size(canvasWidth, thumbZoneHeightPx),
                style = Stroke(width = indicatorStrokeWidth),
            )

            // Ball
            drawCircle(
                color = Color.White,
                radius = gameState.ball.radius.dp.toPx(),
                center = Offset(gameState.ball.x.dp.toPx(), gameState.ball.y.dp.toPx()),
            )

            // Player 1 Paddle (Top)
            drawRect(
                color = Color.White,
                topLeft = Offset(gameState.player1.x.dp.toPx(), gameState.player1.y.dp.toPx()),
                size = Size(gameState.player1.width.dp.toPx(), gameState.player1.height.dp.toPx()),
            )

            // Player 2 Paddle (Bottom)
            drawRect(
                color = Color.White,
                topLeft = Offset(gameState.player2.x.dp.toPx(), gameState.player2.y.dp.toPx()),
                size = Size(gameState.player2.width.dp.toPx(), gameState.player2.height.dp.toPx()),
            )
        }

        // Score Text - Make sure you have both if you want the rotated score
        // Score Text for Player 2 (Bottom Player - standard orientation)
        Text(
            text = "${gameState.player1Score} - ${gameState.player2Score}",
            style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 48.sp),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 20.dp), // Adjust padding if needed
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.update()
            delay(16)
        }
    }
}