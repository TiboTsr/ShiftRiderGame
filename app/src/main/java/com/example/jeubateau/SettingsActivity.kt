package com.example.jeubateau

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("GAME_PREFS", MODE_PRIVATE)

        val switchMusique = findViewById<Switch>(R.id.switch_musique)
        val switchEffets = findViewById<Switch>(R.id.switch_effets)
        val switchVibration = findViewById<Switch>(R.id.switch_vibration)
        val switchFps = findViewById<Switch>(R.id.switch_fps)
        val btnRetour = findViewById<Button>(R.id.btn_retour_settings)
        val btnReset = findViewById<Button>(R.id.btn_reset_progress)

        switchMusique.isChecked = prefs.getBoolean("SETTING_MUSIC", true)
        switchEffets.isChecked = prefs.getBoolean("SETTING_SFX", true)
        switchVibration.isChecked = prefs.getBoolean("SETTING_VIBRATION", true)
        switchFps.isChecked = prefs.getBoolean("SETTING_60FPS", true)

        switchMusique.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_MUSIC", isChecked).apply()
        }
        switchEffets.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_SFX", isChecked).apply()
        }
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_VIBRATION", isChecked).apply()
        }
        switchFps.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("SETTING_60FPS", isChecked).apply()
        }

        btnRetour.setOnClickListener {
            finish()
        }

        btnReset.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Attention !")
                .setMessage("Es-tu sûr de vouloir effacer toute ta progression ? (Pièces, Véhicules achetés, Meilleurs Scores)")
                .setPositiveButton("Oui, tout effacer") { _, _ ->
                    // Le code qui efface tout
                    val prefs = getSharedPreferences("GAME_PREFS", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    android.widget.Toast.makeText(this, "Progression réinitialisée.", android.widget.Toast.LENGTH_SHORT).show()
                    finish() // Ferme la page
                }
                .setNegativeButton("Annuler", null) // Ne fait rien si on annule
                .show()
        }
    }
}