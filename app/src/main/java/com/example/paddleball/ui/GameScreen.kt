// GameScreen.kt - Remove incorrect .dp.toPx() conversions

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
                                val canvasHeight = size.height

                                event.changes.forEach { change ->
                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            val touchY = change.position.y
                                            val isTopPlayerTouch = touchY < thumbZoneHeightPx
                                            val isBottomPlayerTouch = touchY > (canvasHeight - thumbZoneHeightPx)

                                            if (isTopPlayerTouch || isBottomPlayerTouch) {
                                                val playerToAssign = isTopPlayerTouch
                                                val slotTaken = activePointers.any { (_, isP1) -> isP1 == playerToAssign }
                                                if (!slotTaken && !activePointers.containsKey(change.id)) {
                                                    activePointers[change.id] = playerToAssign
                                                    pointerPositions[change.id] = change.position.x
                                                }
                                            }
                                            change.consume()
                                        }
                                        PointerEventType.Move -> {
                                            if (activePointers.containsKey(change.id) && change.pressed) {
                                                val isTopPlayer = activePointers[change.id]!!
                                                val lastX = pointerPositions[change.id] ?: change.previousPosition.x
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
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawRect(color = Color.Black, topLeft = Offset.Zero, size = this.size)

            val indicatorColor = Color.White.copy(alpha = 0.15f)
            val indicatorStrokeWidth = 2.dp.toPx()

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

            // --- CHANGED: Removed all .dp.toPx() conversions below ---
            // The ViewModel now provides correct pixel values directly.

            // Ball
            drawCircle(
                color = Color.White,
                radius = gameState.ball.radius, // Use pixel value directly
                center = Offset(gameState.ball.x, gameState.ball.y), // Use pixel values directly
            )

            // Player 1 Paddle (Top)
            drawRect(
                color = Color.White,
                topLeft = Offset(gameState.player1.x, gameState.player1.y), // Use pixel values directly
                size = Size(gameState.player1.width, gameState.player1.height), // Use pixel values directly
            )

            // Player 2 Paddle (Bottom)
            drawRect(
                color = Color.White,
                topLeft = Offset(gameState.player2.x, gameState.player2.y), // Use pixel values directly
                size = Size(gameState.player2.width, gameState.player2.height), // Use pixel values directly
            )
        }

        Text(
            text = "${gameState.player1Score} - ${gameState.player2Score}",
            style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 48.sp),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 20.dp),
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.update()
            delay(16)
        }
    }
}
