package com.example.paddleball.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

// Constants remain the same
const val SCREEN_WIDTH = 400f
const val SCREEN_HEIGHT = 800f
const val PADDLE_LENGTH = 100f
const val PADDLE_THICKNESS = 20f
const val MAX_BOUNCE_ANGLE_EFFECT = 7.5f
private const val COLLISION_TOLERANCE = 0.1f // Small tolerance for floating point comparisons

// New constant representing the height of the control/thumb zones at top and bottom
const val CONTROL_ZONE_HEIGHT = 150f // From GameScreen thumbZoneHeightDp
const val GAP_BETWEEN_PADDLE_AND_CONTROL_ZONE = 10f // The desired visual space


class GameViewModel constructor() : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    // updateScreenDimensions remains the same

    fun update() {
        _gameState.update { currentState ->
            val currentBall = currentState.ball
            val player1Paddle = currentState.player1
            val player2Paddle = currentState.player2

            val prevBallX = currentBall.x
            val prevBallY = currentBall.y

            var tentativeBallX = currentBall.x + currentBall.velocityX
            var tentativeBallY = currentBall.y + currentBall.velocityY
            var newVelocityX = currentBall.velocityX
            var newVelocityY = currentBall.velocityY

            // 1. Scoring Walls
            if (tentativeBallY - currentBall.radius < 0) { // Player 2 scores
                return@update currentState.copy(
                    player2Score = currentState.player2Score + 1,
                    ball = createResetBall(),
                )
            }
            if (tentativeBallY + currentBall.radius > SCREEN_HEIGHT) { // Player 1 scores
                return@update currentState.copy(
                    player1Score = currentState.player1Score + 1,
                    ball = createResetBall(),
                )
            }

            // 2. Screen Edge Collisions (Sides)
            if (tentativeBallX - currentBall.radius < 0) {
                tentativeBallX = currentBall.radius + COLLISION_TOLERANCE
                newVelocityX = abs(newVelocityX) // Ensure it moves away from wall
            } else if (tentativeBallX + currentBall.radius > SCREEN_WIDTH) {
                tentativeBallX = SCREEN_WIDTH - currentBall.radius - COLLISION_TOLERANCE
                newVelocityX = -abs(newVelocityX) // Ensure it moves away from wall
            }

            // 3. Paddle Collisions
            var collisionData =
                checkPaddleCollision(
                    ball = currentBall,
                    paddle = player1Paddle,
                    prevBallX = prevBallX,
                    prevBallY = prevBallY,
                    tentativeBallX = tentativeBallX,
                    tentativeBallY = tentativeBallY,
                    currentVelX = newVelocityX,
                    currentVelY = newVelocityY,
                )
            if (collisionData.hit) {
                tentativeBallX = collisionData.newX
                tentativeBallY = collisionData.newY
                newVelocityX = collisionData.newVelX
                newVelocityY = collisionData.newVelY
            } else {
                // Process player 2 paddle only if no hit with player 1
                collisionData =
                    checkPaddleCollision(
                        currentBall,
                        player2Paddle,
                        prevBallX,
                        prevBallY,
                        tentativeBallX,
                        tentativeBallY,
                        newVelocityX,
                        newVelocityY,
                    )
                if (collisionData.hit) {
                    tentativeBallX = collisionData.newX
                    tentativeBallY = collisionData.newY
                    newVelocityX = collisionData.newVelX
                    newVelocityY = collisionData.newVelY
                }
            }

            currentState.copy(
                ball =
                    currentBall.copy(
                        x = tentativeBallX,
                        y = tentativeBallY,
                        velocityX = newVelocityX,
                        velocityY = newVelocityY,
                    )
            )
        }
    }

    private fun checkPaddleCollision(
        ball: Ball,
        paddle: Paddle,
        prevBallX: Float,
        prevBallY: Float,
        tentativeBallX: Float,
        tentativeBallY: Float,
        currentVelX: Float,
        currentVelY: Float,
    ): PaddleCollisionState {
        var newTentativeBallX = tentativeBallX
        var newTentativeBallY = tentativeBallY
        var newVelX = currentVelX
        var newVelY = currentVelY
        var collisionOccurred = false

        val paddleLeft = paddle.x
        val paddleRight = paddle.x + paddle.width
        val paddleTop = paddle.y
        val paddleBottom = paddle.y + paddle.height

        // --- Check for collision with the flat surfaces (top/bottom of paddle) ---
        val isTopPaddle = paddle === _gameState.value.player1
        val movingTowardsFlatSurface =
            (isTopPaddle && currentVelY < 0) || (!isTopPaddle && currentVelY > 0)

        if (movingTowardsFlatSurface) {
            // Determine relevant edges for flat surface collision based on paddle
            val paddleImpactSurfaceY = if (isTopPaddle) paddleBottom else paddleTop
            val ballLeadingEdgeTentativeY =
                if (isTopPaddle) tentativeBallY - ball.radius else tentativeBallY + ball.radius
            val ballLeadingEdgePrevY =
                if (isTopPaddle) prevBallY - ball.radius else prevBallY + ball.radius

            // Check if ball crossed the paddle's flat surface plane in this frame
            val crossedSurfaceVertically =
                if (isTopPaddle) {
                    ballLeadingEdgeTentativeY < paddleImpactSurfaceY + COLLISION_TOLERANCE &&
                            ballLeadingEdgePrevY >= paddleImpactSurfaceY - COLLISION_TOLERANCE
                } else {
                    ballLeadingEdgeTentativeY > paddleImpactSurfaceY - COLLISION_TOLERANCE &&
                            ballLeadingEdgePrevY <= paddleImpactSurfaceY + COLLISION_TOLERANCE
                }

            if (crossedSurfaceVertically) {
                // Check for horizontal overlap at the (potential) time of vertical collision
                // A simple way is to check overlap at the tentative X position
                val overlapsHorizontally =
                    tentativeBallX + ball.radius > paddleLeft &&
                            tentativeBallX - ball.radius < paddleRight
                if (overlapsHorizontally) {
                    val hitPositionNormalized =
                        (tentativeBallX - (paddleLeft + paddle.width / 2)) / (paddle.width / 2)
                    newVelX = currentVelX + hitPositionNormalized * MAX_BOUNCE_ANGLE_EFFECT
                    newVelX = newVelX.coerceIn(-abs(currentVelY) * 1.5f, abs(currentVelY) * 1.5f)
                    newVelY = -currentVelY
                    newTentativeBallY =
                        (if (isTopPaddle) paddleBottom + ball.radius else paddleTop - ball.radius) +
                                (COLLISION_TOLERANCE * -newVelY.sign)

                    collisionOccurred = true
                }
            }
        }

        // --- Check for collision with the side surfaces (left/right of paddle) ---
        // Only if no flat surface collision occurred
        if (!collisionOccurred) {
            val movingTowardsLeftPaddleEdge =
                currentVelX > 0 &&
                        tentativeBallX + ball.radius > paddleLeft &&
                        prevBallX + ball.radius <= paddleLeft + COLLISION_TOLERANCE
            val movingTowardsRightPaddleEdge =
                currentVelX < 0 &&
                        tentativeBallX - ball.radius < paddleRight &&
                        prevBallX - ball.radius >= paddleRight - COLLISION_TOLERANCE

            if (movingTowardsLeftPaddleEdge || movingTowardsRightPaddleEdge) {
                // Check for vertical overlap at the (potential) time of horizontal collision
                val overlapsVertically =
                    tentativeBallY + ball.radius > paddleTop &&
                            tentativeBallY - ball.radius < paddleBottom

                if (overlapsVertically) {
                    newVelX = -currentVelX
                    newTentativeBallX =
                        if (movingTowardsLeftPaddleEdge) {
                            paddleLeft - ball.radius - (COLLISION_TOLERANCE * newVelX.sign)
                        } else { // movingTowardsRightPaddleEdge
                            paddleRight + ball.radius + (COLLISION_TOLERANCE * newVelX.sign)
                        }
                    // Optional: Dampen vertical velocity slightly or add slight vertical nudge if
                    // desired for side hits
                    // newVelY *= 0.98f
                    collisionOccurred = true
                }
            }
        }

        // A final catch-all AABB check if still no collision, for very fast objects or complex
        // scenarios (might be too aggressive)
        // This can help with deep penetrations but might cause "sticky" feeling if not careful
        // For simplicity, we'll keep the above more targeted checks. If issues persist, this is an
        // area for more advanced CCD.

        return PaddleCollisionState(
            collisionOccurred,
            newTentativeBallX,
            newTentativeBallY,
            newVelX,
            newVelY,
        )
    }

    // movePaddleHorizontal remains the same
    fun movePaddleHorizontal(isTopPlayer: Boolean, deltaX: Float) {
        _gameState.update { currentState ->
            val playerToUpdate = if (isTopPlayer) currentState.player1 else currentState.player2
            var newX = playerToUpdate.x + deltaX
            val minX = 0f
            val maxX = SCREEN_WIDTH - playerToUpdate.width
            newX = newX.coerceIn(minX, maxX)

            if (isTopPlayer) {
                currentState.copy(player1 = playerToUpdate.copy(x = newX))
            } else {
                currentState.copy(player2 = playerToUpdate.copy(x = newX))
            }
        }
    }

    // createResetBall remains the same
    private fun createResetBall(): Ball {
        return Ball(
            x = SCREEN_WIDTH / 2f,
            y = SCREEN_HEIGHT / 2f,
            radius = 10f,
            velocityX =
                (if (Math.random() > 0.5) 1f else -1f) *
                        (3f + (Math.random() * 2.5f).toFloat()), // Slightly faster base
            velocityY =
                (if (Math.random() > 0.5) 1f else -1f) *
                        (4f + (Math.random() * 1f).toFloat()), // Slightly faster base
        )
    }
}

