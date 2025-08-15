// GameViewModel.kt - Use the real screen height for all calculations

package com.example.paddleball.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel
import kotlin.math.abs

@KoinViewModel
class GameViewModel(
    private val screenWidth: Float = 400f,
    private val screenHeight: Float = 800f,
) : ViewModel() {

    companion object {
        // Layout constants as ratios of screen dimensions
        // --- MODIFIED: Increased initial paddle size and moved them closer to the center ---
        const val PADDLE_LENGTH_RATIO = 0.65f      // Was 0.35f
        const val GAP_BETWEEN_PADDLE_AND_CONTROL_ZONE_RATIO = 0.10f // Was 0.02f

        const val PADDLE_THICKNESS_RATIO = 0.020f   // 2.5% of screen height
        const val BALL_RADIUS_RATIO = 0.02f         // 2% of screen width
        const val CONTROL_ZONE_HEIGHT_RATIO = 0.2f  // 20% of screen height

        // Game physics constants
        const val SPEED_INCREASE_FACTOR = 1.05f
        const val MAX_BOUNCE_ANGLE_EFFECT = 7.5f
        private const val COLLISION_TOLERANCE = 0.1f

        // Ball speed is now relative to screen height, for consistent speed across devices.
        private const val BALL_VERTICAL_SPEED_RATIO_PER_SECOND = 0.50f

        // Game progression constants
        private const val COLLISIONS_BEFORE_CHANGE = 5
        private const val PADDLE_SHRINK_FACTOR = 0.98f // Shrinks by 2% per hit
        private const val PADDLE_MOVE_AMOUNT_RATIO = 0.0025f // Moves by 0.25% of screen height
        private const val MIN_PADDLE_LENGTH_RATIO = 0.05f // Min length is 10% of screen width
    }

    // Calculate absolute sizes from ratios
    private val paddleLength = screenWidth * PADDLE_LENGTH_RATIO
    private val paddleThickness = screenHeight * PADDLE_THICKNESS_RATIO
    private val ballRadius = screenWidth * BALL_RADIUS_RATIO
    private val controlZoneHeight = screenHeight * CONTROL_ZONE_HEIGHT_RATIO
    private val paddleGap = screenHeight * GAP_BETWEEN_PADDLE_AND_CONTROL_ZONE_RATIO
    private val paddleMoveAmount = screenHeight * PADDLE_MOVE_AMOUNT_RATIO
    private val minPaddleLength = screenWidth * MIN_PADDLE_LENGTH_RATIO


    // Calculate per-frame velocity based on a 60 FPS target
    private val ballBaseSpeedY = (screenHeight * BALL_VERTICAL_SPEED_RATIO_PER_SECOND) / 60f
    private val ballBaseSpeedX =
        ballBaseSpeedY * (screenWidth / screenHeight) // Maintain aspect ratio for speed

    private fun createInitialState(): GameState {
        return GameState(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            ball = createResetBall(),
            player1 = Paddle( // Top Paddle
                x = (screenWidth - paddleLength) / 2f,
                y = controlZoneHeight + paddleGap,
                width = paddleLength,
                height = paddleThickness
            ),
            player2 = Paddle( // Bottom Paddle
                x = (screenWidth - paddleLength) / 2f,
                y = screenHeight - controlZoneHeight - paddleGap - paddleThickness,
                width = paddleLength,
                height = paddleThickness
            )
        )
    }

    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState

    fun update() {
        _gameState.update { currentState ->
            val currentBall = currentState.ball
            var newPlayer1 = currentState.player1
            var newPlayer2 = currentState.player2
            var newCollisionCount = currentState.collisionCount

            val prevBallX = currentBall.x
            val prevBallY = currentBall.y

            var tentativeBallX = currentBall.x + currentBall.velocityX
            val tentativeBallY = currentBall.y + currentBall.velocityY
            var newVelocityX = currentBall.velocityX
            val newVelocityY = currentBall.velocityY

            // 1. Scoring Walls
            if (tentativeBallY - currentBall.radius < 0) { // Player 2 scores
                return@update currentState.copy(
                    player2Score = currentState.player2Score + 1,
                    ball = createResetBall(),
                )
            }
            if (tentativeBallY + currentBall.radius > screenHeight) { // Player 1 scores
                return@update currentState.copy(
                    player1Score = currentState.player1Score + 1,
                    ball = createResetBall(),
                )
            }

            // 2. Screen Edge Collisions (Sides)
            if (tentativeBallX - currentBall.radius < 0) {
                tentativeBallX = currentBall.radius + COLLISION_TOLERANCE
                newVelocityX = abs(newVelocityX)
            } else if (tentativeBallX + currentBall.radius > screenWidth) {
                tentativeBallX = screenWidth - currentBall.radius - COLLISION_TOLERANCE
                newVelocityX = -abs(newVelocityX)
            }

            // 3. Paddle Collisions
            var collisionData = checkPaddleCollision(
                currentBall, newPlayer1, prevBallX, prevBallY,
                tentativeBallX, tentativeBallY, newVelocityX, newVelocityY
            )
            if (collisionData.hit) {
                newCollisionCount++
            } else {
                collisionData = checkPaddleCollision(
                    currentBall, newPlayer2, prevBallX, prevBallY,
                    tentativeBallX, tentativeBallY, newVelocityX, newVelocityY
                )
                if (collisionData.hit) {
                    newCollisionCount++
                }
            }

            // After a collision, check if paddles need to be modified
            if (collisionData.hit && newCollisionCount > COLLISIONS_BEFORE_CHANGE) {
                // Modify Player 1's paddle (Top)
                val p1OldWidth = newPlayer1.width
                val p1NewWidth = (p1OldWidth * PADDLE_SHRINK_FACTOR).coerceAtLeast(minPaddleLength)
                val p1WidthChange = p1OldWidth - p1NewWidth
                newPlayer1 = newPlayer1.copy(
                    width = p1NewWidth,
                    x = newPlayer1.x + p1WidthChange / 2, // Recenter
                    y = (newPlayer1.y - paddleMoveAmount).coerceAtLeast(controlZoneHeight)
                )

                // Modify Player 2's paddle (Bottom)
                val p2OldWidth = newPlayer2.width
                val p2NewWidth = (p2OldWidth * PADDLE_SHRINK_FACTOR).coerceAtLeast(minPaddleLength)
                val p2WidthChange = p2OldWidth - p2NewWidth
                newPlayer2 = newPlayer2.copy(
                    width = p2NewWidth,
                    x = newPlayer2.x + p2WidthChange / 2, // Recenter
                    y = (newPlayer2.y + paddleMoveAmount).coerceAtMost(screenHeight - controlZoneHeight - newPlayer2.height)
                )
            }

            val finalBallX = if (collisionData.hit) collisionData.newX else tentativeBallX
            val finalBallY = if (collisionData.hit) collisionData.newY else tentativeBallY
            val finalVelX = if (collisionData.hit) collisionData.newVelX else newVelocityX
            val finalVelY = if (collisionData.hit) collisionData.newVelY else newVelocityY


            currentState.copy(
                ball = currentBall.copy(
                    x = finalBallX,
                    y = finalBallY,
                    velocityX = finalVelX,
                    velocityY = finalVelY,
                ),
                player1 = newPlayer1,
                player2 = newPlayer2,
                collisionCount = newCollisionCount
            )
        }
    }

    private fun checkPaddleCollision(
        ball: Ball, paddle: Paddle, prevBallX: Float, prevBallY: Float,
        tentativeBallX: Float, tentativeBallY: Float, currentVelX: Float, currentVelY: Float
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

        val isTopPaddle = paddle.y < screenHeight / 2
        val movingTowardsFlatSurface =
            (isTopPaddle && currentVelY < 0) || (!isTopPaddle && currentVelY > 0)

        if (movingTowardsFlatSurface) {
            val paddleImpactSurfaceY = if (isTopPaddle) paddleBottom else paddleTop
            val ballLeadingEdgeTentativeY =
                if (isTopPaddle) tentativeBallY - ball.radius else tentativeBallY + ball.radius
            val ballLeadingEdgePrevY =
                if (isTopPaddle) prevBallY - ball.radius else prevBallY + ball.radius

            val crossedSurfaceVertically = if (isTopPaddle) {
                ballLeadingEdgeTentativeY < paddleImpactSurfaceY + COLLISION_TOLERANCE && ballLeadingEdgePrevY >= paddleImpactSurfaceY - COLLISION_TOLERANCE
            } else {
                ballLeadingEdgeTentativeY > paddleImpactSurfaceY - COLLISION_TOLERANCE && ballLeadingEdgePrevY <= paddleImpactSurfaceY + COLLISION_TOLERANCE
            }

            if (crossedSurfaceVertically) {
                val overlapsHorizontally =
                    tentativeBallX + ball.radius > paddleLeft && tentativeBallX - ball.radius < paddleRight
                if (overlapsHorizontally) {
                    val hitPositionNormalized =
                        (tentativeBallX - (paddleLeft + paddle.width / 2)) / (paddle.width / 2)
                    newVelX = currentVelX + hitPositionNormalized * MAX_BOUNCE_ANGLE_EFFECT
                    newVelX = newVelX.coerceIn(-abs(currentVelY) * 1.5f, abs(currentVelY) * 1.5f)
                    newVelY = -currentVelY
                    newTentativeBallY =
                        (if (isTopPaddle) paddleBottom + ball.radius else paddleTop - ball.radius) + (COLLISION_TOLERANCE * -newVelY.sign)

                    newVelX *= SPEED_INCREASE_FACTOR
                    newVelY *= SPEED_INCREASE_FACTOR

                    collisionOccurred = true
                }
            }
        }

        if (!collisionOccurred) {
            val movingTowardsLeftPaddleEdge =
                currentVelX > 0 && tentativeBallX + ball.radius > paddleLeft && prevBallX + ball.radius <= paddleLeft + COLLISION_TOLERANCE
            val movingTowardsRightPaddleEdge =
                currentVelX < 0 && tentativeBallX - ball.radius < paddleRight && prevBallX - ball.radius >= paddleRight - COLLISION_TOLERANCE

            if (movingTowardsLeftPaddleEdge || movingTowardsRightPaddleEdge) {
                val overlapsVertically =
                    tentativeBallY + ball.radius > paddleTop && tentativeBallY - ball.radius < paddleBottom
                if (overlapsVertically) {
                    newVelX = -currentVelX
                    newTentativeBallX = if (movingTowardsLeftPaddleEdge) {
                        paddleLeft - ball.radius - (COLLISION_TOLERANCE * newVelX.sign)
                    } else {
                        paddleRight + ball.radius + (COLLISION_TOLERANCE * newVelX.sign)
                    }

                    newVelX *= SPEED_INCREASE_FACTOR
                    newVelY *= SPEED_INCREASE_FACTOR

                    collisionOccurred = true
                }
            }
        }
        return PaddleCollisionState(
            collisionOccurred,
            newTentativeBallX,
            newTentativeBallY,
            newVelX,
            newVelY
        )
    }

    fun movePaddleHorizontal(isTopPlayer: Boolean, deltaX: Float) {
        _gameState.update { currentState ->
            val playerToUpdate = if (isTopPlayer) currentState.player1 else currentState.player2
            var newX = playerToUpdate.x + deltaX
            val minX = 0f
            val maxX = screenWidth - playerToUpdate.width
            newX = newX.coerceIn(minX, maxX)

            if (isTopPlayer) {
                currentState.copy(player1 = playerToUpdate.copy(x = newX))
            } else {
                currentState.copy(player2 = playerToUpdate.copy(x = newX))
            }
        }
    }

    private fun createResetBall(): Ball {
        val randomFactorX = 0.8f + (Math.random() * 0.4f).toFloat()
        val randomFactorY = 0.8f + (Math.random() * 0.4f).toFloat()

        return Ball(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            radius = ballRadius,
            velocityX = (if (Math.random() > 0.5) 1f else -1f) * ballBaseSpeedX * randomFactorX,
            velocityY = (if (Math.random() > 0.5) 1f else -1f) * ballBaseSpeedY * randomFactorY,
        )
    }
}

val Float.sign: Float
    get() = when {
        this > 0f -> 1f
        this < 0f -> -1f
        else -> 0f
    }

data class PaddleCollisionState(
    val hit: Boolean, val newX: Float, val newY: Float, val newVelX: Float, val newVelY: Float
)

data class GameState(
    val screenWidth: Float,
    val screenHeight: Float,
    val ball: Ball,
    val player1: Paddle,
    val player2: Paddle,
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val collisionCount: Int = 0,
)

data class Ball(
    val screenWidth: Float = 400f,
    val screenHeight: Float = 800f,
    val x: Float = screenWidth / 2f,
    val y: Float = screenHeight / 2f,
    val radius: Float,
    val velocityX: Float = 10f,
    val velocityY: Float = 10f,
)

data class Paddle(
    val x: Float, val y: Float, val width: Float, val height: Float
)