package com.example.jeubateau

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

/**
 * MainActivity est l'écran d'accueil du jeu.
 * Elle permet de gérer le pseudo, d'accéder au garage, aux paramètres, 
 * au classement, aux règles du jeu et de lancer la partie.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    // --- ATTRIBUTS DE L'INTERFACE ---
    private lateinit var tvTopScore: TextView
    private lateinit var tvCoins: TextView
    private lateinit var tvVehicle: TextView
    private lateinit var ivMainVehicle: ImageView
    private lateinit var etPseudo: EditText
    private lateinit var btnPlay: Button
    private lateinit var btnGarage: Button
    private lateinit var btnRules: Button
    private lateinit var btnSettings: Button
    private lateinit var btnClassement: Button

    /**
     * Initialisation de l'activité.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Démarrage de MainActivity")
        
        // Verrouillage en mode portrait pour la cohérence visuelle
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        // Initialisation des composants graphiques
        initViews()

        // Chargement des préférences utilisateur
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        // Génération d'un identifiant unique pour le joueur s'il n'existe pas
        if (!prefs.contains("MON_ID_SECRET")) {
            val nouvelId = UUID.randomUUID().toString()
            prefs.edit().putString("MON_ID_SECRET", nouvelId).apply()
            Log.d(TAG, "Nouvel ID secret généré : $nouvelId")
        }

        setupListeners()
        actualiserInfosAccueil()
    }

    /**
     * Lie les variables aux widgets du fichier XML activity_main.
     */
    private fun initViews() {
        tvTopScore = findViewById(R.id.tv_top_score)
        tvCoins = findViewById(R.id.tv_coins)
        tvVehicle = findViewById(R.id.tv_current_vehicle)
        ivMainVehicle = findViewById(R.id.iv_main_vehicle)
        etPseudo = findViewById(R.id.et_pseudo)
        btnPlay = findViewById(R.id.btn_play)
        btnGarage = findViewById(R.id.btn_garage)
        btnRules = findViewById(R.id.btn_rules)
        btnSettings = findViewById(R.id.btn_settings)
        btnClassement = findViewById(R.id.btn_classement)
    }

    /**
     * Configure les actions au clic sur les boutons.
     */
    private fun setupListeners() {
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        // Bouton Jouer : Enregistre le pseudo et lance GameActivity
        btnPlay.setOnClickListener {
            val pseudo = etPseudo.text.toString()
            prefs.edit().putString("PLAYER_PSEUDO", pseudo).apply()
            Log.d(TAG, "Lancement du jeu pour le joueur : $pseudo")

            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Bouton Garage : Accès à la boutique
        btnGarage.setOnClickListener {
            startActivity(Intent(this, GarageActivity::class.java))
        }

        // Bouton Règles : Affiche comment jouer
        btnRules.setOnClickListener {
            startActivity(Intent(this, RulesActivity::class.java))
        }

        // Bouton Paramètres : Options audio et vibration
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Bouton Classement : Affiche les scores mondiaux
        btnClassement.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }
    }

    /**
     * Met à jour les éléments de l'interface avec les données sauvegardées.
     */
    private fun actualiserInfosAccueil() {
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        val highScore = prefs.getInt("HIGH_SCORE", 0)
        val coins = prefs.getInt("TOTAL_COINS", 0)
        val themeNom = prefs.getString("THEME_NOM", "ATHLÈTE") ?: "ATHLÈTE"
        val difficulte = prefs.getString("DIFF_ACTUELLE", "TRÈS FACILE") ?: "TRÈS FACILE"
        val pseudo = prefs.getString("PLAYER_PSEUDO", "")

        // Affichage du meilleur score et des pièces
        tvTopScore.text = getString(R.string.top_score_prefix, highScore)
        tvCoins.text = getString(R.string.coins_prefix, coins)

        // Affichage de la sélection actuelle
        tvVehicle.text = getString(R.string.current_vehicle_format, themeNom, difficulte)
        etPseudo.setText(pseudo)

        // Mise à jour de l'image selon le thème sélectionné
        updateVehicleImage(themeNom)
    }

    /**
     * Change l'image du véhicule affichée sur l'accueil.
     * @param themeNom Le nom du thème sélectionné.
     */
    private fun updateVehicleImage(themeNom: String) {
        val resourceId = when (themeNom) {
            "ATHLÈTE" -> R.drawable.coureur_image
            "BMX PRO" -> R.drawable.velo_image
            "BOLIDE" -> R.drawable.voiture_image
            "FORMULE 1" -> R.drawable.voituredecourse_image
            "CORSAIRE" -> R.drawable.bateau_image
            "VOL LÉGER" -> R.drawable.petitavion_image
            "AIRLINER" -> R.drawable.grandavion_image
            "STAR JUMPER" -> R.drawable.fusee_image
            "COMÈTE" -> R.drawable.comete_image
            else -> R.drawable.coureur_image
        }
        ivMainVehicle.setImageResource(resourceId)
    }

    /**
     * Actualise les informations chaque fois que l'utilisateur revient sur l'écran.
     */
    override fun onResume() {
        super.onResume()
        actualiserInfosAccueil()
    }
}