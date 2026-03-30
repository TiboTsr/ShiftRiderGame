package com.example.jeubateau

import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * SettingsActivity permet à l\'utilisateur de configurer ses préférences de jeu
 * (Audio, Vibrations, FPS) et de réinitialiser sa progression.
 */
class SettingsActivity : AppCompatActivity() {

    private val TAG = "SettingsActivity"

    // --- ATTRIBUTS DE L\'INTERFACE ---
    private lateinit var switchMusique: Switch
    private lateinit var switchEffets: Switch
    private lateinit var switchVibration: Switch
    private lateinit var switchFps: Switch
    private lateinit var btnRetour: Button
    private lateinit var btnReset: Button

    /**
     * Initialisation de l\'écran des paramètres.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Ouverture des paramètres")
        
        // Verrouillage de l\'orientation en mode portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_settings)

        // Liaison des widgets XML
        initViews()

        // Chargement des préférences actuelles
        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)
        loadCurrentSettings(prefs)

        // Configuration des écouteurs de changement et de clic
        setupListeners(prefs)
    }

    /**
     * Lie les composants du fichier XML activity_settings aux variables.
     */
    private fun initViews() {
        switchMusique   = findViewById(R.id.switch_musique)
        switchEffets    = findViewById(R.id.switch_effets)
        switchVibration = findViewById(R.id.switch_vibration)
        switchFps       = findViewById(R.id.switch_fps)
        btnRetour       = findViewById(R.id.btn_retour_settings)
        btnReset        = findViewById(R.id.btn_reset_progress)
    }

    /**
     * Applique l\'état sauvegardé aux différents interrupteurs (Switch).
     */
    private fun loadCurrentSettings(prefs: android.content.SharedPreferences) {
        switchMusique.isChecked   = prefs.getBoolean("SETTING_MUSIC", true)
        switchEffets.isChecked    = prefs.getBoolean("SETTING_SFX", true)
        switchVibration.isChecked = prefs.getBoolean("SETTING_VIBRATION", true)
        switchFps.isChecked       = prefs.getBoolean("SETTING_60FPS", true)
    }

    /**
     * Définit les actions à effectuer lors de l\'interaction avec les réglages.
     */
    private fun setupListeners(prefs: android.content.SharedPreferences) {
        // Enregistrement immédiat de chaque modification
        switchMusique.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_MUSIC", isChecked).apply()
            Log.d(TAG, "Musique : $isChecked")
        }
        
        switchEffets.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_SFX", isChecked).apply()
            Log.d(TAG, "Effets sonores : $isChecked")
        }
        
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_VIBRATION", isChecked).apply()
            Log.d(TAG, "Vibrations : $isChecked")
        }
        
        switchFps.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_60FPS", isChecked).apply()
            Log.d(TAG, "Mode 60 FPS : $isChecked")
        }

        // Retour au menu principal
        btnRetour.setOnClickListener { finish() }

        // Action de réinitialisation complète avec confirmation
        btnReset.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    /**
     * Affiche une boîte de dialogue de sécurité avant d\'effacer les données.
     */
    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Attention !")
            .setMessage("Es-tu sûr de vouloir effacer toute ta progression ? (Pièces, Véhicules, Records)")
            .setPositiveButton("Oui, tout effacer") { _, _ ->
                // Suppression de toutes les données du fichier GAME_PREFS
                val prefs = getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()

                Log.w(TAG, "Réinitialisation complète effectuée par l\'utilisateur")
                Toast.makeText(this, "Progression réinitialisée.", Toast.LENGTH_SHORT).show()
                
                // On ferme la page pour forcer le rafraîchissement de l\'accueil
                finish()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}