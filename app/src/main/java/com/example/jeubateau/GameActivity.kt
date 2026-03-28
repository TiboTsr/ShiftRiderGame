package com.example.jeubateau

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

class GameActivity : AppCompatActivity() {

    private var frameDelay = 16L

    private lateinit var player: ImageView
    private lateinit var tvScore: TextView
    private lateinit var gameLayout: ConstraintLayout
    private lateinit var layoutPauseMenu: LinearLayout
    private lateinit var layoutConfirmQuit: LinearLayout

    private var difficulteActuelle = "TRÈS FACILE"
    private var currentLane = 1
    private var score = 0
    private var isGameOver = false
    private var isPaused = false
    private var screenWidth = 0
    private var screenHeight = 0

    // Musique et Sons
    private var musicPlayer: MediaPlayer? = null
    private var collisionPlayer: MediaPlayer? = null

    // Variables pour l'Accéléromètre (Capteur d'inclinaison)
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var useSensor = false
    private var lastSensorMoveTime = 0L

    private var gameSpeedBase = 15f
    private var gameSpeed = 15f
    private val REFERENCE_HEIGHT = 800f

    private var x1 = 0f
    private val MIN_SWIPE = 60

    private var obstacleResources: List<Int> = listOf()

    data class ObstacleSlot(val view: ImageView, var lane: Int)
    private val pool = mutableListOf<ObstacleSlot>()

    private fun maxObstacles(): Int = when {
        score < 500 -> 2
        else -> if (difficulteActuelle in listOf("EXPERT", "MAÎTRE", "LÉGENDAIRE")) 3 else 2
    }