// Data classes PaddleCollisionState, GameState, Ball, Paddle remain the same
// ... (as in your provided code)
val Float.sign: Float
    get() =
        when {
            this > 0f -> 1f
            this < 0f -> -1f
            else -> 0f
        }

data class PaddleCollisionState(
    val hit: Boolean,
    val newX: Float,
    val newY: Float,
    val newVelX: Float,
    val newVelY: Float,
)

data class GameState(
    val ball: Ball = Ball(),
    val player1: Paddle = // Top Paddle
        Paddle(
            x = (SCREEN_WIDTH - PADDLE_LENGTH) / 2f,
            // Top of paddle is (bottom of top control zone) + GAP
            y = CONTROL_ZONE_HEIGHT + GAP_BETWEEN_PADDLE_AND_CONTROL_ZONE,
            width = PADDLE_LENGTH,
            height = PADDLE_THICKNESS,
        ),
    val player2: Paddle = // Bottom Paddle
        Paddle(
            x = (SCREEN_WIDTH - PADDLE_LENGTH) / 2f,
            // Top of paddle is (top of bottom control zone) - GAP - PADDLE_THICKNESS
            y = (SCREEN_HEIGHT - CONTROL_ZONE_HEIGHT) - GAP_BETWEEN_PADDLE_AND_CONTROL_ZONE - PADDLE_THICKNESS,
            width = PADDLE_LENGTH,
            height = PADDLE_THICKNESS,
        ),
    val player1Score: Int = 0,
    val player2Score: Int = 0,
)

data class Ball(
    val x: Float = SCREEN_WIDTH / 2f,
    val y: Float = SCREEN_HEIGHT / 2f,
    val radius: Float = 10f,
    val velocityX: Float = 5f,
    val velocityY: Float = 5f,
)

data class Paddle(
    val x: Float,
    val y: Float,
    val width: Float, // This is PADDLE_LENGTH
    val height: Float, // This is PADDLE_THICKNESS
)
