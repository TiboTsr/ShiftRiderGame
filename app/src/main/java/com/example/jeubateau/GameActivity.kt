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
import android.util.Log
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

/**
 * GameActivity gère la boucle principale du jeu, les collisions, 
 * les capteurs et l'interface utilisateur pendant une partie.
 */
class GameActivity : AppCompatActivity() {

    private val TAG = "GameActivity"
    private val REFERENCE_HEIGHT = 800f
    private val MIN_SWIPE = 60

    // --- ATTRIBUTS DE L'INTERFACE (Même nom que XML en camelCase) ---
    private lateinit var player: ImageView
    private lateinit var tvScore: TextView
    private lateinit var gameLayout: ConstraintLayout
    private lateinit var layoutPauseMenu: LinearLayout
    private lateinit var layoutConfirmQuit: LinearLayout
    private lateinit var btnPause: ImageButton
    private lateinit var btnResume: Button
    private lateinit var btnQuit: Button
    private lateinit var btnConfirmQuit: Button
    private lateinit var btnCancelQuit: Button

    // --- ÉTAT DU JEU ---
    private var frameDelay = 16L
    private var difficulteActuelle = "TRÈS FACILE"
    private var currentLane = 1
    private var score = 0
    private var isGameOver = false
    private var isPaused = false
    private var screenWidth = 0
    private var screenHeight = 0

    // --- AUDIO (Musique et Sons) ---
    private var musicPlayer: MediaPlayer? = null
    private var collisionPlayer: MediaPlayer? = null

    // --- CAPTEURS ET MOUVEMENT ---
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var useSensor = false
    private var lastSensorMoveTime = 0L

    private var gameSpeedBase = 15f
    private var gameSpeed = 15f
    private var x1 = 0f

    // --- OBSTACLES ---
    private var obstacleResources: List<Int> = listOf()

    /**
     * Structure pour stocker un emplacement d'obstacle avec sa vue et sa voie.
     */
    data class ObstacleSlot(val view: ImageView, var lane: Int)
    private val pool = mutableListOf<ObstacleSlot>()

    /**
     * Calcule le nombre maximum d'obstacles à l'écran selon le score et la difficulté.
     */
    private fun maxObstacles(): Int = when {
        score < 500 -> 2
        else -> if (difficulteActuelle in listOf("EXPERT", "MAÎTRE", "LÉGENDAIRE")) 3 else 2
    }

    /**
     * Calcule l'espacement minimum entre deux obstacles.
     */
    private fun minSpacing(): Float = screenHeight * when (difficulteActuelle) {
        "TRÈS FACILE", "FACILE" -> 0.55f
        "MOYEN", "NORMAL"       -> 0.50f
        "DIFFICILE", "EXPERT"   -> 0.48f
        "MAÎTRE", "LÉGENDAIRE"  -> 0.45f
        else                    -> 0.50f
    }