    private fun minSpacing(): Float = screenHeight * when (difficulteActuelle) {
        "TRÈS FACILE", "FACILE" -> 0.55f
        "MOYEN", "NORMAL"       -> 0.50f
        "DIFFICILE", "EXPERT"   -> 0.48f
        "MAÎTRE", "LÉGENDAIRE"  -> 0.45f
        else                    -> 0.50f
    }

    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            // Le jeu ne tourne QUE s'il n'est pas en pause
            if (!isGameOver && !isPaused) { update(); handler.postDelayed(this, frameDelay) }
        }
    }

    // --- LE MOTEUR DU CAPTEUR (Écoute l'inclinaison) ---
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // Si le capteur n'est pas activé ou que le jeu est en pause, on ignore.
            if (!useSensor || isGameOver || isPaused || event == null) return

            val x = event.values[0] // L'axe X (Gauche/Droite)
            val currentTime = System.currentTimeMillis()

            // On met un "Cooldown" de 300ms pour éviter que le joueur se téléporte de la voie 0 à 2 instantanément
            if (currentTime - lastSensorMoveTime > 300) {
                if (x > 3.0f) { // Le téléphone est penché vers la GAUCHE
                    movePlayer(true)
                    lastSensorMoveTime = currentTime
                } else if (x < -3.0f) { // Le téléphone est penché vers la DROITE
                    movePlayer(false)
                    lastSensorMoveTime = currentTime
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_game)

        player     = findViewById(R.id.player)
        tvScore    = findViewById(R.id.tv_score)
        gameLayout = findViewById(R.id.game_layout)
        layoutPauseMenu = findViewById(R.id.layout_pause_menu)
        layoutConfirmQuit = findViewById(R.id.layout_confirm_quit)

        // Initialisation audio
        musicPlayer = MediaPlayer.create(this, R.raw.musicdefond)
        musicPlayer?.isLooping = true
        collisionPlayer = MediaPlayer.create(this, R.raw.collision)

        // Initialisation du gestionnaire de capteurs Android
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        applyTheme()

        gameLayout.post {
            screenWidth  = gameLayout.width
            screenHeight = gameLayout.height
            gameSpeed = gameSpeedBase * (screenHeight / REFERENCE_HEIGHT)

            // Au lieu de lancer le jeu tout de suite, on affiche la question !
            afficherChoixControle()
        }

        // Contrôles Tactiles
        gameLayout.setOnTouchListener { _, e ->
            // Le tactile ne marche QUE si on n'a PAS choisi le capteur
            if (!isGameOver && !isPaused && !useSensor) {
                if (e.action == MotionEvent.ACTION_DOWN) x1 = e.x
                if (e.action == MotionEvent.ACTION_UP) {
                    val dx = e.x - x1
                    if (abs(dx) > MIN_SWIPE) { if (dx > 0) movePlayer(false) else movePlayer(true) }
                }
            }
            true
        }

        // --- MENU PAUSE ---
        val btnPause = findViewById<ImageButton>(R.id.btn_pause)
        val btnResume = findViewById<Button>(R.id.btn_resume)
        val btnQuit = findViewById<Button>(R.id.btn_quit)

        btnPause.setOnClickListener {
            if (!isGameOver && !isPaused) {
                isPaused = true
                handler.removeCallbacks(loop) // On arrête le chrono
                musicPlayer?.pause()
                layoutPauseMenu.visibility = View.VISIBLE // Affiche le menu
            }
        }

        btnResume.setOnClickListener {
            isPaused = false
            layoutPauseMenu.visibility = View.GONE
            musicPlayer?.start()
            handler.post(loop) // Relance le chrono
        }

        btnQuit.setOnClickListener {
            layoutPauseMenu.visibility = View.GONE
            layoutConfirmQuit.visibility = View.VISIBLE
        }

        // --- MENU CONFIRMATION QUITTER ---
        val btnConfirmQuit = findViewById<Button>(R.id.btn_confirm_quit)
        val btnCancelQuit = findViewById<Button>(R.id.btn_cancel_quit)

        btnConfirmQuit.setOnClickListener {
            finish()
        }

        btnCancelQuit.setOnClickListener {
            layoutConfirmQuit.visibility = View.GONE
            layoutPauseMenu.visibility = View.VISIBLE
        }
    }

    // --- LA BOITE DE DIALOGUE DU DÉBUT ---
    private fun afficherChoixControle() {
        AlertDialog.Builder(this)
            .setTitle("Comment voulez-vous piloter ?")
            .setMessage("Choisissez votre mode de contrôle :")
            .setCancelable(false) // Empêche de cliquer à côté pour fermer
            .setPositiveButton("📱 Tactile (Glisser)") { _, _ ->
                useSensor = false
                reset() // On lance le jeu !
            }
            .setNegativeButton("📳 Capteur (Inclinaison)") { _, _ ->
                useSensor = true
                // On allume le capteur !
                sensorManager?.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                reset() // On lance le jeu !
            }
            .show()
    }

    private fun applyTheme() {
        val p = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
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

    private fun getDiffScale(): Float = when (difficulteActuelle) {
        "TRÈS FACILE" -> 0.24f
        "FACILE"      -> 0.26f
        "MOYEN"       -> 0.28f
        "NORMAL"      -> 0.30f
        "DIFFICILE"   -> 0.31f
        "EXPERT"      -> 0.32f
        "MAÎTRE"      -> 0.33f
        "LÉGENDAIRE"  -> 0.34f
        else          -> 0.30f
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

        val topY = active.minOfOrNull { it.view.translationY } ?: Float.MAX_VALUE
        if (topY != Float.MAX_VALUE && topY < minSpacing()) return

        val iv = recycleView()
        val sz = obsSize()
        iv.layoutParams = ConstraintLayout.LayoutParams(sz, sz)

        val occupied = active.filter { it.view.translationY < screenHeight * 0.55f }.map { it.lane }.toSet()
        val freeLanes = (0..2).filter { !occupied.contains(it) }
        val lane = if (freeLanes.isNotEmpty()) freeLanes.random() else (0..2).random()

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
        val maxSpeed = (gameSpeedBase * 0.36f) * scale
        val ajout = scale * when (difficulteActuelle) {
            "TRÈS FACILE" -> 0.002f
            "FACILE"      -> 0.003f
            "MOYEN"       -> 0.004f
            "NORMAL"      -> 0.005f
            "DIFFICILE"   -> 0.006f
            "EXPERT"      -> 0.007f
            "MAÎTRE"      -> 0.008f
            "LÉGENDAIRE"  -> 0.009f
            else          -> 0.005f
        }
        if (gameSpeed < maxSpeed) gameSpeed += ajout
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
        
        musicPlayer?.stop()
        collisionPlayer?.start()

        val p = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        if (p.getBoolean("SETTING_VIBRATION", true)) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        }
        startActivity(Intent(this, GameOverActivity::class.java).putExtra("SCORE_FINAL", score))
        finish()
    }

    private fun reset() {
        score = 0; isGameOver = false; currentLane = 1; tvScore.text = "Score: 0"
        pool.forEach { it.view.visibility = View.GONE }
        val lw = screenWidth / 3f
        player.x = lw + lw / 2f - player.width / 2f

        val scale = screenHeight / REFERENCE_HEIGHT
        gameSpeed = gameSpeedBase * scale * getDiffScale()

        val iv = recycleView(); val sz = obsSize()
        iv.layoutParams = ConstraintLayout.LayoutParams(sz, sz)
        if (obstacleResources.isNotEmpty()) iv.setImageResource(obstacleResources.random())

        val randomLane = (0..2).random()
        iv.x = randomLane * lw + lw / 2f - sz / 2f
        iv.translationY = -(screenHeight * 0.20f)
        iv.visibility = View.VISIBLE
        pool.find { it.view === iv }?.lane = randomLane

        musicPlayer?.start()
        handler.post(loop)
    }

    // --- SÉCURITÉS POUR LE CAPTEUR ---
    // Si l'application est mise en arrière-plan, on éteint le capteur pour économiser la batterie
    override fun onPause() {
        super.onPause()
        if (useSensor) sensorManager?.unregisterListener(sensorListener)
        musicPlayer?.pause()
    }

    // Si on revient sur l'application, on rallume le capteur
    override fun onResume() {
        super.onResume()
        if (useSensor && !isGameOver && !isPaused) {
            sensorManager?.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        if (!isPaused && !isGameOver) musicPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(loop)
        sensorManager?.unregisterListener(sensorListener)
        musicPlayer?.release()
        collisionPlayer?.release()
    }
}