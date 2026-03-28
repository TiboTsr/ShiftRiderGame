package com.example.jeubateau

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

class GameActivity : AppCompatActivity() {

    private lateinit var player: ImageView
    private lateinit var obstacle: ImageView
    private lateinit var tvScore: TextView
    private lateinit var gameLayout: ConstraintLayout

    private var currentLane = 1
    private var score = 0
    private var isGameOver = false
    private var screenWidth = 0
    private var gameSpeed = 12f

    private var x1 = 0f
    private val MIN_DISTANCE = 100

    private val handler = Handler(Looper.getMainLooper())
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (!isGameOver) {
                updateGame()
                handler.postDelayed(this, 20)
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

        appliquerTheme()

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
                            if (deltaX > 0) movePlayer(left = false)
                            else movePlayer(left = true)
                        }
                    }
                }
            }
            true
        }
    }

    private fun appliquerTheme() {
        val prefs = getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
        val themeNom = prefs.getString("THEME_NOM", "ATHLÈTE")
        gameSpeed = prefs.getFloat("VITESSE_ACTUELLE", 12f)

        when (themeNom) {
            "ATHLÈTE" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_champ)
                player.setImageResource(R.drawable.coureur_image)
            }
            "BMX PRO" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_champ)
                player.setImageResource(R.drawable.velo_image)
            }
            "BOLIDE" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_route)
                player.setImageResource(R.drawable.voiture_image)
            }
            "FORMULE 1" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_circuit)
                player.setImageResource(R.drawable.voituredecourse_image)
            }
            "CORSAIRE" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_mer)
                player.setImageResource(R.drawable.bateau_image)
            }
            "VOL LÉGER" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_ciel)
                player.setImageResource(R.drawable.petitavion_image)
            }
            "AIRLINER" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_ciel)
                player.setImageResource(R.drawable.grandavion_image)
            }
            "STAR JUMPER" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_espace)
                player.setImageResource(R.drawable.fusee_image)
            }
            "COMÈTE" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_espace)
                player.setImageResource(R.drawable.comete_image)
            }
            else -> {
                gameLayout.setBackgroundResource(R.drawable.fond_champ)
                player.setImageResource(R.drawable.coureur_image)
            }
        }
        
        // On peut aussi mettre une image d'obstacle par défaut ou selon le thème
        // obstacle.setImageResource(R.drawable.votre_image_obstacle)
    }

    private fun movePlayer(left: Boolean) {
        if (left && currentLane > 0) currentLane--
        else if (!left && currentLane < 2) currentLane++

        val laneWidth = screenWidth / 3f
        val targetX = (currentLane * laneWidth) + (laneWidth / 2f) - (player.width / 2f)

        player.animate().x(targetX).setDuration(100).start()
    }

    private fun updateGame() {
        obstacle.translationY += gameSpeed

        if (obstacle.translationY > gameLayout.height) {
            score += 10
            tvScore.text = "Score: $score"
            
            if (score % 100 == 0) gameSpeed += 1f
            
            spawnObstacle()
        }

        checkCollision()
    }

    private fun spawnObstacle() {
        obstacle.translationY = -300f
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

        val intent = Intent(this, GameOverActivity::class.java)
        intent.putExtra("SCORE_FINAL", score)
        startActivity(intent)
        finish()
    }

    private fun resetGame() {
        score = 0
        isGameOver = false
        currentLane = 1
        tvScore.text = "Score: 0"
        spawnObstacle()
        handler.post(gameRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(gameRunnable)
    }
}