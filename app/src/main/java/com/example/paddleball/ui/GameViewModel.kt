// GameViewModel.kt - Use the real screen height for all calculations

package com.example.paddleball.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

// CHANGED: Increased paddle and ball sizes
const val PADDLE_LENGTH = 300f // Was 100f
const val PADDLE_THICKNESS = 40f // Was 20f
const val BALL_RADIUS = 30f // Was 10f

const val SPEED_INCREASE_FACTOR = 1.05f

const val MAX_BOUNCE_ANGLE_EFFECT = 7.5f
private const val COLLISION_TOLERANCE = 0.1f

const val CONTROL_ZONE_HEIGHT = 150f
const val GAP_BETWEEN_PADDLE_AND_CONTROL_ZONE = 350f

class GameViewModel constructor(
    private val screenWidth: Float = 400f,
    private val screenHeight: Float = 800f
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState(screenWidth, screenHeight))
    val gameState: StateFlow<GameState> = _gameState

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
            var collisionData =
                checkPaddleCollision(
                    currentBall, player1Paddle, prevBallX, prevBallY,
                    tentativeBallX, tentativeBallY, newVelocityX, newVelocityY
                )
            if (collisionData.hit) {
                tentativeBallX = collisionData.newX
                tentativeBallY = collisionData.newY
                newVelocityX = collisionData.newVelX
                newVelocityY = collisionData.newVelY
            } else {
                collisionData =
                    checkPaddleCollision(
                        currentBall, player2Paddle, prevBallX, prevBallY,
                        tentativeBallX, tentativeBallY, newVelocityX, newVelocityY
                    )
                if (collisionData.hit) {
                    tentativeBallX = collisionData.newX
                    tentativeBallY = collisionData.newY
                    newVelocityX = collisionData.newVelX
                    newVelocityY = collisionData.newVelY
                }
            }

            currentState.copy(
                ball = currentBall.copy(
                    x = tentativeBallX,
                    y = tentativeBallY,
                    velocityX = newVelocityX,
                    velocityY = newVelocityY,
                )
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

        val isTopPaddle = paddle === _gameState.value.player1
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

                    // CHANGED: Increase speed on paddle hit
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
        return Ball(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            radius = BALL_RADIUS, // Use the new constant
            velocityX = (if (Math.random() > 0.5) 1f else -1f) * (10f + (Math.random() * 2.5f).toFloat()),
            velocityY = (if (Math.random() > 0.5) 1f else -1f) * (10f + (Math.random() * 1f).toFloat()),
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
    val ball: Ball = Ball(screenWidth, screenHeight),
    val player1: Paddle = // Top Paddle
        Paddle(
            x = (screenWidth - PADDLE_LENGTH) / 2f,
            y = CONTROL_ZONE_HEIGHT + GAP_BETWEEN_PADDLE_AND_CONTROL_ZONE,
            width = PADDLE_LENGTH,
            height = PADDLE_THICKNESS,
        ),
    val player2: Paddle = // Bottom Paddle
        Paddle(
            x = (screenWidth - PADDLE_LENGTH) / 2f,
            y = (screenHeight - CONTROL_ZONE_HEIGHT) - GAP_BETWEEN_PADDLE_AND_CONTROL_ZONE - PADDLE_THICKNESS,
            width = PADDLE_LENGTH,
            height = PADDLE_THICKNESS,
        ),
    val player1Score: Int = 0,
    val player2Score: Int = 0,
)

data class Ball(
    val screenWidth: Float = 400f,
    val screenHeight: Float = 800f,
    val x: Float = screenWidth / 2f,
    val y: Float = screenHeight / 2f,
    val radius: Float = BALL_RADIUS, // Use the new constant
    val velocityX: Float = 10f,
    val velocityY: Float = 10f,
)

data class Paddle(
    val x: Float, val y: Float, val width: Float, val height: Float
)
