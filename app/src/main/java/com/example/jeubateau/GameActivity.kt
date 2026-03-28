package com.example.jeubateau

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
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

    private var frameDelay = 16L // 16ms = ~60 FPS

    private lateinit var player: ImageView
    private lateinit var obstacle: ImageView
    private lateinit var obstacle2: ImageView
    private lateinit var tvScore: TextView
    private lateinit var gameLayout: ConstraintLayout

    private var difficulteActuelle = "TRÈS FACILE"

    private var currentLane = 1
    private var score = 0
    private var isGameOver = false
    private var screenWidth = 0
    private var gameSpeed = 12f

    private var x1 = 0f
    private val MIN_DISTANCE = 100

    private var obstacleResources: List<Int> = listOf()

    private val handler = Handler(Looper.getMainLooper())
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (!isGameOver) {
                updateGame()
                handler.postDelayed(this, frameDelay)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_game)

        player = findViewById(R.id.player)
        obstacle = findViewById(R.id.obstacle)
        obstacle2 = findViewById(R.id.obstacle2)
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
        difficulteActuelle = prefs.getString("DIFF_ACTUELLE", "TRÈS FACILE") ?: "TRÈS FACILE"

        val is60Fps = prefs.getBoolean("SETTING_60FPS", true)
        frameDelay = if (is60Fps) 16L else 32L
        val themeNom = prefs.getString("THEME_NOM", "ATHLÈTE")
        gameSpeed = prefs.getFloat("VITESSE_ACTUELLE", 12f)

        when (themeNom) {
            "ATHLÈTE" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_champ)
                player.setImageResource(R.drawable.coureur_image)
                obstacleResources = listOf(R.drawable.tree_image, R.drawable.rock_image, R.drawable.barriere_image, R.drawable.vache_image)
            }
            "BMX PRO" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_champ)
                player.setImageResource(R.drawable.velo_image)
                obstacleResources = listOf(R.drawable.tree_image, R.drawable.rock_image, R.drawable.barriere_image, R.drawable.vache_image)
            }
            "CAVALIER" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_champ)
                player.setImageResource(R.drawable.cavalier_image)
                obstacleResources = listOf(R.drawable.tree_image, R.drawable.rock_image, R.drawable.barriere_image, R.drawable.vache_image)
            }
            "BOLIDE" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_route)
                player.setImageResource(R.drawable.voiture_image)
                obstacleResources = listOf(R.drawable.pneu_image, R.drawable.oil_image, R.drawable.barriere_image)
            }
            "FORMULE 1" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_circuit)
                player.setImageResource(R.drawable.voituredecourse_image)
                obstacleResources = listOf(R.drawable.pneu_image, R.drawable.oil_image, R.drawable.barriere_image)
            }
            "CORSAIRE" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_mer)
                player.setImageResource(R.drawable.bateau_image)
                obstacleResources = listOf(R.drawable.bouee_image, R.drawable.requin_image, R.drawable.rock_image)
            }
            "VOL LÉGER" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_ciel)
                player.setImageResource(R.drawable.petitavion_image)
                obstacleResources = listOf(R.drawable.bird1_image, R.drawable.bird2_image, R.drawable.orage_image, R.drawable.orage2_image, R.drawable.ballon_image, R.drawable.parachute_image, R.drawable.helicoptere_image)
            }
            "AIRLINER" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_ciel)
                player.setImageResource(R.drawable.grandavion_image)
                obstacleResources = listOf(R.drawable.bird1_image, R.drawable.bird2_image, R.drawable.orage_image, R.drawable.orage2_image, R.drawable.ballon_image, R.drawable.parachute_image, R.drawable.helicoptere_image)
            }
            "STAR JUMPER" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_espace)
                player.setImageResource(R.drawable.fusee_image)
                obstacleResources = listOf(R.drawable.planette_image, R.drawable.satellite_image, R.drawable.etoilefilante_image, R.drawable.extraterrestre_image)
            }
            "COMÈTE" -> {
                gameLayout.setBackgroundResource(R.drawable.fond_espace)
                player.setImageResource(R.drawable.comete_image)
                obstacleResources = listOf(R.drawable.planette_image, R.drawable.satellite_image, R.drawable.etoilefilante_image, R.drawable.extraterrestre_image)
            }
            else -> {
                gameLayout.setBackgroundResource(R.drawable.fond_champ)
                player.setImageResource(R.drawable.coureur_image)
                obstacleResources = listOf(R.drawable.tree_image, R.drawable.rock_image, R.drawable.barriere_image)
            }
        }
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
        if (obstacle2.visibility == View.VISIBLE) {
            obstacle2.translationY += gameSpeed
        }

        if (obstacle.translationY > gameLayout.height) {
            score += 10
            tvScore.text = "Score: $score"

            val acceleration = when(difficulteActuelle) {
                "TRÈS FACILE" -> 0.05f  // Accélère à peine
                "FACILE" -> 0.1f
                "NORMAL" -> 0.25f
                "DIFFICILE" -> 0.5f     // Ça commence à chauffer
                "LÉGENDAIRE" -> 0.9f    // Ça devient très vite incontrôlable !
                else -> 0.1f
            }
            gameSpeed += acceleration

            spawnObstacle()
        }

        checkCollision()
    }

    private fun spawnObstacle() {
        if (obstacleResources.isNotEmpty()) {
            obstacle.setImageResource(obstacleResources.random())
            obstacle2.setImageResource(obstacleResources.random())
        }

        obstacle.translationY = -300f
        obstacle2.translationY = -300f

        // Probabilité d'avoir 2 obstacles qui bloquent la route
        val chanceDouble = when(difficulteActuelle) {
            "DIFFICILE" -> 35 // 35% de chances
            "LÉGENDAIRE" -> 70 // 70% de chances !
            else -> 0 // 0% pour Normal, Facile et Très Facile
        }

        val faireDouble = (1..100).random() <= chanceDouble

        // On choisit les colonnes (0 = gauche, 1 = milieu, 2 = droite)
        val colonnesPossibles = mutableListOf(0, 1, 2)

        val col1 = colonnesPossibles.random()
        colonnesPossibles.remove(col1) // On l'enlève de la liste pour ne pas superposer

        val laneWidth = screenWidth / 3f
        obstacle.x = (col1 * laneWidth) + (laneWidth / 2f) - (obstacle.width / 2f)

        if (faireDouble) {
            val col2 = colonnesPossibles.random()
            obstacle2.x = (col2 * laneWidth) + (laneWidth / 2f) - (obstacle2.width / 2f)
            obstacle2.visibility = View.VISIBLE

        } else {
            obstacle2.visibility = View.GONE
        }
    }

    private fun checkCollision() {
        val playerRect = Rect()
        player.getHitRect(playerRect)

        // Vérifie le 1er obstacle
        val obstacleRect = Rect()
        obstacle.getHitRect(obstacleRect)
        if (Rect.intersects(playerRect, obstacleRect)) {
            endGame()
        }

        // Vérifie le 2ème obstacle S'IL EST LÀ
        if (obstacle2.visibility == View.VISIBLE) {
            val obstacle2Rect = Rect()
            obstacle2.getHitRect(obstacle2Rect)
            if (Rect.intersects(playerRect, obstacle2Rect)) {
                endGame()
            }
        }
    }

    private fun endGame() {
        isGameOver = true
        handler.removeCallbacks(gameRunnable)

        val prefs = getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
        if (prefs.getBoolean("SETTING_VIBRATION", true)) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }

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