    // --- BOUCLE DE JEU ---
    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            // Le jeu ne tourne QUE s'il n'est pas en pause ni fini
            if (!isGameOver && !isPaused) { 
                update() 
                handler.postDelayed(this, frameDelay) 
            }
        }
    }

    /**
     * Écouteur pour les changements de l'accéléromètre (Capteur d'inclinaison).
     */
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // Si le capteur n'est pas activé ou que le jeu est en pause, on ignore.
            if (!useSensor || isGameOver || isPaused || event == null) return

            val x = event.values[0] // L'axe X (Gauche/Droite)
            val currentTime = System.currentTimeMillis()

            // On met un "Cooldown" de 300ms pour éviter les mouvements brusques
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

    /**
     * Initialisation de l'activité, des vues et des services.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Démarrage de GameActivity")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        setContentView(R.layout.activity_game)

        // Liaison des variables aux widgets XML
        initViews()

        // Configuration de l'audio
        setupAudio()

        // Initialisation du gestionnaire de capteurs Android
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Applique les réglages utilisateur (Thème, FPS, Vitesse)
        applyTheme()

        // Attend que le layout soit dessiné pour obtenir les dimensions réelles
        gameLayout.post {
            screenWidth  = gameLayout.width
            screenHeight = gameLayout.height
            gameSpeed = gameSpeedBase * (screenHeight / REFERENCE_HEIGHT)

            // Affiche la boîte de dialogue pour choisir le mode de pilotage
            afficherChoixControle()
        }

        // Configuration des écouteurs tactiles et clics
        setupListeners()
    }

    /**
     * Lie les composants XML aux variables de classe.
     */
    private fun initViews() {
        player            = findViewById(R.id.player)
        tvScore           = findViewById(R.id.tv_score)
        gameLayout        = findViewById(R.id.game_layout)
        layoutPauseMenu   = findViewById(R.id.layout_pause_menu)
        layoutConfirmQuit = findViewById(R.id.layout_confirm_quit)
        btnPause          = findViewById(R.id.btn_pause)
        btnResume         = findViewById(R.id.btn_resume)
        btnQuit           = findViewById(R.id.btn_quit)
        btnConfirmQuit    = findViewById(R.id.btn_confirm_quit)
        btnCancelQuit     = findViewById(R.id.btn_cancel_quit)
    }

    /**
     * Prépare les lecteurs MediaPlayer pour la musique et les sons.
     */
    private fun setupAudio() {
        try {
            musicPlayer = MediaPlayer.create(this, R.raw.musicdefond)
            musicPlayer?.isLooping = true
            collisionPlayer = MediaPlayer.create(this, R.raw.collision)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement audio", e)
        }
    }

    /**
     * Configure les écouteurs de clics et de toucher (Swipe).
     */
    private fun setupListeners() {
        // Contrôles Tactiles (Swipe)
        gameLayout.setOnTouchListener { _, e ->
            if (!isGameOver && !isPaused && !useSensor) {
                if (e.action == MotionEvent.ACTION_DOWN) x1 = e.x
                if (e.action == MotionEvent.ACTION_UP) {
                    val dx = e.x - x1
                    if (abs(dx) > MIN_SWIPE) { 
                        if (dx > 0) movePlayer(false) else movePlayer(true) 
                    }
                }
            }
            true
        }

        // Bouton Pause : Met le jeu en pause et affiche le menu
        btnPause.setOnClickListener {
            if (!isGameOver && !isPaused) {
                Log.d(TAG, "Mise en pause demandée")
                isPaused = true
                handler.removeCallbacks(loop)
                musicPlayer?.pause()
                layoutPauseMenu.visibility = View.VISIBLE
            }
        }

        // Bouton Reprendre : Relance le jeu
        btnResume.setOnClickListener {
            Log.d(TAG, "Reprise du jeu")
            isPaused = false
            layoutPauseMenu.visibility = View.GONE
            musicPlayer?.start()
            handler.post(loop)
        }

        // Bouton Quitter : Affiche le menu de confirmation
        btnQuit.setOnClickListener {
            layoutPauseMenu.visibility = View.GONE
            layoutConfirmQuit.visibility = View.VISIBLE
        }

        // Confirmation de sortie : Ferme l'activité
        btnConfirmQuit.setOnClickListener {
            Log.i(TAG, "L'utilisateur quitte la partie")
            finish()
        }

        // Annulation de sortie : Retour au menu de pause
        btnCancelQuit.setOnClickListener {
            layoutConfirmQuit.visibility = View.GONE
            layoutPauseMenu.visibility = View.VISIBLE
        }
    }

    /**
     * Affiche la boîte de dialogue pour choisir entre tactile ou inclinaison.
     */
    private fun afficherChoixControle() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_control_title))
            .setMessage(getString(R.string.dialog_control_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.control_touch)) { _, _ ->
                useSensor = false
                reset() // Lance le jeu !
            }
            .setNegativeButton(getString(R.string.control_sensor)) { _, _ ->
                useSensor = true
                sensorManager?.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                reset() // Lance le jeu !
            }
            .show()
    }

    /**
     * Charge le thème, la difficulté et la vitesse depuis les préférences.
     */
    private fun applyTheme() {
        val p = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        difficulteActuelle = p.getString("DIFF_ACTUELLE", "TRÈS FACILE") ?: "TRÈS FACILE"
        frameDelay = if (p.getBoolean("SETTING_60FPS", true)) 16L else 32L
        gameSpeedBase = p.getFloat("VITESSE_ACTUELLE", 15f)

        // Sélection des ressources graphiques selon le thème choisi
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

    /**
     * Retourne le multiplicateur de vitesse selon la difficulté.
     */
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

    /**
     * Calcule la taille d'un obstacle en pixels (20% de la largeur écran).
     */
    private fun obsSize() = (screenWidth * 0.20f).toInt()

    /**
     * Recyle une vue d'obstacle invisible ou en crée une nouvelle si besoin.
     */
    private fun recycleView(): ImageView {
        pool.firstOrNull { it.view.visibility == View.GONE }?.let { return it.view }
        return ImageView(this).also {
            it.scaleType = ImageView.ScaleType.FIT_CENTER
            it.visibility = View.GONE
            gameLayout.addView(it)
            pool.add(ObstacleSlot(it, 0))
        }
    }

    /**
     * Tente de faire apparaître un nouvel obstacle s'il y a assez de place.
     */
    private fun trySpawn() {
        val active = pool.filter { it.view.visibility == View.VISIBLE }
        if (active.size >= maxObstacles()) return

        val topY = active.minOfOrNull { it.view.translationY } ?: Float.MAX_VALUE
        if (topY != Float.MAX_VALUE && topY < minSpacing()) return

        val iv = recycleView()
        val sz = obsSize()
        iv.layoutParams = ConstraintLayout.LayoutParams(sz, sz)

        // Détermine une voie libre pour éviter les murs infranchissables
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

    /**
     * Déplace le joueur vers la voie de gauche ou de droite.
     */
    private fun movePlayer(left: Boolean) {
        if (left && currentLane > 0) currentLane--
        else if (!left && currentLane < 2) currentLane++
        val lw = screenWidth / 3f
        player.animate().x(currentLane * lw + lw / 2f - player.width / 2f).setDuration(80).start()
    }

    /**
     * Met à jour la position des obstacles et le score.
     */
    private fun update() {
        pool.filter { it.view.visibility == View.VISIBLE }.forEach { obs ->
            obs.view.translationY += gameSpeed
            // Si l'obstacle sort de l'écran par le bas
            if (obs.view.translationY > screenHeight + obsSize()) {
                obs.view.visibility = View.GONE
                score += 10
                tvScore.text = getString(R.string.score_format, score)
                accelerate()
            }
        }
        trySpawn()
        checkHits()
    }

    /**
     * Augmente progressivement la vitesse du jeu selon la difficulté choisie.
     */
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

    /**
     * Vérifie si le joueur entre en collision avec un obstacle actif.
     */
    private fun checkHits() {
        val pr = Rect(); player.getHitRect(pr)
        // Réduction de la hitbox pour plus de "fair-play" (marge de 22%)
        val m = (pr.width() * 0.22f).toInt()
        val fair = Rect(pr.left + m, pr.top + m, pr.right - m, pr.bottom - m)
        
        pool.filter { it.view.visibility == View.VISIBLE }.forEach { obs ->
            val r = Rect(); obs.view.getHitRect(r)
            if (Rect.intersects(fair, r)) { 
                Log.i(TAG, "Collision détectée !")
                endGame() 
                return 
            }
        }
    }

    /**
     * Arrête le jeu, joue le son de collision et lance l'écran de fin.
     */
    private fun endGame() {
        isGameOver = true
        handler.removeCallbacks(loop)
        
        musicPlayer?.stop()
        collisionPlayer?.start()

        // Gestion de la vibration
        vibrateOnCollision()

        val intent = Intent(this, GameOverActivity::class.java)
        intent.putExtra("SCORE_FINAL", score)
        startActivity(intent)
        finish()
    }

    /**
     * Fait vibrer l'appareil en cas de collision (si activé dans les réglages).
     */
    private fun vibrateOnCollision() {
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
    }

    /**
     * Réinitialise les paramètres pour lancer une nouvelle partie.
     */
    private fun reset() {
        Log.i(TAG, "Démarrage d'une nouvelle partie")
        score = 0
        isGameOver = false
        currentLane = 1
        tvScore.text = getString(R.string.score_format, 0)
        pool.forEach { it.view.visibility = View.GONE }
        
        // Repositionne le joueur au centre
        val lw = screenWidth / 3f
        player.x = lw + lw / 2f - player.width / 2f

        val scale = screenHeight / REFERENCE_HEIGHT
        gameSpeed = gameSpeedBase * scale * getDiffScale()

        // Création du premier obstacle
        val iv = recycleView()
        val sz = obsSize()
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

    // --- CYCLES DE VIE (SÉCURITÉS POUR LE CAPTEUR ET L'AUDIO) ---

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause : Libération des capteurs")
        if (useSensor) sensorManager?.unregisterListener(sensorListener)
        musicPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume : Réactivation des capteurs si nécessaire")
        if (useSensor && !isGameOver && !isPaused) {
            sensorManager?.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        if (!isPaused && !isGameOver) musicPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy : Nettoyage des ressources")
        handler.removeCallbacks(loop)
        sensorManager?.unregisterListener(sensorListener)
        musicPlayer?.release()
        collisionPlayer?.release()
    }
}