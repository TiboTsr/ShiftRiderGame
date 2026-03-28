package com.example.jeubateau

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

    private var frameDelay = 16L

    private lateinit var player: ImageView
    private lateinit var tvScore: TextView
    private lateinit var gameLayout: ConstraintLayout

    private var difficulteActuelle = "TRÈS FACILE"
    private var currentLane = 1
    private var score = 0
    private var isGameOver = false
    private var screenWidth = 0
    private var screenHeight = 0

    // Vitesse normalisée : on cible la même vitesse visuelle (% écran/s)
    // indépendamment de la résolution physique
    private var gameSpeedBase = 15f  // px/frame à 800px de hauteur
    private var gameSpeed = 15f
    private val REFERENCE_HEIGHT = 800f

    private var x1 = 0f
    private val MIN_SWIPE = 60

    private var obstacleResources: List<Int> = listOf()

    data class ObstacleSlot(val view: ImageView, var lane: Int)
    private val pool = mutableListOf<ObstacleSlot>()

    // Nombre max d'obstacles simultanés selon le score
    private fun maxObstacles(): Int = when {
        score < 80  -> 1
        score < 250 -> 2
        score < 600 -> 3
        else        -> 4
    }

    // Espacement minimal (en px relatif à l'écran) entre le haut de l'écran
    // et le spawn du prochain obstacle. Diminue avec le score.
    private fun minSpacing(): Float = screenHeight * when {
        score < 80  -> 0.55f
        score < 250 -> 0.38f
        score < 600 -> 0.25f
        else        -> 0.15f
    }

    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            if (!isGameOver) { update(); handler.postDelayed(this, frameDelay) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_game)
        player     = findViewById(R.id.player)
        tvScore    = findViewById(R.id.tv_score)
        gameLayout = findViewById(R.id.game_layout)
        applyTheme()
        gameLayout.post {
            screenWidth  = gameLayout.width
            screenHeight = gameLayout.height
            gameSpeed = gameSpeedBase * (screenHeight / REFERENCE_HEIGHT)
            reset()
        }
        gameLayout.setOnTouchListener { _, e ->
            if (!isGameOver) {
                if (e.action == MotionEvent.ACTION_DOWN) x1 = e.x
                if (e.action == MotionEvent.ACTION_UP) {
                    val dx = e.x - x1
                    if (abs(dx) > MIN_SWIPE) { if (dx > 0) movePlayer(false) else movePlayer(true) }
                }
            }
            true
        }
    }

    private fun applyTheme() {
        val p = getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
        difficulteActuelle = p.getString("DIFF_ACTUELLE", "TRÈS FACILE") ?: "TRÈS FACILE"
        frameDelay = if (p.getBoolean("SETTING_60FPS", true)) 16L else 32L
        gameSpeedBase = p.getFloat("VITESSE_ACTUELLE", 15f)
        when (p.getString("THEME_NOM", "ATHLÈTE")) {
            "ATHLÈTE"     -> { gameLayout.setBackgroundResource(R.drawable.fond_champ);   player.setImageResource(R.drawable.coureur_image);         obstacleResources = listOf(R.drawable.tree_image, R.drawable.rock_image, R.drawable.barriere_image, R.drawable.vache_image) }
            "BMX PRO"     -> { gameLayout.setBackgroundResource(R.drawable.fond_champ);   player.setImageResource(R.drawable.velo_image);            obstacleResources = listOf(R.drawable.tree_image, R.drawable.rock_image, R.drawable.barriere_image, R.drawable.vache_image) }
            "CAVALIER"    -> { gameLayout.setBackgroundResource(R.drawable.fond_champ);   player.setImageResource(R.drawable.cavalier_image);        obstacleResources = listOf(R.drawable.tree_image, R.drawable.rock_image, R.drawable.barriere_image, R.drawable.vache_image) }
            "BOLIDE"      -> { gameLayout.setBackgroundResource(R.drawable.fond_route);   player.setImageResource(R.drawable.voiture_image);         obstacleResources = listOf(R.drawable.pneu_image, R.drawable.oil_image, R.drawable.barriere_image) }
            "FORMULE 1"   -> { gameLayout.setBackgroundResource(R.drawable.fond_circuit); player.setImageResource(R.drawable.voituredecourse_image); obstacleResources = listOf(R.drawable.pneu_image, R.drawable.oil_image, R.drawable.barriere_image) }
            "CORSAIRE"    -> { gameLayout.setBackgroundResource(R.drawable.fond_mer);     player.setImageResource(R.drawable.bateau_image);          obstacleResources = listOf(R.drawable.bouee_image, R.drawable.requin_image, R.drawable.rock_image) }
            "VOL LÉGER"   -> { gameLayout.setBackgroundResource(R.drawable.fond_ciel);    player.setImageResource(R.drawable.petitavion_image);      obstacleResources = listOf(R.drawable.bird1_image, R.drawable.bird2_image, R.drawable.orage_image, R.drawable.orage2_image, R.drawable.ballon_image, R.drawable.parachute_image, R.drawable.helicoptere_image) }
            "AIRLINER"    -> { gameLayout.setBackgroundResource(R.drawable.fond_ciel);    player.setImageResource(R.drawable.grandavion_image);      obstacleResources = listOf(R.drawable.bird1_image, R.drawable.bird2_image, R.drawable.orage_image, R.drawable.orage2_image, R.drawable.ballon_image, R.drawable.parachute_image, R.drawable.helicoptere_image) }
            "STAR JUMPER" -> { gameLayout.setBackgroundResource(R.drawable.fond_espace);  player.setImageResource(R.drawable.fusee_image);           obstacleResources = listOf(R.drawable.planette_image, R.drawable.satellite_image, R.drawable.etoilefilante_image, R.drawable.extraterrestre_image) }
            "COMÈTE"      -> { gameLayout.setBackgroundResource(R.drawable.fond_espace);  player.setImageResource(R.drawable.comete_image);          obstacleResources = listOf(R.drawable.planette_image, R.drawable.satellite_image, R.drawable.etoilefilante_image, R.drawable.extraterrestre_image) }
            else          -> { gameLayout.setBackgroundResource(R.drawable.fond_champ);   player.setImageResource(R.drawable.coureur_image);         obstacleResources = listOf(R.drawable.tree_image, R.drawable.rock_image, R.drawable.barriere_image) }
        }
    }

    private fun obsSize() = (screenWidth * 0.20f).toInt()

    private fun recycleView(): ImageView {
        pool.firstOrNull { it.view.visibility == View.GONE }?.let { return it.view }
        return ImageView(this).also {
            it.scaleType = ImageView.ScaleType.FIT_CENTER
            it.visibility = View.GONE
            gameLayout.addView(it)
            pool.add(ObstacleSlot(it, 0))
        }
    }

    private fun trySpawn() {
        val active = pool.filter { it.view.visibility == View.VISIBLE }
        if (active.size >= maxObstacles()) return

        // Ne spawner que si le dernier obstacle est suffisamment descendu
        val topY = active.minOfOrNull { it.view.translationY } ?: Float.MAX_VALUE
        if (topY != Float.MAX_VALUE && topY < minSpacing()) return

        val iv = recycleView()
        val sz = obsSize()
        iv.layoutParams = ConstraintLayout.LayoutParams(sz, sz)

        val occupied = pool
            .filter { it.view.visibility == View.VISIBLE && it.view.translationY < screenHeight * 0.35f }
            .map { it.lane }
        val lane = ((0..2) - occupied.toSet()).let { if (it.isEmpty()) (0..2).random() else it.random() }

        pool.find { it.view === iv }?.lane = lane
        if (obstacleResources.isNotEmpty()) iv.setImageResource(obstacleResources.random())

        val lw = screenWidth / 3f
        iv.x = lane * lw + lw / 2f - sz / 2f
        iv.translationY = -sz.toFloat()
        iv.visibility = View.VISIBLE
    }

    private fun movePlayer(left: Boolean) {
        if (left && currentLane > 0) currentLane--
        else if (!left && currentLane < 2) currentLane++
        val lw = screenWidth / 3f
        player.animate().x(currentLane * lw + lw / 2f - player.width / 2f).setDuration(80).start()
    }

    private fun update() {
        pool.filter { it.view.visibility == View.VISIBLE }.forEach { obs ->
            obs.view.translationY += gameSpeed
            if (obs.view.translationY > screenHeight + obsSize()) {
                obs.view.visibility = View.GONE
                score += 10; tvScore.text = "Score: $score"
                accelerate()
            }
        }
        trySpawn()
        checkHits()
    }

    private fun accelerate() {
        val scale = screenHeight / REFERENCE_HEIGHT
        gameSpeed += scale * when (difficulteActuelle) {
            "TRÈS FACILE" -> 0.12f
            "FACILE"      -> 0.20f
            "MOYEN"       -> 0.35f
            "NORMAL"      -> 0.50f
            "DIFFICILE"   -> 0.75f
            "EXPERT"      -> 1.00f
            "MAÎTRE"      -> 1.30f
            "LÉGENDAIRE"  -> 1.80f
            else          -> 0.20f
        }
    }

    private fun checkHits() {
        val pr = Rect(); player.getHitRect(pr)
        val m = (pr.width() * 0.22f).toInt()
        val fair = Rect(pr.left + m, pr.top + m, pr.right - m, pr.bottom - m)
        pool.filter { it.view.visibility == View.VISIBLE }.forEach { obs ->
            val r = Rect(); obs.view.getHitRect(r)
            if (Rect.intersects(fair, r)) { endGame(); return }
        }
    }

    private fun endGame() {
        isGameOver = true; handler.removeCallbacks(loop)
        val p = getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
        if (p.getBoolean("SETTING_VIBRATION", true)) {
            (getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator)
                .vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
        startActivity(Intent(this, GameOverActivity::class.java).putExtra("SCORE_FINAL", score))
        finish()
    }

    private fun reset() {
        score = 0; isGameOver = false; currentLane = 1; tvScore.text = "Score: 0"
        pool.forEach { it.view.visibility = View.GONE }
        val lw = screenWidth / 3f
        player.x = lw + lw / 2f - player.width / 2f

        // Premier obstacle : déjà à 8% du haut pour qu'il arrive vite
        val iv = recycleView(); val sz = obsSize()
        iv.layoutParams = ConstraintLayout.LayoutParams(sz, sz)
        if (obstacleResources.isNotEmpty()) iv.setImageResource(obstacleResources.random())
        iv.x = lw + lw / 2f - sz / 2f
        iv.translationY = -(screenHeight * 0.08f)
        iv.visibility = View.VISIBLE
        pool.find { it.view === iv }?.lane = 1

        handler.post(loop)
    }

    override fun onDestroy() { super.onDestroy(); handler.removeCallbacks(loop) }
}