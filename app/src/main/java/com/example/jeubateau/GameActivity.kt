package com.example.jeubateau

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class GameActivity : AppCompatActivity() {

    private lateinit var player: View
    private lateinit var obstacle: View
    private lateinit var tvScore: TextView
    private lateinit var gameLayout: View

    private var currentLane = 1
    private var score = 0
    private var isGameOver = false
    private var screenWidth = 0

    private var x1 = 0f
    private val MIN_DISTANCE = 100

    private val handler = Handler(Looper.getMainLooper())
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (!isGameOver) {
                updateGame()
                handler.postDelayed(this, 30)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        player = findViewById(R.id.player)
        obstacle = findViewById(R.id.obstacle)
        tvScore = findViewById(R.id.tv_score)
        gameLayout = findViewById(R.id.game_layout)

        gameLayout.post {
            screenWidth = gameLayout.width
            resetGame()
        }

        gameLayout.setOnTouchListener { _, event ->
            if (!isGameOver) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x1 = event.x
                    }
                    MotionEvent.ACTION_UP -> {
                        val x2 = event.x
                        val deltaX = x2 - x1
                        if (abs(deltaX) > MIN_DISTANCE) {
                            if (x2 > x1) movePlayer(left = false)
                            else movePlayer(left = true)
                        }
                    }
                }
            }
            true
        }
    }

    private fun movePlayer(left: Boolean) {
        if (left && currentLane > 0) currentLane--
        else if (!left && currentLane < 2) currentLane++

        val laneWidth = screenWidth / 3f
        val targetX = (currentLane * laneWidth) + (laneWidth / 2f) - (player.width / 2f)

        ObjectAnimator.ofFloat(player, "x", targetX).setDuration(150).start()
    }

    private fun updateGame() {
        obstacle.translationY += 20f + (score / 50)

        if (obstacle.translationY > gameLayout.height) {
            score += 10
            tvScore.text = "Score: $score"
            spawnObstacle()
        }

        checkCollision()
    }

    private fun spawnObstacle() {
        obstacle.translationY = -200f
        val randomLane = (0..2).random()
        val laneWidth = screenWidth / 3f
        obstacle.x = (randomLane * laneWidth) + (laneWidth / 2f) - (obstacle.width / 2f)
    }

    private fun checkCollision() {
        val playerRect = Rect()
        player.getHitRect(playerRect)

        val obstacleRect = Rect()
        obstacle.getHitRect(obstacleRect)

        if (Rect.intersects(playerRect, obstacleRect)) {
            endGame()
        }
    }

    private fun endGame() {
        isGameOver = true
        handler.removeCallbacks(gameRunnable)

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        val currentHigh = prefs.getInt("HIGH_SCORE", 0)
        if (score > currentHigh) {
            prefs.edit().putInt("HIGH_SCORE", score).apply()
        }

        handler.postDelayed({
            val intent = Intent(this, GameOverActivity::class.java)
            intent.putExtra("SCORE_FINAL", score)
            startActivity(intent)
            finish()
        }, 1000)
    }

    private fun resetGame() {
        score = 0
        isGameOver = false
        currentLane = 1
        spawnObstacle()
        handler.post(gameRunnable)
    }
}